package it.accur.util.concurrent;

import java.util.concurrent.Executor;

/**
 * Simple executor which executes the job it is given inline.
 *
 * <p>This executor is not a service, and has none of the properties of an
 * executor service.  This executor behaves exactly as the following anonymous
 * implementation would:
 *
 * <pre>
 * Executor executor = new Executor () {
 *     public void execute (Runnable job) {
 *         job.run();
 *     }
 * };
 * </pre>
 * </p>
 *
 * <p>An implementation other than the one prescribed above has been created
 * soley for injection in spring, and for general completeness.</p>
 */
public class CallerRunsExecutor 
implements Executor {

    /**
     * Execute the provided job, immediately.
     *
     * @param job The job to execute.
     */
    public void execute (Runnable job) {
        job.run();
    }
}
