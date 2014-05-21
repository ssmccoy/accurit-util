package it.accur.util.monitoring;

import org.testng.annotations.Test;
import java.util.concurrent.TimeUnit;
import java.util.Random;

/**
 * Simple test which simulates some latency to test the accuracy of the
 * LatencyMonitor.
 *
 * @author <a href="mailto:ssmccoy@blisted.org">Scott S. McCoy</a>
 */
@Test(groups={"unit"}, testName="Simulated latency test")
public class LatencyMonitorTest {
    private Random random = new Random(System.currentTimeMillis());

    public void testSimulatedLatency () 
    throws Exception {
        LatencyMonitor monitor = new LatencyMonitor(50);

        for (int i = 0; i < 200; i++) {
            Timer timer = monitor.startTimer();

            Thread.sleep(15, random.nextInt(5000));

            timer.stop();
        }

        long average = monitor.getAverageDuration(TimeUnit.MILLISECONDS);

        assert average <= 16 && average >= 15 :
            "Expected average duration in ms to be approximately 16:" +
            average;
    }
}
