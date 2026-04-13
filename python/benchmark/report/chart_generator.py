from __future__ import annotations

from pathlib import Path
from typing import Callable, Dict, List, Mapping

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt

from benchmark.core.benchmark_result import BenchmarkResult

_PALETTE = [
    "#4E79A7",
    "#E15759",
    "#59A14F",
    "#F28E2B",
    "#76B7B2",
    "#B07AA1",
    "#EDC948",
    "#FF9DA7",
    "#9C755F",
]


class ChartGenerator:
    """Matplotlib PNG charts — same filenames as the Java framework."""

    def __init__(self, output_dir: str) -> None:
        self._output_dir = Path(output_dir)
        self._output_dir.mkdir(parents=True, exist_ok=True)

    def generate_all(
        self, data: Mapping[str, List[BenchmarkResult]]
    ) -> Dict[str, str]:
        out: Dict[str, str] = {}
        out["Throughput (ops/s)"] = self._render(
            data,
            "Throughput vs Concurrency",
            "Concurrency (threads)",
            "Throughput (ops/s)",
            lambda r: r.get_throughput(),
            "chart_throughput.png",
        )
        out["Avg Latency (ms)"] = self._render(
            data,
            "Average Latency vs Concurrency",
            "Concurrency (threads)",
            "Avg Latency (ms)",
            lambda r: r.get_avg_latency_ms(),
            "chart_latency_avg.png",
        )
        out["P99 Latency (ms)"] = self._render(
            data,
            "P99 Latency vs Concurrency",
            "Concurrency (threads)",
            "P99 Latency (ms)",
            lambda r: r.get_p99_latency_ms(),
            "chart_latency_p99.png",
        )
        out["CPU Utilisation (%)"] = self._render(
            data,
            "CPU Utilisation vs Concurrency",
            "Concurrency (threads)",
            "Avg CPU (%)",
            lambda r: r.avg_cpu_percent,
            "chart_cpu.png",
        )
        out["Peak Memory (MB)"] = self._render(
            data,
            "Peak Memory vs Concurrency",
            "Concurrency (threads)",
            "Peak Memory (MB)",
            lambda r: r.peak_memory_mb,
            "chart_memory.png",
        )
        return out

    def _render(
        self,
        data: Mapping[str, List[BenchmarkResult]],
        title: str,
        x_label: str,
        y_label: str,
        extract: Callable[[BenchmarkResult], float],
        filename: str,
    ) -> str:
        fig, ax = plt.subplots(figsize=(11.5, 6.5), dpi=100)
        fig.patch.set_facecolor("#F0F2F5")
        ax.set_facecolor("white")

        for i, (label, series) in enumerate(data.items()):
            xs = [r.concurrency_level for r in series]
            ys = [extract(r) for r in series]
            color = _PALETTE[i % len(_PALETTE)]
            ax.plot(
                xs,
                ys,
                marker="o",
                linewidth=2.2,
                markersize=7,
                color=color,
                label=label,
                markerfacecolor=color,
                markeredgecolor="white",
                markeredgewidth=1.2,
            )

        ax.set_title(title, fontsize=15, fontweight="bold", color="#0F3460", pad=12)
        ax.set_xlabel(x_label, fontsize=11, fontweight="bold", color="#0F3460")
        ax.set_ylabel(y_label, fontsize=11, fontweight="bold", color="#0F3460")
        ax.grid(True, linestyle="--", alpha=0.45, color="#E2E8F0")
        ax.legend(
            loc="center left",
            bbox_to_anchor=(1.02, 0.5),
            frameon=True,
            fontsize=9,
        )
        fig.tight_layout(rect=(0, 0, 0.78, 1))
        path = self._output_dir / filename
        fig.savefig(path, bbox_inches="tight", facecolor=fig.get_facecolor())
        plt.close(fig)
        return str(path.resolve())
