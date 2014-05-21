package it.accur.util.concurrent;

import java.util.concurrent.locks.ReentrantLock;

/**
 * An atomic initialization test.
 *
 * <p>This class provides an atomic initialization test that will ensure a lazy
 * (or late-availability) initialization routine runs once, and only once.
 * This class is thread-safe and uses a combination of re-entrant exclusive
 * locking and optimized local tests to ensure that the test runs for the least
 * amount of time required, avoiding synchronization when it is not
 * reqiured.</p>
 *
 * <h3>Synopsis</h3>
 * <pre>
 * import it.accur.util.AtomicInitializer;
 *
 * class MyLazyFactory {
 *   private MyLazyFactory factory = null;
 *   private AtomicInitializer initializer = new AtomicInitializer();
 *
 *   private MyLazyFactory () {
 *   }
 *
 *   public MyLazyFactory getInstance () {
 *     if (initializer.isRequired()) {
 *       factory = new MyLazyFactory();
 *
 *       initializer.complete();
 *     }
 *
 *     return factory;
 *   }
 * }
 * </pre>
 *
 * <p>This initializer supports two basic modes, each with their own
 * semantics.  All {@link AtomicInitializer} instances are <a
 * href="#synchronized">Synchronized</a> unless otherwise specified.</p>
 *
 * <table>
 *  <thead>
 *   <tr>
 *    <th>Mode</th>
 *    <th>Description</th>
 *   </tr>
 *  </thead>
 *  <tbody>
 *   <tr>
 *    <td><a name="synchronized">Synchronized</a></td>
 *    <td>A synchronized initializer is guaranteed to run once, and blocks all
 *    subsequently calls to the {@link #isRequired()} method until
 *    initialization has been performed successfully.  It also guarantees that
 *    all subsequent invocations of the {@link #isRequired()} method are
 *    returned fairly on a first-come-first-serve basis.</td>
 *   </tr>
 *
 *   <tr>
 *    <td><a name="run-once">Run once</a></td>
 *    <td>A run-once initializer is guaranteed to run only once, but unlike the
 *    synchronized initializer calls to the {@link isRequired()} method are
 *    guaranteed to return immediately.  This is intended for initialization
 *    routines which are not required before an operation can be completed.</td>
 *   </tr>
 *  </tbody>
 * </table>
 *
 * @author <a href="mailto:ssmccoy@blisted.org">Scott S. McCoy</a>
 */
public class AtomicInitializer {
    private volatile boolean initialized = false;
    private boolean          synchronize = true;
    private ReentrantLock    lock;

    /**
     * Create a new atomic initalizer with default synchronization rules.
     */
    public AtomicInitializer () {
        this.synchronize = true;
        this.lock        = new ReentrantLock(true);
    }

    /**
     * Create a new atomic initializer with specified synchronization rules.
     *
     * <p>See the {@link AtomicInitializer} class description for more
     * information on synchronization rules.</p>
     *
     * @param synchronize <code>true</code> for a <a
     * href="#synchronized">synchronized</a> initializer, <code>false</code>
     * for a <a href="#run-once">run-once</a> initializer.
     */
    public AtomicInitializer (final boolean synchronize) {
        this.synchronize = synchronize;
        this.lock        = new ReentrantLock(synchronize);
    }

    /**
     * Determine if this initializer must be run.
     *
     * <p>Determines if this run-once operation should be run by the current
     * thread.  Calls to this operation will not synchronize if the execution
     * is not required, although they <em>may</em> block waiting until the
     * calling thread which was given the initialization dubty finishes
     * execution.</p>
     *
     * <p>Callers of this method <strong>must always</strong> call {@link
     * #complete()} if this method returns <code>true</code>.</p>
     *
     * @return true if the initializer should run, false otherwise.
     */
    public boolean isRequired () {
        if (!initialized) {
            if (synchronize) {
                lock.lock();

                if (!initialized) {
                    return true;
                }
                else {
                    lock.unlock();
                }
            }
            else {
                if (lock.tryLock()) {
                    initialized = true;

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Determine if this initializer has been run.
     *
     * <p>This method provides the oposite return values of {@link
     * #isRequired()} but behaves differently in the following ways.
     *
     * <ul>
     *  <li>It does not <em>synchronize</em> under any condition.</li>
     *  <li>It will may return true for initializers which have not fully
     *  completed for <em>run-once</em> initializers.</li>
     * </ul>
     *
     * @return true if the initializer has been dispatched, false otherwise.
     */
    public boolean isInitialized () {
        return initialized;
    }

    /**
     * Mark initialization complete.
     *
     * <p>Release the lock acquired for initialization.  For
     * <em>synchronized</em> initializers this will unblock any blocking calls
     * to {@link #isRequired()}, and cause all subsequent calls to skip the
     * locking routine.  For <em>run-once</em> initializers this operation will
     * cause subsequent calls to {@link #isRequired()} to avoid skip-check
     * locking.</p>
     *
     * @throws IllegalStateException if the current thread did not call {@link
     * #isRequired()}.
     */
    public void complete () {
        if (lock.isHeldByCurrentThread()) {
            initialized = true;

            /* If it's not synchronized, then it's a non-discriminating run-once
             * and thus we never release the lock since that's our test.  
             *
             * If it is synchronized, our test is actually the boolean and if that
             * check fails true then we have to wait if anything else ran the test,
             * and let it go if they did.
             */
            if (synchronize) {
                lock.unlock();
            }
        }
        else {
            throw new IllegalStateException(
                "Calling thread must hold lock for AtomicInitializer.complete()"
                );
        }
    }

    /**
     * Clear this initializer.
     *
     * <p>Clear this initializer, causing the next call to {@link
     * #isRequired()} to return <code>true</code>.</p>
     */
    public void clear () {
        lock.lock();

        initialized = false;

        lock.unlock();
    }

    /**
     * Mark initialization as failed, retrying.
     *
     * <p>Release the lock acquired for initialization (synchronized or non)
     * after marking initialization as incomplete.  This will cause the
     * initialization loop to be re-entered at a later point in time.</p>
     *
     * @throws IllegalStateException if the current thread did not call {@link
     * #isRequired()}.
     */
    public void retry () {
        if (lock.isHeldByCurrentThread()) {
            initialized = false;

            lock.unlock();
        }
        else {
            throw new IllegalStateException(
                "Calling thread must hold lock for AtomicInitializer.retry()"
                );
        }
    }
}
