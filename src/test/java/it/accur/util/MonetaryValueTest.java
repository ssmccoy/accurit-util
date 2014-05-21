package it.accur.util;

import java.util.Locale;
import java.util.Currency;
import java.math.BigDecimal;
import org.testng.annotations.Test;

@Test(groups={"unit"}, testName="Prescriptive Tests for MonetaryValue")
public class MonetaryValueTest {
    /**
     * Test addition and String Formatting.
     */
    public void testAdd () {
        MonetaryValue value = new MonetaryValue( Locale.US, BigDecimal.ONE,
                                                 Currency.getInstance("USD"));

        MonetaryValue result = value.add(value);

        assert "$2.00".equals(result.toString()) :
            "Expected $1.00 + $1.00 = $2.00, not " + result;
    }

    /**
     * Test subtraction and String Formatting.
     */
    public void testSubtract () {
        MonetaryValue value = new MonetaryValue( Locale.US, BigDecimal.ONE,
                                                 Currency.getInstance("USD"));

        MonetaryValue result = value.subtract(value);

        assert "$0.00".equals(result.toString()) :
            "Expected $1.00 - $1.00 = $0.00, not " + result;
    }

    /**
     * Test modulous and BigDecimal conversion.
     */
    public void testRemainder () {
        MonetaryValue value = 
            MonetaryValue.valueOf(Locale.US, BigDecimal.valueOf(3L),
                                  Currency.getInstance("USD"));

        MonetaryValue remainder = value.remainder(
            BigDecimal.valueOf(2L)
            );

        assert remainder.asBigDecimal().longValue() == 1L :
            "Expected $3.00 % $2.00 = $1.00, not " + remainder;
    }

    /**
     * Test multiplication and String Formatting.
     */
    public void testMultiply () {
        MonetaryValue value = 
            MonetaryValue.valueOf(Locale.US, BigDecimal.valueOf(3L),
                                  Currency.getInstance("USD"));

        MonetaryValue result = value.multiply(BigDecimal.ONE);

        assert "$3.00".equals(result.toString()) :
            "Expected $3.00 × $1.00 = $3.00, not " + result;
    }

    /**
     * Verify hashcode distribution is "adequate".
     */
    public void testHashCodeDistribution () {
        int[] hits = new int [ 1024 ];

        for (int i = 0; i < (hits.length / 10); i++) {
            for (int j = 0; j < 99; j++) {
                MonetaryValue value = new MonetaryValue(
                    Locale.US, String.format("%d.%02d", i, j), 
                    "USD"
                    );

                hits[ value.hashCode() % hits.length ]++;
            }
        }

        for (int count : hits) {
            assert count < (hits.length / 10) : 
                "Hash table distribution is inadequate, " +
                "one bucket hit more than " + count + 
                " times out of " + hits.length + 
                " values (expecting no more than " + (hits.length / 10);
        }
    }

    /**
     * Test basic continual arithemtic.
     */
    public void testArithemticBasic () {
        MonetaryValue total = new MonetaryValue("0.20").subtract(
                new MonetaryValue("0.05")).subtract(
                    new MonetaryValue("0.15"));

        assert total.equals(new MonetaryValue("0.00"));
    }
}
