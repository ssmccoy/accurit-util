package it.accur.util;

import org.testng.annotations.Test;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

@Test(groups={"unit"}, testName="Strings API Test")
public class StringsTest {

    private void runIsNumberTest (final String string,
                                  final boolean expectation,
                                  final String errmsg)
    {
        assert Strings.isNumber(string) == expectation : errmsg;
    }

    public void testIsNumber () {
        assert Strings.isNumber("0")          : "Expected \"0\" to be a number";
        assert Strings.isNumber("10")         : "Expected \"10\" to be a number";
        assert Strings.isNumber("10.10")      : "Expected \"10.10\" to be a number";
        assert Strings.isNumber("1234567890") : "Expected \"1234567890\" to be a number";
        assert Strings.isNumber("-10")        : "Expected \"-10\" to be a number";
        assert Strings.isNumber("-10.10")     : "Expected \"-10.10\" to be a number";
        assert Strings.isNumber(".95")        : "Expected \".95\" is a number";

        assert !Strings.isNumber("1,010.10")  : "Expected \"1,010.10\" is not a number";
        assert !Strings.isNumber("10.10.10")  : "Expected \"10.10.10\" is not a number";
        assert !Strings.isNumber("$1.10")     : "Expected \"$1.10\" is not a number";
    }

    public void testEmpty () {
        assert Strings.isEmpty("")   : "Expected that \"\" is considered empty";
        assert Strings.isEmpty(null) : "Expected that null is considered empty";
    }

    public void testAsList () {
        List <String> three = Strings.asList("1,   2, 3");

        assert three.contains("1") : "Expected to contain string \"1\"";
        assert three.equals(new ArrayList(Arrays.asList("1", "2", "3"))) :
            "Expected input \"1, 2, 3\" to translate to list {1,2,3}";
    }

    public void testAsSet () {
        String input = "3,2,1,2,3";

        Set <String> three = Strings.asSet(input);

        assert three.size() == 3 :
            "Expected set from \"" + input + "\" to contain 3 values";

        assert three.contains("1") : input + ": expected to contain \"1\"";
        assert three.contains("2") : input + ": expected to contain \"2\"";
        assert three.contains("3") : input + ": expected to contain \"3\"";
    }
}
