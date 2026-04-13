from __future__ import annotations

from datetime import datetime
from pathlib import Path
from typing import Dict, List, Mapping

from benchmark.core.benchmark_result import BenchmarkResult


class HtmlReportGenerator:
    def __init__(self, output_dir: str) -> None:
        self._output_dir = Path(output_dir)
        self._output_dir.mkdir(parents=True, exist_ok=True)

    def generate(
        self,
        results_by_task: Mapping[str, List[BenchmarkResult]],
        chart_files: Mapping[str, str],
    ) -> str:
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        html: List[str] = []
        html.append(_head())
        html.append("<body>\n")
        html.append("<div class='container'>\n")
        html.append(_header(timestamp))
        html.append(_summary_table(results_by_task))
        html.append(_charts_section(chart_files))
        html.append(_detail_tables(results_by_task))
        html.append(_footer())
        html.append("</div></body></html>\n")

        out = self._output_dir / "benchmark-report.html"
        out.write_text("".join(html), encoding="utf-8")
        return str(out.resolve())


def _head() -> str:
    return (
        "<!DOCTYPE html><html lang='en'><head>\n"
        "<meta charset='UTF-8'>\n"
        "<meta name='viewport' content='width=device-width, initial-scale=1'>\n"
        "<title>Benchmark Report</title>\n"
        "<style>\n"
        + _css()
        + "</style></head>\n"
    )


def _css() -> str:
    return """
            * { box-sizing: border-box; margin: 0; padding: 0; }
            body { font-family: 'Segoe UI', system-ui, sans-serif; background: #f0f2f5;
                   color: #1a1a2e; line-height: 1.6; }
            .container { max-width: 1200px; margin: 0 auto; padding: 24px; }
            .hero { background: linear-gradient(135deg, #1a1a2e 0%, #16213e 60%, #0f3460 100%);
                    color: #fff; border-radius: 16px; padding: 40px 48px; margin-bottom: 32px; }
            .hero h1 { font-size: 2rem; font-weight: 700; letter-spacing: -0.5px; }
            .hero .sub { opacity: .65; font-size: .95rem; margin-top: 6px; }
            .hero .badge { display: inline-block; background: rgba(255,255,255,.12);
                           border-radius: 20px; padding: 4px 14px; font-size: .8rem;
                           margin-top: 14px; }
            section { margin-bottom: 40px; }
            h2 { font-size: 1.2rem; font-weight: 700; color: #0f3460;
                 margin-bottom: 16px; padding-bottom: 8px;
                 border-bottom: 2px solid #e2e8f0; }
            .card { background: #fff; border-radius: 12px;
                    box-shadow: 0 1px 3px rgba(0,0,0,.08); padding: 24px;
                    margin-bottom: 20px; }
            table { width: 100%; border-collapse: collapse; font-size: .88rem; }
            thead th { background: #0f3460; color: #fff; padding: 10px 14px;
                       text-align: right; font-weight: 600; white-space: nowrap; }
            thead th:first-child { text-align: left; border-radius: 6px 0 0 0; }
            thead th:last-child  { border-radius: 0 6px 0 0; }
            tbody tr:nth-child(even) { background: #f8fafc; }
            tbody td { padding: 9px 14px; text-align: right; border-bottom: 1px solid #e2e8f0; }
            tbody td:first-child { text-align: left; font-weight: 600; color: #0f3460; }
            .good  { color: #22863a; font-weight: 700; }
            .warn  { color: #e65c00; font-weight: 700; }
            .charts-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(440px, 1fr));
                           gap: 20px; }
            .chart-card { background: #fff; border-radius: 12px;
                          box-shadow: 0 1px 3px rgba(0,0,0,.08); overflow: hidden; }
            .chart-card h3 { font-size: .9rem; font-weight: 700; color: #0f3460;
                             padding: 14px 18px 0; }
            .chart-card img { width: 100%; display: block; padding: 8px; }
            footer { text-align: center; color: #94a3b8; font-size: .8rem;
                     padding: 24px 0 8px; }
            """


def _header(timestamp: str) -> str:
    return (
        "<div class='hero'>\n"
        "  <h1>⚡ Concurrency Benchmark Report</h1>\n"
        "  <div class='sub'>Execution time · Throughput · CPU utilisation · Memory usage</div>\n"
        f"  <div class='badge'>Generated: {timestamp}</div>\n"
        "</div>\n"
    )


