package it.accur.util.monitoring;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.Map.Entry;
import javax.management.MBeanInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanAttributeInfo;
import javax.management.AttributeNotFoundException;
import javax.management.AttributeList;
import javax.management.Attribute;
import javax.management.ReflectionException;
import javax.management.DynamicMBean;
import it.accur.util.monitoring.LatencyMonitor;
import it.accur.util.monitoring.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A latency monitoring proxy object factory.
 *
 * <p>This class provides a factory for proxies which keep {@link
 * LatencyMonitor} objects for each method invoked on the proxy.  The object
 * itself is a {@link DynamicMBean}, which can be exported via JMX to make
 * visible the measured latency of each method this proxy-factory has generated
 * proxy classes for.  It measures method latency as the time from invocation
 * to completion.</p>
 */
public class LatencyMonitorProxyFactory 
implements DynamicMBean {
    private static final String LONG_TYPE    = Long.class.getName();
    private static final String BOOLEAN_TYPE = Boolean.class.getName();
    private static final String STRING_TYPE  = String.class.getName();
    private final Logger log = LoggerFactory.getLogger(
        LatencyMonitorProxyFactory.class );

    static class TimedInvocationHandler <T>
    implements InvocationHandler {

        /**
         * The attribute monitors stored in a hash suitable for attribute
         * lookups (so we do fewer string conversions and avoid deep searches).
         */
        /* TODO: Remove duplicate monitors, we can simply use Method.toString()
         * in the AttributeInfos.
         */
        private final ConcurrentHashMap <String, LatencyMonitor> monitors;

        private Class<?> type;
        private T        target;
        private int      sampleSize;
        private TimeUnit timeUnit;

        TimedInvocationHandler (
            final ConcurrentHashMap <String, LatencyMonitor> monitors,
            final int sampleSize,
            final TimeUnit timeUnit,
            final T target)
        {
            this.monitors   = monitors;
            this.sampleSize = sampleSize;
            this.timeUnit   = timeUnit;
            this.target     = target;
            this.type       = target.getClass();
        }

        /**
         * Time the given method invocation.
         *
         * <p>Vivifies the {@link LatencyMonitor} for the given method, and
         * times the invocation of the given method.</p>
         *
         * @param proxy The target object upon which to time the invocation.
         * @param method The method to time.
         * @param args The arguments to pass to the method.
         * 
         * @return The result of executing the method.
         */
        public Object invoke (Object proxy, Method method, Object... args)
        throws IllegalAccessException, InvocationTargetException,
               IllegalStateException, NoSuchMethodException
        {

            /* We use the abstract class method as the key, because the Method
             * itself belongs to an interface which may be common among
             * numerous implementations, and the goal is to time
             * implementations.  
             *
             * If the method was not found, a NoSuchMethodException is thrown
             * which will be wrapped in a runtime exception by the JVM.
             */

            Method targetMethod = type.getMethod(method.getName(), 
                                                 method.getParameterTypes());

            /* Using the string-version of the method name simplifies
             * traversal for building the MBeanAttributeInfo later on.
             */
            String methodName = targetMethod.toString();

            LatencyMonitor monitor = monitors.get(methodName);

            if (monitor == null) {
                monitor = new LatencyMonitor(sampleSize, timeUnit);

                LatencyMonitor current = monitors.putIfAbsent(methodName,
                                                              monitor);

                if (current != null) {
                    monitor = current;
                }
            }

            Timer timer = monitor.startTimer();

            try {
                return method.invoke(target, args);
            }
            finally {
                timer.stop();
            }
        }
    }

    /** 
     * Shared monitors — this monitor collection is shared amongst all timed
     * invocation handlers.
     */
    private final ConcurrentHashMap <String, LatencyMonitor> monitors =
        new ConcurrentHashMap <String, LatencyMonitor> ();
    private final ConcurrentHashMap <Class, Class> proxies =
        new ConcurrentHashMap <Class, Class> ();

    private final int        sampleSize;
    private final TimeUnit   timeUnit;
    private volatile boolean enabled    = true;

    /**
     * Create a new monitor factory.
     *
     * <p>Given a size for each sample buffer, and a time unit for calculating
     * throughput rates, create a new monitor proxy factory.</p>
     *
     * @param sampleSize The size of buffer to allocate for each monitor.
     * @param timeUnit   The unit of time to use for latency calculations.
     */
    public LatencyMonitorProxyFactory (final int sampleSize,
                                       final TimeUnit timeUnit)
    {
        this.sampleSize = sampleSize;
        this.timeUnit   = timeUnit;
    }

    /**
     * Create a new monitor factory.
     *
     * <p>Given a size for each sample buffer, a time unit for calculating
     * throughput rates, and a flag indicating whether or not this factory
     * should actually do anything — create a new monitor proxy factory.</p>
     *
     * @param sampleSize The size of buffer to allocate for each monitor.
     * @param timeUnit   The unit of time to use for latency calculations.
     * @param enabled    Boolean flag indicating whether or not this factory
     * creates proxies.
     */
    public LatencyMonitorProxyFactory (final int      sampleSize,
                                       final TimeUnit timeUnit,
                                       final boolean  enabled)
    {
        this.sampleSize = sampleSize;
        this.timeUnit   = timeUnit;
        this.enabled    = enabled;
    }

    /**
     * Create a new proxy object.
     *
     * @param type The type to create this proxy as.
     * @param object The object to proxy.
     *
     * @throws IllegalStateException If any of {@link IllegalAccessException},
     * {@link InvocationTargetException}, {@link InstantiationException},
     * {@link NoSuchMethodException} occur.
     */
    public <T> T create (Class <T> type, T object) {
        if (!type.isInterface()) {
            throw new IllegalArgumentException(
                "Type argument of create(type,object) must be interface"
                );
        }

        if (enabled) {
            Class proxyClass = proxies.get(type);

            if (proxyClass == null) {
                /* Occasionally races may generate extra classes, we just discard
                 * them when this occurs.
                 */
                proxyClass = Proxy.getProxyClass(type.getClassLoader(), 
                                                 new Class[] { type });

                Class previous = proxies.putIfAbsent(type, proxyClass);

                if (previous != null) {
                    proxyClass = previous;
                }
            }

            try {
                Constructor <T> constructor =
                    proxyClass.getConstructor(InvocationHandler.class);

                return constructor.newInstance(
                    new TimedInvocationHandler(monitors, sampleSize,
                                               timeUnit, object)
                    );
            }
            catch (InvocationTargetException exception) {
                String message = "Unexpected exceptionin proxy constructor";

                log.error(message, exception);
                throw new IllegalStateException(message, exception);
            }
            catch (IllegalAccessException exception) {
                String message = "Unexpected error accessing proxy constructor";

                log.error(message, exception);
                throw new IllegalStateException(message, exception);
            }
            catch (InstantiationException exception) {
                String message = "Unexpected error thrown while building proxy";

                log.error(message, exception);
                throw new IllegalStateException(message, exception);
            }
            catch (NoSuchMethodException exception) {
                String message = "Unexpected missing constructor in proxy";

                log.error(message, exception);
                throw new IllegalStateException(message, exception);
            }
        }
        else {
            return object;
        }
    }

    public MBeanInfo getMBeanInfo () {
        MBeanAttributeInfo[] attrs = new MBeanAttributeInfo [monitors.size()+2];

        int i = 0;

        for (Entry <String, LatencyMonitor> entry : monitors.entrySet()) {
            attrs[i++] = new MBeanAttributeInfo(
                entry.getKey(),
                LONG_TYPE,
                "Average time to service",
                true, false, false
                );
        }

        attrs[i++] = new MBeanAttributeInfo(
            "enabled",
            BOOLEAN_TYPE,
            "Flag indicating whether or not this monitor is actaully enabled",
            true, true, false
            );

        attrs[i++] = new MBeanAttributeInfo(
            "time unit",
            STRING_TYPE,
            "The time unit being used for calculation",
            true, false, false
            );

        return new MBeanInfo(LatencyMonitorProxyFactory.class.getName(),
                             "Method invocation timer",
                             attrs,
                             new MBeanConstructorInfo[]{},
                             new MBeanOperationInfo[]{},
                             new MBeanNotificationInfo[]{});
    }

    public Object getAttribute (final String attribute) {
        if ("enabled".equals(attribute)) {
            return enabled;
        }
        else if ("time unit".equals(attribute)) {
            return timeUnit.toString();
        }
        else {
            LatencyMonitor monitor = monitors.get(attribute);

            if (monitor != null) {
                return monitor.getAverageDuration();
            }
            else {
                return null;
            }
        }
    }

    public AttributeList getAttributes (final String[] attributes) {
        AttributeList list = new AttributeList(attributes.length);

        for (String attribute : attributes) {
            list.add(new Attribute(attribute, getAttribute(attribute)));
        }

        return list;
    }

    public Object invoke (String action, Object[] params, String[] signature)
    throws ReflectionException {
        throw new ReflectionException(new NoSuchMethodException(
                action + " does not exist"));
    }

    public AttributeList setAttributes (final AttributeList list) {
        return new AttributeList(0);
    }

    public void setAttribute (final Attribute attribute) {
        if ("enabled".equals(attribute.getName())) {
            Object value = attribute.getValue();

            if (!(value instanceof Boolean)) {
                throw new IllegalArgumentException(
                    "Expected \"enabled\" to be a boolean value"
                    );
            }

            enabled = (Boolean) value;
        }
    }
}
