from __future__ import annotations

from dataclasses import dataclass
from typing import Sequence


@dataclass(frozen=True)
class BenchmarkConfig:
    """Immutable configuration for a benchmark run."""

    concurrency_levels: tuple[int, ...] = (1, 2, 4, 8, 16, 32)
    metrics_sample_interval_ms: int = 200
    output_directory: str = "benchmark-results"
    generate_html_report: bool = True
    generate_charts: bool = True
    verbose: bool = False
    task_timeout_seconds: int = 300

    @staticmethod
    def builder() -> "BenchmarkConfig.Builder":
        return BenchmarkConfig.Builder()

    class Builder:
        def __init__(self) -> None:
            self._concurrency_levels: tuple[int, ...] = (1, 2, 4, 8, 16, 32)
            self._metrics_sample_interval_ms: int = 200
            self._output_directory: str = "benchmark-results"
            self._generate_html_report: bool = True
            self._generate_charts: bool = True
            self._verbose: bool = False
            self._task_timeout_seconds: int = 300

        def concurrency_levels(self, *levels: int) -> "BenchmarkConfig.Builder":
            self._concurrency_levels = tuple(levels)
            return self

        def metrics_sample_interval_ms(self, ms: int) -> "BenchmarkConfig.Builder":
            self._metrics_sample_interval_ms = ms
            return self

        def output_directory(self, dir_: str) -> "BenchmarkConfig.Builder":
            self._output_directory = dir_
            return self

        def generate_html_report(self, b: bool) -> "BenchmarkConfig.Builder":
            self._generate_html_report = b
            return self

        def generate_charts(self, b: bool) -> "BenchmarkConfig.Builder":
            self._generate_charts = b
            return self

        def verbose(self, b: bool) -> "BenchmarkConfig.Builder":
            self._verbose = b
            return self

        def task_timeout_seconds(self, s: int) -> "BenchmarkConfig.Builder":
            self._task_timeout_seconds = s
            return self

        def build(self) -> "BenchmarkConfig":
            if not self._concurrency_levels:
                raise ValueError("At least one concurrency level is required")
            if self._metrics_sample_interval_ms < 10:
                raise ValueError("Sample interval must be >= 10 ms")
            return BenchmarkConfig(
                concurrency_levels=self._concurrency_levels,
                metrics_sample_interval_ms=self._metrics_sample_interval_ms,
                output_directory=self._output_directory,
                generate_html_report=self._generate_html_report,
                generate_charts=self._generate_charts,
                verbose=self._verbose,
                task_timeout_seconds=self._task_timeout_seconds,
            )