def _summary_table(results_by_task: Mapping[str, List[BenchmarkResult]]) -> str:
    parts: List[str] = [
        "<section>\n<h2>Summary — Best Throughput per Task</h2>\n<div class='card'>\n",
        "<table><thead><tr>",
        "<th>Task</th>",
        "<th>Best Concurrency</th>",
        "<th>Peak Throughput (ops/s)</th>",
        "<th>Avg Latency (ms)</th>",
        "<th>P99 Latency (ms)</th>",
        "<th>Avg CPU (%)</th>",
        "<th>Peak Memory (MB)</th>",
        "</tr></thead><tbody>\n",
    ]
    for _name, results in results_by_task.items():
        best = max(results, key=lambda r: r.get_throughput())
        parts.append(
            "<tr><td>{task}</td><td>{conc}</td><td class='good'>{tp:,.1f}</td>"
            "<td>{avg:.2f}</td><td>{p99:.2f}</td><td>{cpu:.1f}</td><td>{mem:.1f}</td></tr>\n".format(
                task=best.task_name,
                conc=best.concurrency_level,
                tp=best.get_throughput(),
                avg=best.get_avg_latency_ms(),
                p99=best.get_p99_latency_ms(),
                cpu=best.avg_cpu_percent,
                mem=best.peak_memory_mb,
            )
        )
    parts.append("</tbody></table></div></section>\n")
    return "".join(parts)


def _charts_section(chart_files: Mapping[str, str]) -> str:
    parts: List[str] = [
        "<section>\n<h2>Charts</h2>\n<div class='charts-grid'>\n",
    ]
    for label, file_path in chart_files.items():
        fname = Path(file_path).name
        parts.append(
            "<div class='chart-card'><h3>{title}</h3><img src='{src}' alt='{alt}'></div>\n".format(
                title=label,
                src=fname,
                alt=label,
            )
        )
    parts.append("</div></section>\n")
    return "".join(parts)


def _detail_tables(results_by_task: Mapping[str, List[BenchmarkResult]]) -> str:
    parts: List[str] = [
        "<section>\n<h2>Detailed Results per Concurrency Level</h2>\n",
    ]
    for task_name, results in results_by_task.items():
        parts.append(
            f"<div class='card'><h3 style='margin-bottom:14px;color:#0f3460'>{task_name}</h3>\n"
        )
        parts.append("<table><thead><tr>")
        for h in (
            "Threads",
            "Iterations",
            "Duration (ms)",
            "Throughput (ops/s)",
            "Avg Latency (ms)",
            "Median (ms)",
            "P95 (ms)",
            "P99 (ms)",
            "Avg CPU (%)",
            "Peak CPU (%)",
            "Avg Mem (MB)",
            "Peak Mem (MB)",
        ):
            parts.append(f"<th>{h}</th>")
        parts.append("</tr></thead><tbody>\n")
        for r in results:
            parts.append(
                "<tr><td>{th}</td><td>{it:,}</td><td>{dur:,}</td>"
                "<td>{tp:,.1f}</td><td>{avg:.2f}</td><td>{med:.2f}</td><td>{p95:.2f}</td><td>{p99:.2f}</td>"
                "<td>{acpu:.1f}</td><td>{pcpu:.1f}</td><td>{amem:.1f}</td><td>{pmem:.1f}</td></tr>\n".format(
                    th=r.concurrency_level,
                    it=r.total_iterations,
                    dur=r.total_duration_ms,
                    tp=r.get_throughput(),
                    avg=r.get_avg_latency_ms(),
                    med=r.get_median_latency_ms(),
                    p95=r.get_p95_latency_ms(),
                    p99=r.get_p99_latency_ms(),
                    acpu=r.avg_cpu_percent,
                    pcpu=r.peak_cpu_percent,
                    amem=r.avg_memory_mb,
                    pmem=r.peak_memory_mb,
                )
            )
        parts.append("</tbody></table></div>\n")
    parts.append("</section>\n")
    return "".join(parts)


def _footer() -> str:
    return "<footer>Generated by Python Concurrency Benchmark Framework</footer>\n"
