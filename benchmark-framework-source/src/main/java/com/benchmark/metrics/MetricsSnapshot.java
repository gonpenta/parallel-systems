package com.benchmark.metrics;

/**
 * A snapshot of system metrics at one point in time.
 */
public final class MetricsSnapshot {

    private final long   timestampMs;
    private final double cpuUsagePercent;     // 0–100
    private final long   heapUsedBytes;
    private final long   heapMaxBytes;
    private final long   nonHeapUsedBytes;

    public MetricsSnapshot(long timestampMs,
                           double cpuUsagePercent,
                           long heapUsedBytes,
                           long heapMaxBytes,
                           long nonHeapUsedBytes) {
        this.timestampMs      = timestampMs;
        this.cpuUsagePercent  = cpuUsagePercent;
        this.heapUsedBytes    = heapUsedBytes;
        this.heapMaxBytes     = heapMaxBytes;
        this.nonHeapUsedBytes = nonHeapUsedBytes;
    }

    public long   getTimestampMs()      { return timestampMs; }
    public double getCpuUsagePercent()  { return cpuUsagePercent; }
    public long   getHeapUsedBytes()    { return heapUsedBytes; }
    public long   getHeapMaxBytes()     { return heapMaxBytes; }
    public long   getNonHeapUsedBytes() { return nonHeapUsedBytes; }

    /** Total JVM memory in MB (heap + non-heap). */
    public double getTotalMemoryMB() {
        return (heapUsedBytes + nonHeapUsedBytes) / (1024.0 * 1024.0);
    }

    /** Heap used as a percentage of max heap. */
    public double getHeapUsagePercent() {
        return heapMaxBytes > 0 ? (100.0 * heapUsedBytes / heapMaxBytes) : 0;
    }
}
