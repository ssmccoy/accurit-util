package it.accur.util;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A local event broker.
 *
 * <p>This object provides a prototypable local publish/subscribe event broker.
 * The broker operates simply, encapsulating registration of subscribers to
 * a given topic and generating publisher objects which federate messages to
 * those subscribers.  The <em>pub/sub</em> model is mapped to plain java
 * objects by treating interfaces as <em>topics</em> and method invocations as
 * <em>messages</em>.  This API does not provide a mechanism for interprocess
 * communication in any way, and does not implement the publish/subscribe model
 * through asynchronous messaging.  Additionally, message consumption is
 * <em>not</em> asynchronous unless such functionality is provided by the
 * subscriber itself.</p>
 * 
 * <p>This utility is intended to provide automation when building domain event
 * models.  Event distribution is handled by dynamically generated objects
 * which reduces the amount of boilerplate code required for adopting this
 * paradigm.  Brokers should be encapsulated within the message producer
 * objects themselves.</p>
 *
 * <h4>Adding Event Listeners at Runtime</h4>
 * <pre>
 * EventBroker broker = object.getBroker();
 *
 * broker.subscribe(RegisteredCustomerListener.class, emailListener);
 * </pre>
 *
 * <h4>Example Prototype Event Model with Spring &amp; AspectJ</h4>
 * <pre>
 * @Configurable
 * public class Customer {
 *    private EventBroker broker;
 *
 *    public void setBroker (final EventBroker broker) 
 *    {
 *      this.broker = broker;
 *    }
 *
 *    public EventBroker getBroker () {
 *      return broker;
 *    }
 *
 *    public void register () {
 *      registeredListener.registeredCustomer(this);
 *    }
 *
 *    public void resetPassword () {
 *      // reset password business logic...
 *      PasswordChangedListener listener =
 *	    broker.getPublisher(PasswordChangedListener.class);
 *
 *      passwordListener.changedPassword(this, newpassword);
 *    }
 *
 *    public boolean changePassword (final String credental, 
 *                                   final String password) 
 *    {
 *      boolean change = this.password.equals(encrypt(credental));
 *
 *      if (change) {
 *          this.password = password;
 *
 *          PasswordChangedListener listeners =
 *              broker.getPublisher(
 *                  PasswordChangedListener.class);
 *
 *          listeners.changedPassword(this, password);
 *      }
 *
 *      return change;
 *    }
 *
 *    public void lazyLookup () {
 *      LazyEventListener listener = broker.getPublisher(
 *          LazyEventListener.class);
 *
 *      listener.lazyEvent(this);
 *    }
 * }
 * </pre>
 *
 * <h5>Spring Configuration</h5>
 * <pre>
 * &lt;bean id="email-customer" 
 *    class="com.business.event.notification.SendMessage"/&gt;
 *
 * &lt;bean class="com.business.model.Customer" scope="prototype"&gt;
 *  &lt;property name="broker"&gt;
 *    &lt;map&gt;
 *      &lt;entry key="com.business.model.event.PasswordChangeListener"&gt;
 *        &lt;list&gt;
 *          &lt;bean class="com.business.event.storage.UpdatePassword"&gt;
 *            &lt;constructor-arg ref="crm-readwrite-strategy"/&gt;
 *          &lt;/bean&gt;
 *          &lt;bean ref="email-customer"/&gt;
 *        &lt;/list&gt;
 *      &lt;/entry&gt;
 *      &lt;entry key="com.business.model.event.RegistrationListener"&gt;
 *        &lt;list&gt;
 *          &lt;bean ref="email-customer"/&gt;
 *        &lt;/list&gt;
 *      &lt;/entry&gt;
 *    &lt;/map&gt;
 *  &lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 * 
 * <h4>Prototype Event Model Without DI</h4>
 * <pre>
 * class PrototypedObject {
 *   private static final EventBroker prototype = new EventBroker();
 *   private final EventBroker broker = prototype.clone();
 *
 *   public EventBroker getBroker () {
 *     return broker;
 *   }
 *
 *   public static EventBroker getPrototype () {
 *     return prototype;
 *   }
 *
 *   public void example () {
 *     ExampleListener listener = broker.getPublisher(ExampleListener.class);
 *     listener.doExample(this);
 *   }
 *
 *   public static void main (String[] args) {
 *     EventBroker prototype = PrototypedObject.getPrototype();
 *     prototype.subscribe(ExampleListener.class, exampleListener);
 *     
 *     PrototypedObject object = new PrototypedObject();
 *
 *     object.example();
 *   }
 * }
 * </pre>
 */
