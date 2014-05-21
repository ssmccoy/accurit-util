package it.accur.util;
import org.testng.annotations.Test;

@Test(groups={"unit"}, testName="Basic URLs unit test")
public class URLsTest {

    public void testBuildQuery () {
        String expect   = "foo=bar&bar=baz";
        String observed = URLs.buildQuery("foo", "bar", "bar", "baz");

        assert expect.equals(observed) :
            "Expected a \"" + expect + "\", observed \"" + observed;
    }
}
