package it.accur.util;

import org.testng.annotations.Test;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

@Test(groups={"unit"},testName = "Unit tests for it.accur.util.Files")
public class FilesTest {
    /**
     * Simple prescriptive test for Files.slurp.
     */
    public void testSlurp () 
    throws IOException {
        String contents = Files.slurp("src/test/resources/data/slurp.txt");

        assert "slurp test\n".equals(contents) :
            "Expected file to contain contents \"slurp test\"";
    }

    private void verifyChecksum (File ... files) 
    throws IOException, NoSuchAlgorithmException {
        byte[] checksum = null;

        for (File file : files) {
            InputStream stream = new FileInputStream(file);

            MessageDigest   digest = MessageDigest.getInstance("MD5");

            byte[] buf = new byte[1024];
            int r;

            while ((r = stream.read(buf)) != -1) {
                digest.update(buf, 0, r);
            }

            if (checksum == null) {
                checksum = digest.digest();
            }
            else {
                assert Arrays.equals(checksum, digest.digest()) :
                    "Digest checksums do not match!";
            }
        }
    }

    /**
     * Verify binary copies work properly.
     *
     * <p>This test exercises a bug found in the field where binary copies did
     * not work properly.  This test must pass to ensure the bug does not
     * regress.</p>
     */
    public void testCopy () 
    throws IOException, NoSuchAlgorithmException {
        File file = new File("src/test/resources/data/1269759740713.log");
        File tmp  = File.createTempFile("test", ".log");

        Files.copy(file, tmp);

        verifyChecksum(file, tmp);
    }
}
