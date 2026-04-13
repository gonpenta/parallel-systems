package com.benchmark.core;

import java.util.Arrays;
import java.util.List;

/**
 * Immutable configuration for a benchmark run.
 * Build via {@link BenchmarkConfig.Builder}.
 */
public final class BenchmarkConfig {

    private final List<Integer> concurrencyLevels;
    private final int metricsSampleIntervalMs;
    private final String outputDirectory;
    private final boolean generateHtmlReport;
    private final boolean generateCharts;
    private final boolean verbose;
    private final int taskTimeoutSeconds;

    private BenchmarkConfig(Builder b) {
        this.concurrencyLevels        = b.concurrencyLevels;
        this.metricsSampleIntervalMs  = b.metricsSampleIntervalMs;
        this.outputDirectory          = b.outputDirectory;
        this.generateHtmlReport       = b.generateHtmlReport;
        this.generateCharts           = b.generateCharts;
        this.verbose                  = b.verbose;
        this.taskTimeoutSeconds       = b.taskTimeoutSeconds;
    }

    public List<Integer> getConcurrencyLevels()  { return concurrencyLevels; }
    public int getMetricsSampleIntervalMs()       { return metricsSampleIntervalMs; }
    public String getOutputDirectory()            { return outputDirectory; }
    public boolean isGenerateHtmlReport()         { return generateHtmlReport; }
    public boolean isGenerateCharts()             { return generateCharts; }
    public boolean isVerbose()                    { return verbose; }
    public int getTaskTimeoutSeconds()            { return taskTimeoutSeconds; }

    // -------------------------------------------------------------------------

    public static Builder builder() { return new Builder(); }

    public static final class Builder {

        private List<Integer> concurrencyLevels       = Arrays.asList(1, 2, 4, 8, 16, 32);
        private int           metricsSampleIntervalMs = 200;
        private String        outputDirectory         = "benchmark-results";
        private boolean       generateHtmlReport      = true;
        private boolean       generateCharts          = true;
        private boolean       verbose                 = false;
        private int           taskTimeoutSeconds      = 300;

        /**
         * Thread counts at which each task will be exercised.
         * E.g. {@code concurrencyLevels(1, 2, 4, 8, 16, 32)}
         */
        public Builder concurrencyLevels(Integer... levels) {
            this.concurrencyLevels = Arrays.asList(levels);
            return this;
        }

        /**
         * How often (ms) the metrics collector samples CPU and memory.
         * Lower = more accurate averages, higher CPU overhead of collection.
         */
        public Builder metricsSampleIntervalMs(int ms) {
            this.metricsSampleIntervalMs = ms;
            return this;
        }

        /** Directory where PNGs, JSON, and the HTML report are written. */
        public Builder outputDirectory(String dir) {
            this.outputDirectory = dir;
            return this;
        }

        public Builder generateHtmlReport(boolean b) {
            this.generateHtmlReport = b;
            return this;
        }

        public Builder generateCharts(boolean b) {
            this.generateCharts = b;
            return this;
        }

        /** Print per-iteration timings to stdout. */
        public Builder verbose(boolean b) {
            this.verbose = b;
            return this;
        }

        /** Hard wall-clock cap per concurrency level to prevent hangs. */
        public Builder taskTimeoutSeconds(int s) {
            this.taskTimeoutSeconds = s;
            return this;
        }

        public BenchmarkConfig build() {
            if (concurrencyLevels == null || concurrencyLevels.isEmpty())
                throw new IllegalStateException("At least one concurrency level is required");
            if (metricsSampleIntervalMs < 10)
                throw new IllegalStateException("Sample interval must be >= 10 ms");
            return new BenchmarkConfig(this);
        }
    }
}
