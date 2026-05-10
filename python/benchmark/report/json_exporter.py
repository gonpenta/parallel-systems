from __future__ import annotations

import json
from datetime import datetime, timezone
from pathlib import Path
from typing import List, Mapping

from benchmark.core.benchmark_result import BenchmarkResult


class JsonExporter:
    def __init__(self, output_dir: str) -> None:
        self._output_dir = Path(output_dir)

    def export(self, results_by_task: Mapping[str, List[BenchmarkResult]]) -> str:
        self._output_dir.mkdir(parents=True, exist_ok=True)
        out_path = self._output_dir / "benchmark-results-python.json"

        tasks = []
        for task_name, results in results_by_task.items():
            levels = []
            for r in results:
                levels.append(
                    {
                        "concurrency": r.concurrency_level,
                        "totalIterations": r.total_iterations,
                        "totalDurationMs": r.total_duration_ms,
                        "throughputOpsPerSec": _round(r.get_throughput()),
                        "avgLatencyMs": _round(r.get_avg_latency_ms()),
                        "medianLatencyMs": _round(r.get_median_latency_ms()),
                        "p95LatencyMs": _round(r.get_p95_latency_ms()),
                        "p99LatencyMs": _round(r.get_p99_latency_ms()),
                        "avgCpuPercent": _round(r.avg_cpu_percent),
                        "peakCpuPercent": _round(r.peak_cpu_percent),
                        "avgMemoryMB": _round(r.avg_memory_mb),
                        "peakMemoryMB": _round(r.peak_memory_mb),
                    }
                )
            tasks.append({"taskName": task_name, "concurrencyLevels": levels})

        payload = {
            "generatedAt": datetime.now(timezone.utc).isoformat(),
            "framework": "Python Concurrency Benchmark Framework",
            "language": "python",
            "tasks": tasks,
        }

        out_path.write_text(json.dumps(payload, indent=2), encoding="utf-8")
        return str(out_path.resolve())


def _round(v: float) -> float:
    return round(v * 100.0) / 100.0
