package com.benchmark.core;

import com.benchmark.metrics.MetricsSnapshot;

import java.util.Collections;
import java.util.List;
import java.util.LongSummaryStatistics;

/**
 * Immutable record of all metrics captured for one (task, concurrencyLevel) pair.
 */
public final class BenchmarkResult {

    private final String            taskName;
    private final int               concurrencyLevel;
    private final long              totalIterations;
    private final long              totalDurationMs;
    private final List<Long>        iterationTimesNs;    // per-iteration latencies
    private final List<MetricsSnapshot> metricsSnapshots;

    // Pre-computed aggregates
    private final double avgCpuPercent;
    private final double peakCpuPercent;
    private final double avgMemoryMB;
    private final double peakMemoryMB;

    public BenchmarkResult(String taskName,
                           int concurrencyLevel,
                           long totalIterations,
                           long totalDurationMs,
                           List<Long> iterationTimesNs,
                           List<MetricsSnapshot> metricsSnapshots,
                           double avgCpuPercent,
                           double peakCpuPercent,
                           double avgMemoryMB,
                           double peakMemoryMB) {
        this.taskName          = taskName;
        this.concurrencyLevel  = concurrencyLevel;
        this.totalIterations   = totalIterations;
        this.totalDurationMs   = totalDurationMs;
        this.iterationTimesNs  = Collections.unmodifiableList(iterationTimesNs);
        this.metricsSnapshots  = Collections.unmodifiableList(metricsSnapshots);
        this.avgCpuPercent     = avgCpuPercent;
        this.peakCpuPercent    = peakCpuPercent;
        this.avgMemoryMB       = avgMemoryMB;
        this.peakMemoryMB      = peakMemoryMB;
    }

    // -------------------------------------------------------------------------
    // Derived metrics

    /** Operations per second across the whole run. */
    public double getThroughput() {
        return totalDurationMs > 0
                ? (totalIterations * 1000.0) / totalDurationMs
                : 0;
    }

    /** Average latency per iteration in milliseconds. */
    public double getAvgLatencyMs() {
        if (iterationTimesNs.isEmpty()) return 0;
        LongSummaryStatistics s = iterationTimesNs.stream()
                .mapToLong(Long::longValue).summaryStatistics();
        return s.getAverage() / 1_000_000.0;
    }

    /** Median latency in ms. */
    public double getMedianLatencyMs() {
        if (iterationTimesNs.isEmpty()) return 0;
        List<Long> sorted = new java.util.ArrayList<>(iterationTimesNs);
        Collections.sort(sorted);
        int mid = sorted.size() / 2;
        return sorted.size() % 2 == 0
                ? (sorted.get(mid - 1) + sorted.get(mid)) / 2_000_000.0
                : sorted.get(mid) / 1_000_000.0;
    }

    /** 95th-percentile latency in ms. */
    public double getP95LatencyMs() { return percentileMs(95); }

    /** 99th-percentile latency in ms. */
    public double getP99LatencyMs() { return percentileMs(99); }

    private double percentileMs(int p) {
        if (iterationTimesNs.isEmpty()) return 0;
        List<Long> sorted = new java.util.ArrayList<>(iterationTimesNs);
        Collections.sort(sorted);
        int idx = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(idx, sorted.size() - 1))) / 1_000_000.0;
    }

    // -------------------------------------------------------------------------
    // Accessors

    public String               getTaskName()         { return taskName; }
    public int                  getConcurrencyLevel() { return concurrencyLevel; }
    public long                 getTotalIterations()  { return totalIterations; }
    public long                 getTotalDurationMs()  { return totalDurationMs; }
    public List<Long>           getIterationTimesNs() { return iterationTimesNs; }
    public List<MetricsSnapshot> getMetricsSnapshots() { return metricsSnapshots; }
    public double               getAvgCpuPercent()   { return avgCpuPercent; }
    public double               getPeakCpuPercent()  { return peakCpuPercent; }
    public double               getAvgMemoryMB()     { return avgMemoryMB; }
    public double               getPeakMemoryMB()    { return peakMemoryMB; }

    @Override
    public String toString() {
        return String.format(
            "[%s | concurrency=%-3d] throughput=%,.1f ops/s  avgLatency=%6.2f ms  " +
            "p99=%6.2f ms  avgCPU=%5.1f%%  peakMem=%6.1f MB",
            taskName, concurrencyLevel,
            getThroughput(), getAvgLatencyMs(), getP99LatencyMs(),
            avgCpuPercent, peakMemoryMB
        );
    }
}
