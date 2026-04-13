from __future__ import annotations

import threading
import time
from typing import List

import psutil

from benchmark.metrics.metrics_snapshot import MetricsSnapshot


class MetricsCollector:
    """
    Background sampling of CPU and RSS memory at a configurable interval.
    Maps RSS into heap_used_bytes; non_heap is 0 (parity with total MB reporting).
    """

    def __init__(self, sample_interval_ms: int) -> None:
        self._sample_interval_ms = sample_interval_ms
        self._snapshots: List[MetricsSnapshot] = []
        self._lock = threading.Lock()
        self._stop = threading.Event()
        self._thread: threading.Thread | None = None
        self._process = psutil.Process()

    def start(self) -> None:
        with self._lock:
            self._snapshots.clear()
        self._stop.clear()
        self._process.cpu_percent(interval=None)
        self._thread = threading.Thread(target=self._run, name="metrics-collector", daemon=True)
        self._thread.start()

    def stop(self) -> tuple[MetricsSnapshot, ...]:
        self._stop.set()
        if self._thread is not None:
            self._thread.join(timeout=self._sample_interval_ms / 1000.0 * 5 + 1.0)
            self._thread = None
        self._sample()
        with self._lock:
            return tuple(self._snapshots)

    def shutdown(self) -> None:
        self._stop.set()

    def _run(self) -> None:
        interval_s = self._sample_interval_ms / 1000.0
        while not self._stop.is_set():
            self._sample()
            if self._stop.wait(timeout=interval_s):
                break

    def _sample(self) -> None:
        cpu = self._process.cpu_percent(interval=None)
        if cpu < 0:
            cpu = 0.0
        rss = int(self._process.memory_info().rss)
        snap = MetricsSnapshot(
            timestamp_ms=int(time.time() * 1000),
            cpu_usage_percent=cpu,
            heap_used_bytes=rss,
            heap_max_bytes=0,
            non_heap_used_bytes=0,
        )
        with self._lock:
            self._snapshots.append(snap)

    @staticmethod
    def average_cpu(samples: tuple[MetricsSnapshot, ...]) -> float:
        if not samples:
            return 0.0
        return sum(s.cpu_usage_percent for s in samples) / len(samples)

    @staticmethod
    def peak_cpu(samples: tuple[MetricsSnapshot, ...]) -> float:
        if not samples:
            return 0.0
        return max(s.cpu_usage_percent for s in samples)

    @staticmethod
    def average_memory_mb(samples: tuple[MetricsSnapshot, ...]) -> float:
        if not samples:
            return 0.0
        return sum(s.get_total_memory_mb() for s in samples) / len(samples)

    @staticmethod
    def peak_memory_mb(samples: tuple[MetricsSnapshot, ...]) -> float:
        if not samples:
            return 0.0
        return max(s.get_total_memory_mb() for s in samples)
