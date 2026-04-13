package com.benchmark.metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import com.sun.management.OperatingSystemMXBean;

/**
 * Runs a background sampling thread that collects CPU and memory snapshots
 * at a configurable interval.
 *
 * <p>Usage:
 * <pre>{@code
 *   MetricsCollector collector = new MetricsCollector(200);
 *   collector.start();
 *   // ... run workload ...
 *   List<MetricsSnapshot> samples = collector.stop();
 * }</pre>
 * </p>
 */
public final class MetricsCollector {

    private final int                       sampleIntervalMs;
    private final OperatingSystemMXBean     osMXBean;
    private final MemoryMXBean              memMXBean;
    private final List<MetricsSnapshot>     snapshots = new ArrayList<>();
    private final ScheduledExecutorService  scheduler;
    private ScheduledFuture<?>              future;

    public MetricsCollector(int sampleIntervalMs) {
        this.sampleIntervalMs = sampleIntervalMs;
        this.osMXBean  = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        this.memMXBean = ManagementFactory.getMemoryMXBean();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "metrics-collector");
            t.setDaemon(true);
            return t;
        });
    }

    /** Begin periodic sampling. */
    public synchronized void start() {
        snapshots.clear();
        future = scheduler.scheduleAtFixedRate(
                this::sample,
                0,
                sampleIntervalMs,
                TimeUnit.MILLISECONDS
        );
    }

    /** Stop sampling and return all collected snapshots (immutable). */
    public synchronized List<MetricsSnapshot> stop() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
        // One final sample
        sample();
        return Collections.unmodifiableList(new ArrayList<>(snapshots));
    }

    /** Shut down the background scheduler. Call when done with this collector. */
    public void shutdown() {
        scheduler.shutdownNow();
    }

    // -------------------------------------------------------------------------

    private synchronized void sample() {
        double cpuUsage = osMXBean.getProcessCpuLoad() * 100.0;
        if (cpuUsage < 0) cpuUsage = 0; // -1 means not yet available

        MemoryUsage heap    = memMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memMXBean.getNonHeapMemoryUsage();

        snapshots.add(new MetricsSnapshot(
                System.currentTimeMillis(),
                cpuUsage,
                heap.getUsed(),
                heap.getMax(),
                nonHeap.getUsed()
        ));
    }

    // -------------------------------------------------------------------------
    // Aggregate helpers used by BenchmarkResult

    public static double averageCpu(List<MetricsSnapshot> samples) {
        return samples.stream()
                .mapToDouble(MetricsSnapshot::getCpuUsagePercent)
                .average().orElse(0);
    }

    public static double peakCpu(List<MetricsSnapshot> samples) {
        return samples.stream()
                .mapToDouble(MetricsSnapshot::getCpuUsagePercent)
                .max().orElse(0);
    }

    public static double averageMemoryMB(List<MetricsSnapshot> samples) {
        return samples.stream()
                .mapToDouble(MetricsSnapshot::getTotalMemoryMB)
                .average().orElse(0);
    }

    public static double peakMemoryMB(List<MetricsSnapshot> samples) {
        return samples.stream()
                .mapToDouble(MetricsSnapshot::getTotalMemoryMB)
                .max().orElse(0);
    }
}
