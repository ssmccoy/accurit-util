package it.accur.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

@Test(groups={"unit"}, testName="EventBroker Prescriptive API Test")
public class EventBrokerTest {
    /**
     * Test interface.
     */
    static interface MessageSentListener {
        public boolean sentMessage (String message);
    }

    /**
     * Test data collector.
     */
    public class MessageLister implements MessageSentListener {
        private List <String> messages;

        public MessageLister (List <String> messages) {
            this.messages = messages;
        }

        public boolean sentMessage (String message) {
            return messages.add(message);
        }
    }

    /**
     * Test conditional canceler.
     */
    public class HelloFilter implements MessageSentListener {
        public boolean sentMessage (String message) {
            return !"hello".equals(message);
        }
    }

    /**
     * Test canceler.
     */
    public class CancelingMessenger implements MessageSentListener {
        public boolean sentMessage (String message) {
            return false;
        }
    }

    /**
     * Test domain object.
     */
    static class Messenger {
        private static final EventBroker prototype = new EventBroker();

        public static EventBroker getPrototype () {
            return prototype;
        }

        private final EventBroker dispatcher = prototype.clone();

        public EventBroker getBroker () {
            return dispatcher;
        }

        public boolean sendMessage (String message) {
            MessageSentListener listener = dispatcher.getPublisher(
                MessageSentListener.class);

            return listener.sentMessage(message);
        }
    }

    /**
     * Test prototyping.
     *
     * <P>Add an event listener to the prototype and ensure it's working by
     * guaranteeing the result which can only happen if it's present.</p>
     *
     * <p>Exercises event cancellation, prototype containers, and simple
     * dispatching.</p>
     */
    @Test(groups={"unit"})
    public void testPrototypeEvent () {
        List <String> messages = new ArrayList <String> ();

        EventBroker prototype = Messenger.getPrototype();

        MessageSentListener filter = new HelloFilter();

        prototype.subscribe(MessageSentListener.class, filter);

        Messenger messenger = new Messenger();

        EventBroker broker = messenger.getBroker();

        broker.subscribe(MessageSentListener.class, 
                       new MessageLister(messages));

        assert prototype.cancel(MessageSentListener.class, filter) : 
            "Filter unexpectedly not removed";

        assert messenger.sendMessage("foo") : "Event was unexpectedly canceled";

        boolean canceled = !messenger.sendMessage("hello");

        assert canceled : "Cancelable event was not canceled";

        assert messenger.sendMessage("bar") : "Event was unexpectedly canceled";

        assert messages.equals(Arrays.asList("foo", "bar")) :
            "Exected a list of \"foo\", \"bar\", observed: " + messages;
    }
}
