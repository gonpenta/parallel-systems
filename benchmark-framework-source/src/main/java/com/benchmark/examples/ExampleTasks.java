package com.benchmark.examples;

import com.benchmark.core.BenchmarkTask;

import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

// =============================================================================
// EXAMPLE 1 – In-memory sort (CPU-bound)
// =============================================================================

/**
 * Sorts a randomly generated integer array on every iteration.
 * Good for measuring pure CPU parallelism scaling.
 */
class SortingTask implements BenchmarkTask {

    private final int arraySize;

    SortingTask(int arraySize) { this.arraySize = arraySize; }

    @Override public String name() { return "Array Sort (" + arraySize + " ints)"; }

    @Override
    public void execute(int workerId) {
        int[] data = new int[arraySize];
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < data.length; i++) data[i] = rng.nextInt();
        Arrays.sort(data);
    }

    @Override public int iterationsPerWorker() { return 200; }
    @Override public int warmupIterations()    { return 20; }
}

// =============================================================================
// EXAMPLE 2 – SHA-256 hashing (CPU + allocation)
// =============================================================================

/**
 * Hashes a random 4KB payload with SHA-256 on every iteration.
 * Exercises both CPU and object allocation.
 */
class HashingTask implements BenchmarkTask {

    private static final int PAYLOAD_SIZE = 4096;
    private final ThreadLocal<MessageDigest> digestLocal;

    HashingTask() {
        digestLocal = ThreadLocal.withInitial(() -> {
            try { return MessageDigest.getInstance("SHA-256"); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
    }

    @Override public String name() { return "SHA-256 Hashing (4 KB)"; }

    @Override
    public void execute(int workerId) throws Exception {
        byte[] payload = new byte[PAYLOAD_SIZE];
        ThreadLocalRandom.current().nextBytes(payload);
        MessageDigest md = digestLocal.get();
        md.reset();
        md.digest(payload);
    }

    @Override public int iterationsPerWorker() { return 500; }
    @Override public int warmupIterations()    { return 50; }
}

// =============================================================================
// EXAMPLE 3 – Simulated I/O (mixed blocking + compute)
// =============================================================================

/**
 * Simulates an I/O-bound operation: a short sleep + lightweight computation.
 * Demonstrates how throughput scales when tasks block (ideal for thread-pool tuning).
 */
class SimulatedIOTask implements BenchmarkTask {

    private final int sleepMs;

    SimulatedIOTask(int sleepMs) { this.sleepMs = sleepMs; }

    @Override public String name() { return "Simulated I/O (" + sleepMs + " ms latency)"; }

    @Override
    public void execute(int workerId) throws Exception {
        Thread.sleep(sleepMs);
        // Lightweight computation after "I/O" returns
        double acc = 0;
        for (int i = 0; i < 1000; i++) acc += Math.sqrt(i);
        // Prevent dead-code elimination
        if (acc < 0) throw new RuntimeException("unreachable");
    }

    @Override public int iterationsPerWorker() { return 20; }
    @Override public int warmupIterations()    { return 3; }
}

// =============================================================================
// EXAMPLE 4 – HashMap concurrent reads/writes
// =============================================================================

/**
 * Exercises a ConcurrentHashMap with a 90/10 read/write mix.
 * Shows contention patterns as concurrency increases.
 */
class MapTask implements BenchmarkTask {

    private final java.util.concurrent.ConcurrentHashMap<Integer, String> map
            = new java.util.concurrent.ConcurrentHashMap<>();

    @Override public String name() { return "ConcurrentHashMap (90% read)"; }

    @Override
    public void setup() {
        for (int i = 0; i < 10_000; i++) map.put(i, "value-" + i);
    }

    @Override
    public void execute(int workerId) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int key = rng.nextInt(10_000);
        if (rng.nextInt(10) == 0) {        // 10 % writes
            map.put(key, "v-" + workerId);
        } else {                           // 90 % reads
            map.get(key);
        }
    }

    @Override public int iterationsPerWorker() { return 1000; }
    @Override public int warmupIterations()    { return 100; }
}

// =============================================================================
// Public factory — used by Main
// =============================================================================

public final class ExampleTasks {
    private ExampleTasks() {}

    public static BenchmarkTask sortSmall()   { return new SortingTask(10_000); }
    public static BenchmarkTask sortLarge()   { return new SortingTask(1_000_000); }
    public static BenchmarkTask hashing()     { return new HashingTask(); }
    public static BenchmarkTask simulatedIO() { return new SimulatedIOTask(10); }
    public static BenchmarkTask mapOps()      { return new MapTask(); }
}
