package it.accur.util.concurrent;

import java.util.Collections;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A synchronization point intended to make threads await correction.
 *
 * <p>This synchronization point allows jobs to wait until an action has been
 * taken when a given error occurs.  Any number of threads may suspend
 * themselves in error, which makes their reason for suspension available for
 * soliciation.</p>
 *
 * <p>This can be used with JMX by exposing a single version of this object
 * using spring's dynamic mbean integration.  Exposing {@link #resume()} as an
 * executable method in JMX allows can be used to enable operational
 * intervention for problems correctable during runtime, such as unavailable
 * external resources.</p> 
 */
public class RunnableSuspender {
    public static class Suspension {
        private final Thread    thread;
        private final Runnable  job;
        private final Exception exception;

        Suspension (final Thread thread, 
                    final Runnable job, 
                    final Exception exception) 
        {
            this.thread    = thread;
            this.job       = job;
            this.exception = exception;
        }

        public Runnable getJob () {
            return job;
        }

        public Exception getException () {
            return exception;
        }

        public Thread getThread () {
            return thread;
        }

        public String toString () {
            return String.format(
                "[thread: %s, job: %s, exception: %s]",
                thread.toString(), job.toString(), exception.toString()
                );
        }
    }

    /* A linked blocking queue with no capacity does not actually block. */
    private BlockingQueue <Suspension> suspensions =
        new LinkedBlockingQueue <Suspension> ();

    /**
     * Suspend the current thread, calling the given job when finished.
     *
     * <p>This method suspends the current thread, running the provided job
     * when the suspension is interrupted.</p>
     *
     * @param job The job to suspend.
     * @param cause The cause of the suspension.
     */
    public void suspend (final Runnable job, final Exception cause) {
        Suspension suspension = new Suspension(
            Thread.currentThread(), job, cause
            );

        suspensions.add(suspension);

        try {
            synchronized (suspension) {
                suspension.wait();

                job.run();
            }
        }
        catch (InterruptedException exception) {
            /* Cheesy way to fail violently */
            throw new IllegalStateException(
                "Waiting thread has been interrupted, job lost",
                exception
                );
        }
    }

    /**
     * Resume all suspended jobs.
     *
     * <p>Resumes all jobs currently suspended by this suspender, removing them
     * from the queue.</p>
     */
    public void resume () {
        Iterator <Suspension> iterator = suspensions.iterator();

        while (iterator.hasNext()) {
            Suspension suspension = iterator.next();

            synchronized (suspension) {
                suspension.notify();

                iterator.remove();
            }
        }
    }

    /**
     * Fetch the current suspensions.
     *
     * @return An <em>unmodifiable</em> collection of the currently suspended
     * jobs.
     */
    public Collection <Suspension> getSuspensions () {
        return Collections.unmodifiableCollection(suspensions);
    }

    /**
     * Fetch descriptions of the current suspensions.
     *
     * <p>Simply calls {@link Suspension#toString()} on all suspensions and
     * returns the corresponding list of strings.  The complex type of
     * Suspension is exposable through JMX.</p>
     */
    public Exception[] getCauses () {
        /* Take a snapshot first so it's stable */
        Suspension[] jobs = suspensions.toArray(new Suspension[0]);

        Exception[] results = new Exception [jobs.length];

        for (int i = 0; i < jobs.length; i++) {
            results[i] = jobs[i].getException();
        }

        return results;
    }

    /**
     * The number of currently suspended jobs.
     *
     * @return The number of suspensions which will be released when resume is
     * called.
     */
    public int getSuspensionCount () {
        return suspensions.size();
    }

    public boolean hasSuspensions () {
        return suspensions.size() > 0;
    }
}
