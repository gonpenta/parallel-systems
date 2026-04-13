from __future__ import annotations

import sys
import threading
import time
from concurrent.futures import ThreadPoolExecutor, wait
from typing import List, Optional

from benchmark.core._sync import CountDownLatch
from benchmark.core.benchmark_config import BenchmarkConfig
from benchmark.core.benchmark_result import BenchmarkResult
from benchmark.core.benchmark_task import BenchmarkTask
from benchmark.metrics.metrics_collector import MetricsCollector


class BenchmarkRunner:
    """Drives a BenchmarkTask across all configured concurrency levels."""

    def __init__(self, config: BenchmarkConfig) -> None:
        self._config = config

    def run(self, task: BenchmarkTask) -> List[BenchmarkResult]:
        results: List[BenchmarkResult] = []
        print()
        print("+======================================================+")
        print(f"|  Benchmarking: {task.name():<37}|")
        print("+======================================================+")

        task.setup()

        for concurrency in self._config.concurrency_levels:
            print(f"\n  >>  concurrency = {concurrency}")
            result = self._run_level(task, concurrency)
            results.append(result)
            print(f"     {result}")
            time.sleep(0.5)

        task.teardown()
        return results

    def _run_level(self, task: BenchmarkTask, concurrency: int) -> BenchmarkResult:
        self._run_workers(
            task, concurrency, task.warmup_iterations(), None, None, None, None
        )

        iteration_times_ns: List[int] = []
        times_lock = threading.Lock()
        total_iterations = _AtomicInt()
        error_count = _AtomicInt()

        collector = MetricsCollector(self._config.metrics_sample_interval_ms)
        wall_start = time.time() * 1000.0
        collector.start()

        self._run_workers(
            task,
            concurrency,
            task.iterations_per_worker(),
            iteration_times_ns,
            times_lock,
            total_iterations,
            error_count,
        )

        snapshots = collector.stop()
        collector.shutdown()
        wall_end = time.time() * 1000.0
        total_duration_ms = int(wall_end - wall_start)

        if error_count.value > 0:
            print(
                f"     WARNING: {error_count.value} iteration(s) threw exceptions (included in timing)"
            )

        return BenchmarkResult(
            task_name=task.name(),
            concurrency_level=concurrency,
            total_iterations=total_iterations.value,
            total_duration_ms=total_duration_ms,
            iteration_times_ns=tuple(iteration_times_ns),
            metrics_snapshots=snapshots,
            avg_cpu_percent=MetricsCollector.average_cpu(snapshots),
            peak_cpu_percent=MetricsCollector.peak_cpu(snapshots),
            avg_memory_mb=MetricsCollector.average_memory_mb(snapshots),
            peak_memory_mb=MetricsCollector.peak_memory_mb(snapshots),
        )

    def _run_workers(
        self,
        task: BenchmarkTask,
        concurrency: int,
        iterations: int,
        iteration_times_ns: Optional[List[int]],
        times_lock: Optional[threading.Lock],
        total_iterations: Optional["_AtomicInt"],
        error_count: Optional["_AtomicInt"],
    ) -> None:
        ready = CountDownLatch(concurrency)
        start = CountDownLatch(1)

        def worker(worker_id: int) -> None:
            ready.count_down()
            start.await_zero()
            for _ in range(iterations):
                t0 = time.perf_counter_ns()
                try:
                    task.execute(worker_id)
                except Exception as ex:
                    if error_count is not None:
                        error_count.add(1)
                    if self._config.verbose:
                        print(
                            f"  Worker {worker_id} failed: {ex}",
                            file=sys.stderr,
                        )
                elapsed = time.perf_counter_ns() - t0
                if iteration_times_ns is not None and times_lock is not None:
                    with times_lock:
                        iteration_times_ns.append(elapsed)
                if total_iterations is not None:
                    total_iterations.add(1)

        with ThreadPoolExecutor(max_workers=concurrency) as executor:
            futures = [executor.submit(worker, w) for w in range(concurrency)]
            ready.await_zero()
            start.count_down()
            done, not_done = wait(futures, timeout=self._config.task_timeout_seconds)
            if not_done:
                print(
                    f"  WARNING: Concurrency level {concurrency} timed out after "
                    f"{self._config.task_timeout_seconds}s"
                )
                for f in not_done:
                    f.cancel()


class _AtomicInt:
    def __init__(self) -> None:
        self._v = 0
        self._lock = threading.Lock()

    def add(self, n: int = 1) -> None:
        with self._lock:
            self._v += n

    @property
    def value(self) -> int:
        with self._lock:
            return self._v
