package it.accur.util.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.AbstractExecutorService;
import java.util.Collections;
import java.util.List;

/* TODO: Should this really be a service at all?   Is it necessary as opposed
 * to simply an Executor? */
public class InlineExecutor 
extends AbstractExecutorService
implements ExecutorService {
    /**
     * Execute the given job inline.
     *
     * <p>Execute the given runnable object.</p>
     *
     * @param command The job to execute.
     */
    public void execute (Runnable command) {
        command.run();
    }

    public void shutdown () {
        /* NOOP for now */
    }

    public List <Runnable> shutdownNow () {
        return Collections.EMPTY_LIST;
    }

    public boolean awaitTermination (long timeout, TimeUnit unit) {
        /* NOOP for now */
        return false;
    }

    public boolean isTerminated () {
        return false;
    }

    public boolean isShutdown () {
        return false;
    }
}
