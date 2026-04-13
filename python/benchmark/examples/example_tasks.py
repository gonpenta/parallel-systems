from __future__ import annotations

import hashlib
import math
import random
import threading
import time
from typing import Dict

from benchmark.core.benchmark_task import BenchmarkTask


class SortingTask(BenchmarkTask):
    def __init__(self, array_size: int) -> None:
        self._array_size = array_size

    def name(self) -> str:
        return f"Array Sort ({self._array_size} ints)"

    def execute(self, worker_id: int) -> None:
        data = [random.randint(-(2**31), 2**31 - 1) for _ in range(self._array_size)]
        data.sort()

    def iterations_per_worker(self) -> int:
        return 200

    def warmup_iterations(self) -> int:
        return 20


class HashingTask(BenchmarkTask):
    _PAYLOAD_SIZE = 4096

    def name(self) -> str:
        return "SHA-256 Hashing (4 KB)"

    def execute(self, worker_id: int) -> None:
        payload = random.randbytes(self._PAYLOAD_SIZE)
        hashlib.sha256(payload).digest()

    def iterations_per_worker(self) -> int:
        return 500

    def warmup_iterations(self) -> int:
        return 50


class SimulatedIOTask(BenchmarkTask):
    def __init__(self, sleep_ms: int) -> None:
        self._sleep_ms = sleep_ms

    def name(self) -> str:
        return f"Simulated I/O ({self._sleep_ms} ms latency)"

    def execute(self, worker_id: int) -> None:
        time.sleep(self._sleep_ms / 1000.0)
        acc = 0.0
        for i in range(1000):
            acc += math.sqrt(i)
        if acc < 0:
            raise RuntimeError("unreachable")

    def iterations_per_worker(self) -> int:
        return 20

    def warmup_iterations(self) -> int:
        return 3


class MapTask(BenchmarkTask):
    def __init__(self) -> None:
        self._map: Dict[int, str] = {}
        self._lock = threading.Lock()

    def name(self) -> str:
        return "ConcurrentHashMap (90% read)"

    def setup(self) -> None:
        for i in range(10_000):
            self._map[i] = f"value-{i}"

    def execute(self, worker_id: int) -> None:
        key = random.randint(0, 9_999)
        if random.randint(0, 9) == 0:
            with self._lock:
                self._map[key] = f"v-{worker_id}"
        else:
            with self._lock:
                _ = self._map.get(key)

    def iterations_per_worker(self) -> int:
        return 1000

    def warmup_iterations(self) -> int:
        return 100


class ExampleTasks:
    @staticmethod
    def sort_small() -> BenchmarkTask:
        return SortingTask(10_000)

    @staticmethod
    def sort_large() -> BenchmarkTask:
        return SortingTask(1_000_000)

    @staticmethod
    def hashing() -> BenchmarkTask:
        return HashingTask()

    @staticmethod
    def simulated_io() -> BenchmarkTask:
        return SimulatedIOTask(10)

    @staticmethod
    def map_ops() -> BenchmarkTask:
        return MapTask()
