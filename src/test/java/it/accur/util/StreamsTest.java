package it.accur.util;
import org.testng.annotations.Test;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.Random;
import java.util.Arrays;

@Test(groups={"unit"}, testName = "Streams utility unit test")
public class StreamsTest {
    private void assertSame (byte[] testResult, byte[] testData) {
        assert testData.length == testResult.length :
            "Expected Streams.readBytes to return array of input size";

        assert Arrays.equals(testData, testResult) :
            "Expected Streams.readBytes return array equivilent to input";
    }

    public void test () 
    throws IOException {
        Random random = new Random(System.nanoTime());

        int size = 96212;
        byte[] testData = new byte [size];

        for (int i = 0; i < size; i++) {
            testData[i] = (byte) random.nextInt(256);
        }

        InputStream input = new ByteArrayInputStream(testData);

        byte[] testResult = Streams.readBytes(input);

        assertSame(testResult, testData);

        /* Reset the buffer */
        for (int i = 0; i < testResult.length; i++) {
            testResult[i] = 0x00;
        }

        /* Reset the stream */
        input.reset();

        /* Refresh the stream by using readBytes again */
        Streams.readBytes(testResult, input);

        assertSame(testResult, testData);
    }
}
