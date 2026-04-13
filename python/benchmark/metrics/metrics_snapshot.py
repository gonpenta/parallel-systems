from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class MetricsSnapshot:
    """A snapshot of process metrics at one point in time."""

    timestamp_ms: int
    cpu_usage_percent: float
    heap_used_bytes: int
    heap_max_bytes: int
    non_heap_used_bytes: int

    def get_total_memory_mb(self) -> float:
        return (self.heap_used_bytes + self.non_heap_used_bytes) / (1024.0 * 1024.0)

    def get_heap_usage_percent(self) -> float:
        if self.heap_max_bytes <= 0:
            return 0.0
        return 100.0 * self.heap_used_bytes / self.heap_max_bytes
