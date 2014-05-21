package it.accur.util.monitoring;

import java.util.concurrent.TimeUnit;

/**
 * A simple timer.
 *
 * <p>This is a simple timer which is used for collection realtime
 * duration-based measurements for monitoring.  It's intended to be used in
 * parallel with one of the duration based measurements.</p>
 *
 * <p>This object is <strong>not threadsafe</strong>.  It's expected usage is
 * that only {@link Timer} will be used by a given thread at any time.</p>
 *
 * <p><em>This object uses nanosecond measurements, but does not guarantee
 * nanosecond accuracy on all platforms.</em>  See {@link
 * System.nanoTime()}</p>
 *
 * @author <a href="mailto:ssmccoy@blisted.org">Scott S. McCoy</a>
 * @see LatencyMonitor
 */
public final class Timer {
    private final long start    = System.nanoTime();
    private long       stop     = -1L;
    private long       duration = 0L;

    /* TODO: If we use Timer in a place other than the LatencyMonitor, we'll
     * need to create a "DurationMonitor" or similarly named interface which
     * contains an addTimer(Timer) method.
     */
    private final LatencyMonitor monitor;

    /**
     * Create a new timer.
     *
     * <p>Create a new timer with a given parent monitor.</p>
     *
     * @param monitor The monitor to add this time duration to once it is
     * collected.
     */
    Timer (final LatencyMonitor monitor) {
        this.monitor = monitor;
    }

    /**
     * Stop the timer, and add it to it's monitor.
     *
     * <p>Stops the current timer (making {@link getDuration()} available), and
     * add the timer to it's parent monitor.</p>
     *
     * @throws IllegalStateException if it's called multiple times.
     */
    public void stop () {
        if (stop > 0L) {
            throw new IllegalStateException(
                "Attempted to stop a timer twice");
        }

        stop     = System.nanoTime();
        duration = stop - start;

        monitor.addTimer(this);
    }

    /**
     * Get the duration this timer ran.
     *
     * <p>Return the duration, in nanoseconds, that this timer ran for.</p>
     *
     * @return The duration, in nanoseconds.
     * @throws IllegalStateException if the timer has not yet been stopped.
     */
    public long getDuration () {
        if (stop < 0L) {
            throw new IllegalStateException(
                "Duration is not available until the timer has been stopped");
        }

        return duration;
    }

    /**
     * Get the duration this timer ran.
     *
     * <p>Given a TimeUnit, return the result of converting the measured
     * duration into the given unit of time.</p>
     * 
     * @param unit The unit to use for measurement.
     *
     * @throws IllegalStateException if the timer has not yet been stopped.
     * @return The duration in the given time unit of time.
     */
    public long getDuration (TimeUnit unit) {
        /* Call getter so it may enforce it's sanity checking */
        return unit.convert(getDuration(), TimeUnit.NANOSECONDS);
    }
}
