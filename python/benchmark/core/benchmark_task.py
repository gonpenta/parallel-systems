from __future__ import annotations

from abc import ABC, abstractmethod


class BenchmarkTask(ABC):
    """Contract for any workload to be benchmarked."""

    @abstractmethod
    def name(self) -> str:
        """Human-readable name shown in reports and charts."""

    def setup(self) -> None:
        """Optional one-time setup before any concurrency level runs."""

    @abstractmethod
    def execute(self, worker_id: int) -> None:
        """The work unit executed repeatedly by each worker thread."""

    def teardown(self) -> None:
        """Optional cleanup after all concurrency levels have finished."""

    def iterations_per_worker(self) -> int:
        return 100

    def warmup_iterations(self) -> int:
        return 10
