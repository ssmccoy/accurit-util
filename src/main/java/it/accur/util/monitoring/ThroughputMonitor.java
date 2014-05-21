package it.accur.util.monitoring;

import it.accur.util.concurrent.ConcurrentCircularBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An efficient throughput measurement.
 *
 * <p>This class provides a simple measurement of throughput for monitoring
 * using time-sampling of a fixed number of requests.  It stores the sample
 * times in a circular buffer of the specified size.  It then uses the duration
 * of the sampled area to calculate the average number of requests per the
 * specified time unit.  Both the collection of the samples and the calculation
 * of the throughput measurement are <em>O(1)</em>.</p>
 *
 * <h3>Synopsis</h3>
 *
 * <pre>
 * public MyService {
 *     private ThroughputMonitor throughputMonitor;
 *
 *     public MyService (final ThroughputMonitor throughputMonitor) {
 *         this.throughputMonitor = throughputMonitor;
 *     }
 *
 *     public void handleAction () {
 *         throughputMonitor.count();
 *     }
 * }
 * </pre>
 *
 * <h3>Setup</h3>
 *
 * <p>To use this class for counting using Spring's management extensions, use
 * define it as a singleton in your <code>beans.xml</code>.
 * </p>
 *
 * <pre>
 * &lt;bean id="signup-monitor"
 *   class="it.accur.util.monitoring.ThroughputMonitor"&gt;
 *    &lt;constructor-arg&gt;${monitoring.signups.samplesize}&lt;/constructor-arg&gt;
 *    &lt;constructor-arg&gt;${monitoring.signups.unit}&lt;/constructor-arg&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * <p>Then configure spring to export the bean using the MBeanExporter.  It's
 * suggested you restrict the methods exposed via the management interface to
 * the {@link getRate()} property.</p>
 *
 * <pre>
 * &lt;bean id="exporter" class="org.springframework.jmx.export.MBeanExporter"&gt;
 *     &lt;property name="beans"&gt;
 *       &lt;map&gt;
 *         &lt;entry key="bean:name=signup-monitor" value-ref="signup-monitor"/&gt;
 *       &lt;/map&gt;
 *     &lt;/property&gt;
 *     &lt;property name="assembler"&gt;
 *       &lt;bean class="org.springframework.jmx.export.assembler.MethodNameBasedMBeanInfoAssembler"&gt;
 *         &lt;property name="methodMappings"&gt;
 *           &lt;props&gt;
 *             &lt;prop key="bean:name=signup-monitor"&gt;getRate&lt;/prop&gt;
 *           &lt;/props&gt;
 *         &lt;/property&gt;
 *       &lt;/bean&gt;
 *     &lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * <p><em>This object uses nanosecond measurements, but does not guarantee
 * nanosecond accuracy on all platforms.</em>  See {@link
 * System.nanoTime()}</p>
 *
 * <p>This class is thread safe.</p>
 *
 * @author <a href="mailto:ssmccoy@blisted.org">Scott S. McCoy</a>
 */
public class ThroughputMonitor {
    private final ConcurrentCircularBuffer <Long> samples;
    private final TimeUnit timeUnit;

    /**
     * Create a new counting monitor.
     *
     * @param sampleSize The size of the sampling buffer, affects accuracy and
     * diviation rate.
     * @param timeUnit The unit in which to calculate results.
     *
     * @throws IllegalArgumentException If the sample size is a non-positive
     * value.
     * @throws IllegalArgumentException If the timeUnit is null.
     */
    public ThroughputMonitor (final int sampleSize, final TimeUnit timeUnit) {
        this.samples  = new ConcurrentCircularBuffer <Long> (Long.class,
                                                             sampleSize);
        this.timeUnit = timeUnit;
    }

    /**
     * Create a new counting monitor with a default time resolution of
     * nanoseconds.
     *
     * @param sampleSize The size of the sampling buffer, affects accuracy and
     * diviation rate.
     *
     * @throws IllegalArgumentException If the sample size is a non-positive
     * value.
     */
    public ThroughputMonitor (final int sampleSize) {
        this.samples  = new ConcurrentCircularBuffer <Long> (Long.class,
                                                             sampleSize);
        this.timeUnit = TimeUnit.NANOSECONDS;
    }

    /**
     * Add to the count.
     *
     * <p>Count a new item.</p>
     */
    public void count () {
        samples.add(System.nanoTime());
    }

    /**
     * Calculate the current rate at which items are being counted.
     *
     * <p>Determines based on the time-samples of the counts currently
     * available in the buffer what the average rate of counting is.</p>
     *
     * @param unit The time unit by which to calculate the rate of throughput.
     * @return The current rate of throughput per given time-unit.
     *
     * @throws IllegalArgumentException if the provided conversion unit is
     * null.
     */
    public long getRate (final TimeUnit conversionUnit) {
        if (conversionUnit == null) {
            throw new IllegalArgumentException(
                "A conversion unit is required"
                );
        }

        Long[] snapshot = samples.snapshot();

        if (snapshot.length == 0) {
            return 0L;
        }

        /* Rather than converting down (which reduces resolution, and increases
         * possibility of a duration of zero) the timestamps of the sample,
         * convert up the size of the sample relative to the provided
         * conversion unit and provide a rate based on the average requests per
         * nanosecond.
         */
        long duration = snapshot[snapshot.length - 1] - snapshot[0];

        long statesize = TimeUnit.NANOSECONDS.convert(snapshot.length,
                                                      conversionUnit);

        if (duration > 0) {
            return statesize / duration;
        }
        else {
            /* If we for some reason have a zero-nanosecond sample, just
             * return some ceiling amount (or floored amount).
             */
            return snapshot.length == 1 ? 1 : statesize;
        }
    }

    /**
     * Calculate the current rate of throughput.
     *
     * @return The current rate of throughput per default time-unit.
     */
    public long getRate () {
        return getRate(timeUnit);
    }
}
