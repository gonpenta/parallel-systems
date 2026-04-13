from __future__ import annotations

import math
from dataclasses import dataclass

from benchmark.metrics.metrics_snapshot import MetricsSnapshot


@dataclass(frozen=True)
class BenchmarkResult:
    """Immutable record of metrics for one (task, concurrency_level) pair."""

    task_name: str
    concurrency_level: int
    total_iterations: int
    total_duration_ms: int
    iteration_times_ns: tuple[int, ...]
    metrics_snapshots: tuple[MetricsSnapshot, ...]
    avg_cpu_percent: float
    peak_cpu_percent: float
    avg_memory_mb: float
    peak_memory_mb: float

    def get_throughput(self) -> float:
        if self.total_duration_ms <= 0:
            return 0.0
        return (self.total_iterations * 1000.0) / self.total_duration_ms

    def get_avg_latency_ms(self) -> float:
        if not self.iteration_times_ns:
            return 0.0
        return (sum(self.iteration_times_ns) / len(self.iteration_times_ns)) / 1_000_000.0

    def get_median_latency_ms(self) -> float:
        if not self.iteration_times_ns:
            return 0.0
        sorted_ns = sorted(self.iteration_times_ns)
        n = len(sorted_ns)
        mid = n // 2
        if n % 2 == 0:
            return (sorted_ns[mid - 1] + sorted_ns[mid]) / 2_000_000.0
        return sorted_ns[mid] / 1_000_000.0

    def get_p95_latency_ms(self) -> float:
        return self._percentile_ms(95)

    def get_p99_latency_ms(self) -> float:
        return self._percentile_ms(99)

    def _percentile_ms(self, p: int) -> float:
        if not self.iteration_times_ns:
            return 0.0
        sorted_ns = sorted(self.iteration_times_ns)
        idx = int(math.ceil(p / 100.0 * len(sorted_ns))) - 1
        idx = max(0, min(idx, len(sorted_ns) - 1))
        return sorted_ns[idx] / 1_000_000.0

    def __str__(self) -> str:
        return (
            f"[{self.task_name} | concurrency={self.concurrency_level:<3}] "
            f"throughput={self.get_throughput():,.1f} ops/s  "
            f"avgLatency={self.get_avg_latency_ms():6.2f} ms  "
            f"p99={self.get_p99_latency_ms():6.2f} ms  "
            f"avgCPU={self.avg_cpu_percent:5.1f}%  "
            f"peakMem={self.peak_memory_mb:6.1f} MB"
        )
