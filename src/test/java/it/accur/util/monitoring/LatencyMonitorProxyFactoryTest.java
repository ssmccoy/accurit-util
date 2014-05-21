package it.accur.util.monitoring;
import org.testng.annotations.Test;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ArrayBlockingQueue;
import javax.management.AttributeList;
import javax.management.MBeanInfo;
import javax.management.MBeanAttributeInfo;

@Test(groups={"unit"}, testName = "Proxy monitor verification test")
public class LatencyMonitorProxyFactoryTest {
    private LatencyMonitorProxyFactory proxyFactory = 
        new LatencyMonitorProxyFactory(256, TimeUnit.MILLISECONDS);

    static class Sleeper implements Runnable {
        public void run () {
            try {
                TimeUnit.MILLISECONDS.sleep(20);
            }
            catch (InterruptedException exception) {
                throw new IllegalStateException(
                    "Unexpected interruption", exception
                    );
            }
        }
    }

    public void test () 
    throws InterruptedException {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            50, 50, 1, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1024)
            );

        for (int i = 0; i < 1000; i++) {
            Sleeper sleeper = new Sleeper();

            Runnable job = proxyFactory.create(Runnable.class, sleeper);

            executor.execute(job);
        }

        executor.shutdown();
        executor.awaitTermination(20, TimeUnit.SECONDS);

        Object latency = proxyFactory.getAttribute(
            "public void it.accur.util.monitoring.LatencyMonitorProxyFactoryTest$Sleeper.run()"
            );

        assert latency != null :
            "Expected to find method " + Sleeper.class.getName() + ".run";

        assert latency instanceof Long :
            "Expected property to be a Long";

        assert 10L < (Long) latency :
            "Expected latency of execution to be atleast 10ms: " + latency + "ms";

        assert proxyFactory.getAttribute("missing") == null :
            "Expected illogical attributes to be undiscoverable";

        MBeanInfo mbeanInfo = proxyFactory.getMBeanInfo();

        MBeanAttributeInfo[] attributeInfos = mbeanInfo.getAttributes();

        assert attributeInfos.length > 0 :
            "Expected to find some attributes";
    }
}
