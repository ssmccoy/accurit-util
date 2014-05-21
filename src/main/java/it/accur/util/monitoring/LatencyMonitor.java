package it.accur.util.monitoring;

import it.accur.util.concurrent.ConcurrentCircularBuffer;
import java.util.concurrent.TimeUnit;

/**
 * An efficient average-latency measurement.
 *
 * <p>This object monitors latency by keeping a fixed-size circular buffer of
 * latency samples.  The samples are collected from the timer object and are
 * <em>O(1)</em> for collection and <em>O(n)</em> for calculation of the
 * measurement.</p>
 *
 * <p>This class is thread safe.</p>
 *
 * <h3>Synopsis</h3>
 *
 * <pre>
 * public MyService {
 *     private LatencyMonitor monitor;
 *
 *     public MyService (final LatencyMonitor monitor) {
 *         this.monitor = monitor;
 *     }
 *
 *     public void handleAction () {
 *         Timer timer = monitor.startTimer();
 *
 *         ...
 *
 *         timer.stop();
 *     }
 * }
 * </pre>
 *
 * <p><em>This object uses nanosecond measurements, but does not guarantee
 * nanosecond accuracy on all platforms.</em>  See {@link
 * System.nanoTime()}</p>
 *
 * @see ThroughputMonitor
 *
 * @author <a href="mailto:ssmccoy@blisted.org">Scott S. McCoy</a>
 */
public class LatencyMonitor {
    private final ConcurrentCircularBuffer <Long> samples;
    private final TimeUnit timeUnit;

    /**
     * Create a new latency monitor.
     * 
     * @param sampleSize The size of the sampling buffer, affects accuracy
     * and diviation rate.
     * @param timeUnit The default time unit.
     *
     * @throws IllegalArgumentException if the sample size is non-positive.
     * @throws IllegalArgumentException if the given time unit is null.
     */
    public LatencyMonitor (final int sampleSize, final TimeUnit timeUnit) {
        this.samples  = new ConcurrentCircularBuffer <Long> (Long.class,
                                                             sampleSize);
        this.timeUnit = timeUnit;
    }

    /**
     * Create a new latency monitor with nanosecond resolution.
     * 
     * @param sampleSize The size of the sampling buffer, affects accuracy
     * and diviation rate.
     *
     * @throws IllegalArgumentException if the sample size is non-positive.
     */
    public LatencyMonitor (final int sampleSize) {
        this.samples  = new ConcurrentCircularBuffer <Long> (Long.class,
                                                             sampleSize);
        this.timeUnit = TimeUnit.NANOSECONDS;
    }

    /**
     * Create a new timer.
     *
     * <p>Create a timer which starts <em>now</em> and return it, so it may be
     * stopped in the future.</p>
     *
     * @return A timer.
     * @see Timer
     */
    public Timer startTimer () {
        return new Timer(this);
    }

    /**
     * Add a new timer to the sample.
     *
     * @param timer The timer to add.
     */
    void addTimer (Timer timer) {
        samples.add(timer.getDuration());
    }

    /**
     * Calculate the average duration of the sample.
     *
     * <p>Determines the average time duration of the sample as a long value
     * representing the given time unit.</p>
     *
     * @param conversionUnit The time unit to convert the result to.
     *
     * @throws IllegalArgumentException if the provided conversion unit is
     * null.
     *
     * @return The average duration of the timers in the sample.
     */
    public long getAverageDuration (TimeUnit conversionUnit) {
        if (conversionUnit == null) {
            throw new IllegalArgumentException(
                "A conversion unit is required"
                );
        }

        Long[] snapshot = samples.snapshot();

        if (snapshot.length == 0) {
            return 0L;
        }

        long totalDuration = 0L;

        for (long duration : snapshot) {
            totalDuration += duration;
        }

        return conversionUnit.convert(totalDuration / snapshot.length,
                                      TimeUnit.NANOSECONDS);
    }

    /**
     * Calculate the average duration of the sample based on the default time
     * unit.
     *
     * <p>Determines the average duration of the sample as a long value
     * representing the default time unit for this monitor.</p>
     *
     * @return The average duration of the timers in the sample.
     */
    public long getAverageDuration () {
        return getAverageDuration(timeUnit);
    }
}
