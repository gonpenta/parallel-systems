from __future__ import annotations

from collections import OrderedDict
from typing import Dict, List

from benchmark.core import BenchmarkConfig, BenchmarkRunner, BenchmarkTask
from benchmark.core.benchmark_result import BenchmarkResult
from benchmark.examples import ExampleTasks
from benchmark.report import ChartGenerator, HtmlReportGenerator, JsonExporter


def main() -> None:
    config = (
        BenchmarkConfig.builder()
        .concurrency_levels(1, 2, 4, 8, 16, 32)
        .metrics_sample_interval_ms(150)
        .output_directory("benchmark-results")
        .generate_charts(True)
        .generate_html_report(True)
        .verbose(False)
        .task_timeout_seconds(120)
        .build()
    )

    tasks: List[BenchmarkTask] = [
        ExampleTasks.hashing(),
        ExampleTasks.sort_small(),
        ExampleTasks.map_ops(),
        ExampleTasks.simulated_io(),
    ]

    runner = BenchmarkRunner(config)
    results_by_task: OrderedDict[str, List[BenchmarkResult]] = OrderedDict()

    print()
    print("+========================================================+")
    print("|      Python Concurrency Benchmark Framework           |")
    print("+========================================================+")
    print()
    print(f"Concurrency levels : {config.concurrency_levels}")
    print(f"Output directory   : {config.output_directory}")
    print(f"Tasks              : {len(tasks)}")
    print()

    for task in tasks:
        results = runner.run(task)
        results_by_task[task.name()] = results

    chart_files: Dict[str, str] = {}
    if config.generate_charts:
        print("\nGenerating charts...")
        charts = ChartGenerator(config.output_directory)
        chart_files = charts.generate_all(results_by_task)
        for label, path in chart_files.items():
            print(f"     OK  {label} -> {path}")

    print("\nExporting JSON...")
    json_exporter = JsonExporter(config.output_directory)
    json_path = json_exporter.export(results_by_task)
    print(f"     OK  {json_path}")

    if config.generate_html_report:
        print("\nGenerating HTML report...")
        reporter = HtmlReportGenerator(config.output_directory)
        html_path = reporter.generate(results_by_task, chart_files)
        print(f"     OK  {html_path}")

    print("\n+========================================================+")
    print("|                    FINAL SUMMARY                     |")
    print("+========================================================+")
    for name, results in results_by_task.items():
        print(f"\n  {name}")
        print("  " + "-" * 70)
        print(
            f"  {'Threads':<12} {'Throughput (ops/s)':<18} {'Avg Latency (ms)':<18} "
            f"{'CPU (%)':<12} {'Peak Mem (MB)':<14}"
        )
        for r in results:
            print(
                f"  {r.concurrency_level:<12} {r.get_throughput():<18.1f} "
                f"{r.get_avg_latency_ms():<18.2f} {r.avg_cpu_percent:<12.1f} {r.peak_memory_mb:<14.1f}"
            )

    print(
        "\n  Done. Open benchmark-results/benchmark-report.html to view the report.\n"
    )


if __name__ == "__main__":
    main()
