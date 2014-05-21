package it.accur.util.concurrent;
import org.testng.annotations.Test;
import java.util.concurrent.TimeUnit;

@Test(groups={"unit"}, testName = "Runnable Suspender Unit Test")
public class RunnableSuspenderTest {
    static class ReruningJob
    implements Runnable {
        boolean suspend = true;
        int counter = 0;

        RunnableSuspender suspender;

        ReruningJob (final RunnableSuspender suspender) 
        {
            this.suspender = suspender;
        }

        public void run () {
            counter++;

            if (suspend) {
                suspend = false;

                suspender.suspend(this, new Exception("Testing..."));
            }
        }
    }

    public void testSuspender () 
    throws Exception {
        RunnableSuspender suspender = new RunnableSuspender();
        ReruningJob job = new ReruningJob(suspender);

        Thread testThread = new Thread(job);

        testThread.start();

        /* Wait until there are some suspsensions, we're expecting one so this
         * should never loop indefinitely
         */
        while (! suspender.hasSuspensions()) {
            Thread.yield();
        }

        int suspended = suspender.getSuspensionCount();

        assert suspended == 1 :
            "Expecting one suspended job, found: " + suspended;


        suspender.resume();

        testThread.join();

        assert job.counter == 2 :
            "Expected run to have been called twice on our job, observed: " +
            job.counter;
    }
}
