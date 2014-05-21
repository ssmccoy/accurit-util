package it.accur.util.concurrent;

import it.accur.util.concurrent.AtomicInitializer;
import org.testng.annotations.Test;

@Test(groups={"unit"},testName="Atomic Initalizer Test")
public class AtomicInitializerTest {
    static class FairnessTest 
    implements Runnable {
        private AtomicInitializer initializer;
        private long              id;
        private boolean           wonRace     = false;

        FairnessTest (AtomicInitializer initializer, long id) {
            this.id          = id;
            this.initializer = initializer;
            this.wonRace     = false;
        }

        public void run () {
            try {
                Thread.sleep(id * 60L);

                if (initializer.isRequired()) {
                    initializer.complete();
                }
            }
            catch (InterruptedException exception) {
                assert false : "Unexpected interruption";
            }
        }
    }

    public void testFairness () {
        /* TODO Do we even want to do a fairness test?  That seems sketchy. */
    }

    public void testOutofboundsRetry () {
        boolean caughtException = false;

        AtomicInitializer initializer = new AtomicInitializer();

        try {
            initializer.retry();
        }
        catch (IllegalStateException exception) {
            caughtException = true;
        }

        assert caughtException : "Expected IllegalStateException was unseen";
    }

    public void testOutofboundsComplete () {
        boolean caughtException = false;

        AtomicInitializer initializer = new AtomicInitializer();

        try {
            initializer.complete();
        }
        catch (IllegalStateException exception) {
            caughtException = true;
        }

        assert caughtException : "Expected IllegalStateException was unseen";
    }

    public void testRetry ()
    throws InterruptedException {
        Runnable job = new Runnable () {
            public void run () {
                AtomicInitializer initializer = new AtomicInitializer();

                assert initializer.isRequired() : 
                    "Atomic lock was not initialized correctly?";

                initializer.retry();

                assert initializer.isRequired() : "Reset after retry failed";

                initializer.complete();

                assert !initializer.isRequired() : 
                    "Reset after retry inconsistent state";
            }
        };

        Thread thread = new Thread(job);

        /* Sleep for 0.25 seconds, should be long enough */
        Thread.sleep(250L);

        if (thread.isAlive()) {
            thread.interrupt();

            assert false : "Unable to complete retry test within 250ms";
        }
    }
}
