package com.benchmark.report;

import com.benchmark.core.BenchmarkResult;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Produces a polished, self-contained HTML report from benchmark results and chart PNGs.
 */
public final class HtmlReportGenerator {

    private final String outputDir;

    public HtmlReportGenerator(String outputDir) {
        this.outputDir = outputDir;
        new File(outputDir).mkdirs();
    }

    public String generate(Map<String, List<BenchmarkResult>> resultsByTask,
                           Map<String, String> chartFiles) throws IOException {

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        StringBuilder html = new StringBuilder();
        html.append(head());
        html.append("<body>\n");
        html.append("<div class='container'>\n");
        html.append(header(timestamp));
        html.append(summaryTable(resultsByTask));
        html.append(chartsSection(chartFiles));
        html.append(detailTables(resultsByTask));
        html.append(footer());
        html.append("</div></body></html>\n");

        File out = new File(outputDir, "benchmark-report.html");
        try (Writer w = new FileWriter(out, StandardCharsets.UTF_8)) {
            w.write(html.toString());
        }
        return out.getAbsolutePath();
    }

    // -------------------------------------------------------------------------

    private String head() {
        return "<!DOCTYPE html><html lang='en'><head>\n" +
               "<meta charset='UTF-8'>\n" +
               "<meta name='viewport' content='width=device-width, initial-scale=1'>\n" +
               "<title>Benchmark Report</title>\n" +
               "<style>\n" +
               css() +
               "</style></head>\n";
    }

    private String css() {
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
            """;
    }

    private String header(String timestamp) {
        return String.format("""
            <div class='hero'>
              <h1>⚡ Concurrency Benchmark Report</h1>
              <div class='sub'>Execution time · Throughput · CPU utilisation · Memory usage</div>
              <div class='badge'>Generated: %s</div>
            </div>
            """, timestamp);
    }

    private String summaryTable(Map<String, List<BenchmarkResult>> resultsByTask) {
        StringBuilder sb = new StringBuilder("<section>\n<h2>Summary — Best Throughput per Task</h2>\n<div class='card'>\n");
        sb.append("<table><thead><tr>")
          .append("<th>Task</th>")
          .append("<th>Best Concurrency</th>")
          .append("<th>Peak Throughput (ops/s)</th>")
          .append("<th>Avg Latency (ms)</th>")
          .append("<th>P99 Latency (ms)</th>")
          .append("<th>Avg CPU (%)</th>")
          .append("<th>Peak Memory (MB)</th>")
          .append("</tr></thead><tbody>\n");

        for (Map.Entry<String, List<BenchmarkResult>> entry : resultsByTask.entrySet()) {
            BenchmarkResult best = entry.getValue().stream()
                    .max(Comparator.comparingDouble(BenchmarkResult::getThroughput))
                    .orElseThrow();
            sb.append(String.format(
                "<tr><td>%s</td><td>%d</td><td class='good'>%,.1f</td>" +
                "<td>%.2f</td><td>%.2f</td><td>%.1f</td><td>%.1f</td></tr>\n",
                best.getTaskName(), best.getConcurrencyLevel(),
                best.getThroughput(), best.getAvgLatencyMs(), best.getP99LatencyMs(),
                best.getAvgCpuPercent(), best.getPeakMemoryMB()
            ));
        }
        sb.append("</tbody></table></div></section>\n");
        return sb.toString();
    }

    private String chartsSection(Map<String, String> chartFiles) {
        StringBuilder sb = new StringBuilder("<section>\n<h2>Charts</h2>\n<div class='charts-grid'>\n");
        for (Map.Entry<String, String> e : chartFiles.entrySet()) {
            // Embed as relative path
            String filename = new File(e.getValue()).getName();
            sb.append(String.format(
                "<div class='chart-card'><h3>%s</h3><img src='%s' alt='%s'></div>\n",
                e.getKey(), filename, e.getKey()
            ));
        }
        sb.append("</div></section>\n");
        return sb.toString();
    }

    private String detailTables(Map<String, List<BenchmarkResult>> resultsByTask) {
        StringBuilder sb = new StringBuilder("<section>\n<h2>Detailed Results per Concurrency Level</h2>\n");

        for (Map.Entry<String, List<BenchmarkResult>> entry : resultsByTask.entrySet()) {
            sb.append(String.format("<div class='card'><h3 style='margin-bottom:14px;color:#0f3460'>%s</h3>\n", entry.getKey()));
            sb.append("<table><thead><tr>")
              .append("<th>Threads</th>")
              .append("<th>Iterations</th>")
              .append("<th>Duration (ms)</th>")
              .append("<th>Throughput (ops/s)</th>")
              .append("<th>Avg Latency (ms)</th>")
              .append("<th>Median (ms)</th>")
              .append("<th>P95 (ms)</th>")
              .append("<th>P99 (ms)</th>")
              .append("<th>Avg CPU (%)</th>")
              .append("<th>Peak CPU (%)</th>")
              .append("<th>Avg Mem (MB)</th>")
              .append("<th>Peak Mem (MB)</th>")
              .append("</tr></thead><tbody>\n");

            for (BenchmarkResult r : entry.getValue()) {
                sb.append(String.format(
                    "<tr><td>%d</td><td>%,d</td><td>%,d</td>" +
                    "<td>%,.1f</td><td>%.2f</td><td>%.2f</td><td>%.2f</td><td>%.2f</td>" +
                    "<td>%.1f</td><td>%.1f</td><td>%.1f</td><td>%.1f</td></tr>\n",
                    r.getConcurrencyLevel(), r.getTotalIterations(), r.getTotalDurationMs(),
                    r.getThroughput(), r.getAvgLatencyMs(), r.getMedianLatencyMs(),
                    r.getP95LatencyMs(), r.getP99LatencyMs(),
                    r.getAvgCpuPercent(), r.getPeakCpuPercent(),
                    r.getAvgMemoryMB(), r.getPeakMemoryMB()
                ));
            }
            sb.append("</tbody></table></div>\n");
        }
        sb.append("</section>\n");
        return sb.toString();
    }

    private String footer() {
        return "<footer>Generated by Java Concurrency Benchmark Framework</footer>\n";
    }
}
