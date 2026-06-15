package com.thesis.workout.benchmark;

import java.util.Arrays;

/**
 * Latency summary (in milliseconds) over a set of measured query executions. Percentiles use the
 * nearest-rank method on the sorted samples.
 */
record LatencyStats(double p50, double p95, double p99, double mean, double min, double max) {

    static LatencyStats of(long[] nanos) {
        if (nanos.length == 0) {
            return new LatencyStats(0, 0, 0, 0, 0, 0);
        }
        long[] sorted = nanos.clone();
        Arrays.sort(sorted);
        double sum = 0;
        for (long n : sorted) {
            sum += n;
        }
        return new LatencyStats(
                ms(percentile(sorted, 50)),
                ms(percentile(sorted, 95)),
                ms(percentile(sorted, 99)),
                ms((long) (sum / sorted.length)),
                ms(sorted[0]),
                ms(sorted[sorted.length - 1]));
    }

    /** Single-threaded throughput estimate (queries/sec) derived from the mean. */
    double throughput() {
        return mean > 0 ? 1000.0 / mean : 0;
    }

    private static long percentile(long[] sorted, int p) {
        int rank = (int) Math.ceil(p / 100.0 * sorted.length);
        int index = Math.min(Math.max(rank - 1, 0), sorted.length - 1);
        return sorted[index];
    }

    private static double ms(long nanos) {
        return Math.round(nanos / 1_000.0) / 1_000.0; // nanos -> ms, 3 decimals
    }
}
