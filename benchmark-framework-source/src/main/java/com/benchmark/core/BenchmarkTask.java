package com.benchmark.core;

/**
 * Contract for any workload to be benchmarked.
 *
 * <p>Implement this interface to plug any algorithm, I/O operation,
 * or service call into the framework. The framework will invoke
 * {@link #execute(int)} across varying concurrency levels and
 * automatically collect execution time, throughput, CPU, and memory metrics.</p>
 *
 * <pre>{@code
 * public class MyTask implements BenchmarkTask {
 *     public String name() { return "My Custom Task"; }
 *     public void setup()  { /* warm up resources *\/ }
 *     public void execute(int workerId) throws Exception {
 *         // your workload here
 *     }
 *     public void teardown() { /* release resources *\/ }
 * }
 * }</pre>
 */
public interface BenchmarkTask {

    /**
     * Human-readable name shown in reports and charts.
     */
    String name();

    /**
     * Optional one-time setup before any concurrency level runs.
     * Ideal for warming up thread pools, opening connections, etc.
     */
    default void setup() throws Exception {}

    /**
     * The actual work unit executed repeatedly by each worker thread.
     *
     * @param workerId zero-based index of the calling worker thread
     */
    void execute(int workerId) throws Exception;

    /**
     * Optional cleanup after all concurrency levels have finished.
     */
    default void teardown() throws Exception {}

    /**
     * Minimum number of iterations per worker per concurrency level.
     * Override to tune warm-up vs. measurement trade-off.
     */
    default int iterationsPerWorker() {
        return 100;
    }

    /**
     * Warm-up iterations discarded before metrics collection begins.
     */
    default int warmupIterations() {
        return 10;
    }
}
