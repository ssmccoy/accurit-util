package it.accur.util;

import java.util.Properties;

import org.testng.annotations.Test;

@Test(groups={"unit"},
  testName="AbstractFactoryFinder positive verification unit test")
public class AbstractFactoryFinderTest {
    public void testObjectFactory () {
        Properties properties = System.getProperties();

        /* Explicitly set the property, don't use service locator... */
        properties.setProperty("it.accur.util.ObjectFactory",
                               "it.accur.util.NullFactory");

        ObjectFactory factory = ObjectFactory.getFactory();

        assert factory != null :
            "Unable to find object factory, expected instance";

        assert factory instanceof NullFactory :
            "Expected factory would be a NullFactory";
    }
}
