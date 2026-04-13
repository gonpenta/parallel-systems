package com.benchmark;

import com.benchmark.core.*;
import com.benchmark.examples.ExampleTasks;
import com.benchmark.report.*;

import java.util.*;

/**
 * Entry point for the benchmark framework.
 *
 * <p>Runs a suite of example tasks across multiple concurrency levels,
 * then writes:
 * <ul>
 *   <li>PNG charts  →  benchmark-results/chart_*.png</li>
 *   <li>JSON export →  benchmark-results/benchmark-results.json</li>
 *   <li>HTML report →  benchmark-results/benchmark-report.html</li>
 * </ul>
 * </p>
 *
 * <p><b>To add your own task:</b> implement {@link com.benchmark.core.BenchmarkTask}
 * and add it to the {@code tasks} list below.</p>
 */
public class Main {

    public static void main(String[] args) throws Exception {

        // ─── 1. Configure the framework ───────────────────────────────────────
        BenchmarkConfig config = BenchmarkConfig.builder()
                .concurrencyLevels(1, 2, 4, 8, 16, 32)
                .metricsSampleIntervalMs(150)
                .outputDirectory("benchmark-results")
                .generateCharts(true)
                .generateHtmlReport(true)
                .verbose(false)
                .taskTimeoutSeconds(120)
                .build();

        // ─── 2. Choose tasks to benchmark ─────────────────────────────────────
        //
        //  Add your own BenchmarkTask implementations here:
        //
        //    tasks.add(new MyCustomTask());
        //
        List<BenchmarkTask> tasks = Arrays.asList(
                ExampleTasks.hashing(),
                ExampleTasks.sortSmall(),
                ExampleTasks.mapOps(),
                ExampleTasks.simulatedIO()
        );

        // ─── 3. Run all tasks ─────────────────────────────────────────────────
        BenchmarkRunner runner = new BenchmarkRunner(config);
        Map<String, List<BenchmarkResult>> resultsByTask = new LinkedHashMap<>();

        System.out.println("\n╔═══════════════════════════════════════════════════════╗");
        System.out.println("║       Java Concurrency Benchmark Framework            ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝\n");
        System.out.printf("Concurrency levels : %s%n", config.getConcurrencyLevels());
        System.out.printf("Output directory   : %s%n", config.getOutputDirectory());
        System.out.printf("Tasks              : %d%n%n", tasks.size());

        for (BenchmarkTask task : tasks) {
            List<BenchmarkResult> results = runner.run(task);
            resultsByTask.put(task.name(), results);
        }

        // ─── 4. Generate charts ───────────────────────────────────────────────
        Map<String, String> chartFiles = Collections.emptyMap();
        if (config.isGenerateCharts()) {
            System.out.println("\nGenerating charts...");
            ChartGenerator charts = new ChartGenerator(config.getOutputDirectory());
            chartFiles = charts.generateAll(resultsByTask);
            chartFiles.forEach((label, path) ->
                    System.out.printf("     ✓  %s → %s%n", label, path));
        }

        // ─── 5. Export JSON ───────────────────────────────────────────────────
        System.out.println("\nExporting JSON...");
        JsonExporter json = new JsonExporter(config.getOutputDirectory());
        String jsonPath = json.export(resultsByTask);
        System.out.println("     ✓  " + jsonPath);

        // ─── 6. Generate HTML report ──────────────────────────────────────────
        if (config.isGenerateHtmlReport()) {
            System.out.println("\nGenerating HTML report...");
            HtmlReportGenerator reporter = new HtmlReportGenerator(config.getOutputDirectory());
            String htmlPath = reporter.generate(resultsByTask, chartFiles);
            System.out.println("     ✓  " + htmlPath);
        }

        // ─── 7. Print console summary ─────────────────────────────────────────
        System.out.println("\n╔═══════════════════════════════════════════════════════╗");
        System.out.println("║                    FINAL SUMMARY                     ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝");
        for (Map.Entry<String, List<BenchmarkResult>> entry : resultsByTask.entrySet()) {
            System.out.println("\n  " + entry.getKey());
            System.out.println("  " + "─".repeat(70));
            System.out.printf("  %-12s %-18s %-18s %-12s %-14s%n",
                    "Threads", "Throughput (ops/s)", "Avg Latency (ms)", "CPU (%)", "Peak Mem (MB)");
            for (BenchmarkResult r : entry.getValue()) {
                System.out.printf("  %-12d %-18.1f %-18.2f %-12.1f %-14.1f%n",
                        r.getConcurrencyLevel(), r.getThroughput(),
                        r.getAvgLatencyMs(), r.getAvgCpuPercent(), r.getPeakMemoryMB());
            }
        }
        System.out.println("\n  Done. Open benchmark-results/benchmark-report.html to view the report.\n");
    }
}