public class EventBroker {
    /**
     * An invocation handler for event broker proxies.
     *
     * <p>This class implements the {@link InvocationHandler} interface and can be
     * used to proxy a given event handler interface.  It will federate all method
     * invocations to the proxy implementation of the interface, causing
     * multi-dispatch for all registered listeners of the given interface of the
     * method(s) invoked on the proxy instance.</p>
     *
     * <p>The dispatch may be <em>canceled</em> if the return type of the given
     * method is {@link Boolean} and one of the registered handlers returns
     * <code>false</code>.  Cancelation will be indicated by a return value of
     * <code>false</code> respectively, otherwise the return value of the last handler in the
     * chain is returned, when available.</p>
     *
     * @see EventBroker
     */
    static class EventBrokerProxy <T>
    implements InvocationHandler, Serializable {
        private List <T> handlers;

        /**
         * Create a new event broker proxy with no listeners.
         */
        public EventBrokerProxy () {
            this.handlers = Collections.EMPTY_LIST;
        }

        /**
         * Create a new event broker proxy with a list of handlers.
         *
         * @param handlers The list of handlers to federate method invocations to.
         */
        public EventBrokerProxy (final List <T> handlers) {
            this.handlers = handlers;
        }

        /**
         * Create a new proxy handler with the same list of listeners.
         *
         * <p>This is a shallow clone, which will share the same list instance
         * with it's parent.</p>
         */
        public EventBrokerProxy <T> clone () {
            return new EventBrokerProxy <T> (handlers);
        }

        /**
         * Federate a method invocation.
         *
         * <p>Given a method called on one of our proxies, invoke the same
         * method on the internal list of handlers for this interface.
         * Invocation is stopped prematurely if one if the method returns a
         * boolean value and one of the invocations in the list returned false.
         * Otherwise, the value returned from the last event handler in the
         * list is returned, if any.</p>
         *
         * @param proxy The generated proxy object.
         * @param method The method to invoke.
         * @param args The list of arguments for the invocation.
         *
         * @throws Throwable (whatever is thrown internally is passed along).
         *
         * @return The return value of the last successfully executed method
         * invocation, if anything.
         */
        public Object invoke (final Object proxy, final Method method,
                              final Object[] args)
        throws Throwable {
            Object result = null;

            for (T handler : handlers) {
                result = method.invoke(handler, args);

                if (Boolean.FALSE.equals(result)) {
                    return false;
                }
            }

            return result;
        }
    }

    private static Map <Class, Constructor> constructors =
	new ConcurrentHashMap <Class, Constructor> ();

    private Map <Class, List>   handlerMap;
    private Map <Class, Object> listeners = new HashMap <Class, Object> ();

    /**
     * Create a new blank-slate broker.
     *
     * <p>Create a new broker with an empty handler set.</p>
     */
    public EventBroker () {
	this.handlerMap = new HashMap <Class, List> ();
    }

    /**
     * Create a new broker with a specified prototype.
     *
     * <p>Create a new broker with a predefined set of listeners for given
     * event types.</p>
     *
     * @param handlerMap A map of lists of handler interfaces and corresponding
     * handler objects.
     */
    public EventBroker (final Map <Class, List> handlerMap) {
        this.handlerMap = handlerMap;
    }

