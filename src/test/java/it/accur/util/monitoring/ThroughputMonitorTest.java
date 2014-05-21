package it.accur.util.monitoring;

import org.testng.annotations.Test;
import java.util.concurrent.TimeUnit;

/**
 * Attempts to generate 25 requests per second.
 *
 * <p>This test is capable of failing on an unusually slow CPU.</p>
 *
 * @author <a href="mailto:ssmccoy@blisted.org">Scott S. McCoy</a>
 */
@Test(groups={"unit"}, testName="Positive test of ThroughputMonitor")
public class ThroughputMonitorTest {
    public void testMonitor () 
    throws Exception {
        ThroughputMonitor monitor = new ThroughputMonitor(
            25, TimeUnit.SECONDS);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 50; i++) {
            long sleepms = (50 * i) - (System.currentTimeMillis() - startTime);

            if (sleepms > 0) {
                Thread.sleep(sleepms);
            }

            monitor.count();
        }

        long rate = monitor.getRate();

        assert rate > 19 && rate < 22 :
            "Expected rate to be approximately 20 requests a second: " + rate;
    }
}
