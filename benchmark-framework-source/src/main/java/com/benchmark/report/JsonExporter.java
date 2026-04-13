package com.benchmark.report;

import com.benchmark.core.BenchmarkResult;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Exports benchmark results to a structured JSON file with no external libraries.
 */
public final class JsonExporter {

    private final String outputDir;

    public JsonExporter(String outputDir) {
        this.outputDir = outputDir;
    }

    public String export(Map<String, List<BenchmarkResult>> resultsByTask) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"generatedAt\": \"").append(Instant.now()).append("\",\n");
        sb.append("  \"framework\": \"Java Concurrency Benchmark Framework\",\n");
        sb.append("  \"tasks\": [\n");

        int ti = 0;
        for (Map.Entry<String, List<BenchmarkResult>> entry : resultsByTask.entrySet()) {
            if (ti++ > 0) sb.append(",\n");
            sb.append("    {\n");
            sb.append("      \"taskName\": \"").append(escape(entry.getKey())).append("\",\n");
            sb.append("      \"concurrencyLevels\": [\n");

            int ri = 0;
            for (BenchmarkResult r : entry.getValue()) {
                if (ri++ > 0) sb.append(",\n");
                sb.append("        {\n");
                sb.append("          \"concurrency\": ").append(r.getConcurrencyLevel()).append(",\n");
                sb.append("          \"totalIterations\": ").append(r.getTotalIterations()).append(",\n");
                sb.append("          \"totalDurationMs\": ").append(r.getTotalDurationMs()).append(",\n");
                sb.append("          \"throughputOpsPerSec\": ").append(round(r.getThroughput())).append(",\n");
                sb.append("          \"avgLatencyMs\": ").append(round(r.getAvgLatencyMs())).append(",\n");
                sb.append("          \"medianLatencyMs\": ").append(round(r.getMedianLatencyMs())).append(",\n");
                sb.append("          \"p95LatencyMs\": ").append(round(r.getP95LatencyMs())).append(",\n");
                sb.append("          \"p99LatencyMs\": ").append(round(r.getP99LatencyMs())).append(",\n");
                sb.append("          \"avgCpuPercent\": ").append(round(r.getAvgCpuPercent())).append(",\n");
                sb.append("          \"peakCpuPercent\": ").append(round(r.getPeakCpuPercent())).append(",\n");
                sb.append("          \"avgMemoryMB\": ").append(round(r.getAvgMemoryMB())).append(",\n");
                sb.append("          \"peakMemoryMB\": ").append(round(r.getPeakMemoryMB())).append("\n");
                sb.append("        }");
            }
            sb.append("\n      ]\n");
            sb.append("    }");
        }
        sb.append("\n  ]\n}\n");

        File out = new File(outputDir, "benchmark-results.json");
        try (Writer w = new FileWriter(out, StandardCharsets.UTF_8)) {
            w.write(sb.toString());
        }
        return out.getAbsolutePath();
    }

    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }
    private static String escape(String s) { return s.replace("\\","\\\\").replace("\"","\\\""); }
}