    /**
     * Create a new event publisher for the given type.
     *
     * <u>Consider using {@link getPublisher(Class)} instead.</u>
     *
     * <p>Given a type, create an event listener bound to this event
     * containers entries for that type.  The event handler list is bound to
     * this container and is autovivified.  The listener generated from this
     * method will federate all methods invoked on it to all listeners
     * registered with this container through the 
     * {@link subscribe(Class,Object)} method.</p>
     *
     * <em>This is not threadsafe, it's assumed that there will not be
     * concurrent access to this method on a single event container
     * instance.</em>
     *
     * @param type The type of event to create a listener proxy for.
     *
     * @return An implementation of the given interface which relays method
     * invocations to all subscribers.
     */
    public <L> L createPublisher (final Class <L> type) {
	/* NOTE 20091102T10:41:17 Well aware of this race condition, really
	 * don't mind it.
	 */
	Constructor <L> constructor = constructors.get(type);

	if (constructor == null) {
	    Class proxy = Proxy.getProxyClass(type.getClassLoader(),
					      new Class [] { type });

	    try {
		constructor = proxy.getConstructor(InvocationHandler.class);

		constructors.put(type, constructor);
	    }
	    catch (NoSuchMethodException exception) {
		throw new IllegalStateException(
		    "Generated proxy class has no appropriate constructor",
		    exception);
	    }
	}

	List <L> handlers = handlerMap.get(type);

	if (handlers == null) {
	    handlerMap.put(type, handlers = new ArrayList <L> ());
	}

	try {
	    return constructor.newInstance(
		new EventBrokerProxy <L> (handlers)
		);
	}
	catch (InvocationTargetException exception) {
	    throw new IllegalStateException(
		"Error while executing constructor of generated proxy class",
		exception);
	}
	catch (IllegalAccessException exception) {
	    throw new IllegalStateException(
		"Generated proxy class cannot execute proxy constructor!",
		exception);
	}
	catch (InstantiationException exception) {
	    throw new IllegalStateException(
		"Generated proxy class cannot execute proxy constructor!",
		exception);
	}
    }

    /**
     * Fetch the appropriate publisher for this type.
     *
     * <p>Autovivies a publisher for the given type.  Keeps a pool of reactors
     * bound to this particular instance.</p>
     *
     * @return An implementation of the subscriber interface which is a
     * publisher.
     */
    public <L> L getPublisher (final Class <L> type) {
	/* NOTE 20091102T10:41:35 This should *not* be a race condition since
	 * in practice, only one event broker should really exist per
	 * thread.
	 */
        L listener = (L) listeners.get(type);

        if (listener == null) {
            listeners.put(type, listener = createPublisher(type));
        }

        return listener;
    }

    /**
     * Add a new listener <em>(at runtime)</em>.
     *
     * <p>Given a type and a listener which matches that type, add the listener
     * for this broker.  If this broker has been cloned from a
     * prototype, either through spring or by explicitly using the {@link
     * #clone()} method, then the given listener will affect only this instance
     * of the event broker.</p>
     *
     * @param type The type of listener to add.
     * @param handler The handler to add.
     */
    public <L> void subscribe (final Class <L> type, L handler) {
        List <L> handlers = handlerMap.get(type);
        
        if (handlers == null) {
            handlers = new ArrayList <L> ();

            handlerMap.put(type, handlers);
        }

        handlers.add(handler);
    }

    /**
     * Remove a given listener.
     *
     * <p>Removes the first occurance of the given listener from the list of
     * event handlers for the supplied type, if present.  If there is no
     * listener present for the given type, the event handler list remains
     * unmodified.</p>
     *
     * @param type The type of event listener to remove.
     * @param handler The handler to remove.
     *
     * @return Returns <code>true</code> if the given event handler was
     * successfully removed, <code>false</code> otherwise.
     */
    public <L> boolean cancel (final Class <L> type, L handler) {
        List <L> handlers = handlerMap.get(type);

        if (handlers != null) {
            return handlers.remove(handler);
        }

        return false;
    }

    /**
     * Create a new event broker.
     *
     * <p>Creates a copy of the current event listener.  This copy includes
     * <em>all</em> registered event listeners but does not include any
     * previously returned event actors.</p>
     *
     * @return A new event broker which is keyed in to listen to a superset
     * of this brokers current event listeners.
     */
    public EventBroker clone () {
	EventBroker broker = new EventBroker();

	/* Create a separate array list for each list in our set, copy
	 * the contents of the current array list to this array.  This is used
	 * for model prototypes.
	 */
	for (Entry <Class, List> entry : handlerMap.entrySet()) {
	    List handlers = new ArrayList ();
	    
	    handlers.addAll(entry.getValue());

	    broker.handlerMap.put(entry.getKey(), handlers);
	}

	return broker;
    }
}
