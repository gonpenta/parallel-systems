package com.benchmark.core;

import com.benchmark.metrics.MetricsCollector;
import com.benchmark.metrics.MetricsSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Drives a {@link BenchmarkTask} across all configured concurrency levels.
 *
 * <p>For each level the runner:
 * <ol>
 *   <li>Runs warm-up iterations (discarded)</li>
 *   <li>Starts the metrics-collection background thread</li>
 *   <li>Launches N worker threads, each executing the task repeatedly</li>
 *   <li>Waits for all workers to finish (or times out)</li>
 *   <li>Stops the metrics collector</li>
 *   <li>Assembles a {@link BenchmarkResult}</li>
 * </ol>
 * </p>
 */
public final class BenchmarkRunner {

    private final BenchmarkConfig config;

    public BenchmarkRunner(BenchmarkConfig config) {
        this.config = config;
    }

    /**
     * Run a single task across all concurrency levels.
     *
     * @return one result per concurrency level
     */
    public List<BenchmarkResult> run(BenchmarkTask task) throws Exception {
        List<BenchmarkResult> results = new ArrayList<>();

        System.out.printf("%n╔══════════════════════════════════════════════════════╗%n");
        System.out.printf("║  Benchmarking: %-37s║%n", task.name());
        System.out.printf("╚══════════════════════════════════════════════════════╝%n");

        task.setup();

        for (int concurrency : config.getConcurrencyLevels()) {
            System.out.printf("%n  ▶  concurrency = %d%n", concurrency);
            BenchmarkResult result = runLevel(task, concurrency);
            results.add(result);
            System.out.println("     " + result);

            // Brief pause between levels to let the JVM settle
            Thread.sleep(500);
        }

        task.teardown();
        return results;
    }

    // -------------------------------------------------------------------------

    private BenchmarkResult runLevel(BenchmarkTask task, int concurrency) throws Exception {

        // --- warm-up phase (not measured) ---
        runWorkers(task, concurrency, task.warmupIterations(), null, null, null);

        // --- measured phase ---
        List<Long> iterationTimesNs     = new CopyOnWriteArrayList<>();
        AtomicLong totalIterations      = new AtomicLong();
        AtomicInteger errorCount        = new AtomicInteger();
        MetricsCollector metricsCollector = new MetricsCollector(config.getMetricsSampleIntervalMs());

        long wallStart = System.currentTimeMillis();
        metricsCollector.start();

        runWorkers(task, concurrency, task.iterationsPerWorker(),
                   iterationTimesNs, totalIterations, errorCount);

        List<MetricsSnapshot> snapshots = metricsCollector.stop();
        long wallEnd = System.currentTimeMillis();
        metricsCollector.shutdown();

        long totalDurationMs = wallEnd - wallStart;

        if (errorCount.get() > 0) {
            System.out.printf("     ⚠  %d iteration(s) threw exceptions (included in timing)%n",
                              errorCount.get());
        }

        return new BenchmarkResult(
                task.name(),
                concurrency,
                totalIterations.get(),
                totalDurationMs,
                iterationTimesNs,
                snapshots,
                MetricsCollector.averageCpu(snapshots),
                MetricsCollector.peakCpu(snapshots),
                MetricsCollector.averageMemoryMB(snapshots),
                MetricsCollector.peakMemoryMB(snapshots)
        );
    }

    /**
     * Spin up {@code concurrency} threads, each running {@code iterations} times.
     * If {@code timings} / {@code counter} are null this is a warm-up run.
     */
    private void runWorkers(BenchmarkTask task,
                            int concurrency,
                            int iterations,
                            List<Long> timings,
                            AtomicLong counter,
                            AtomicInteger errors) throws Exception {

        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        CountDownLatch ready  = new CountDownLatch(concurrency);
        CountDownLatch start  = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int w = 0; w < concurrency; w++) {
            final int workerId = w;
            futures.add(pool.submit(() -> {
                ready.countDown();
                try { start.await(); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int i = 0; i < iterations; i++) {
                    long t0 = System.nanoTime();
                    try {
                        task.execute(workerId);
                    } catch (Exception ex) {
                        if (errors != null) errors.incrementAndGet();
                        if (config.isVerbose())
                            System.err.printf("  Worker %d iteration %d failed: %s%n",
                                              workerId, i, ex.getMessage());
                    }
                    long elapsed = System.nanoTime() - t0;
                    if (timings  != null) timings.add(elapsed);
                    if (counter  != null) counter.incrementAndGet();
                }
            }));
        }

        // Wait for all workers to be ready, then release them simultaneously
        ready.await();
        start.countDown();

        pool.shutdown();
        boolean finished = pool.awaitTermination(config.getTaskTimeoutSeconds(), TimeUnit.SECONDS);
        if (!finished) {
            pool.shutdownNow();
            System.out.printf("  ⚠  Concurrency level %d timed out after %ds%n",
                              concurrency, config.getTaskTimeoutSeconds());
        }
    }
}
