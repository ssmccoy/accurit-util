package it.accur.util;

import java.util.Currency;
import java.util.Locale;
import java.text.NumberFormat;
import java.math.MathContext;
import java.math.RoundingMode;
import java.math.BigDecimal;

/**
 * An appropriate type for money.
 *
 * <p>This class provides an appropriate type for representing monetary sums in
 * various currencies, and formatting them in a localization-aware scheme.
 * It implements the majority of the functionality of {@link BigDecimal} but
 * enforces appropriate scale for the given currency and responsible rounding
 * behavior in accordinace with that of the IEEE 754R specification for decimal
 * values.  It uses a <em>word-size</em> binary representation of the
 * numberical value capable of holding up to and no more than 16 total digits,
 * or $99,999,999,999,999.99.  As a result of this limitation this type should
 * not be used for performing debt amortization calculations on the gross
 * deficit of the united states government.</p>
 *
 * <p>Operations defined in this type are generally implemented by {@link
 * BigDecimal}.  This type however differs from {@link BigDecimal} in that the
 * precision is fixed to the assumed word-size of 64 bits, and that all
 * operations ensure consistency of currency and a currency-appropriate scale.
 * This type is not well suited for values of scale higher than the currency
 * appropriate scale (such as fractional cents often used in the US stock
 * market).</p>
 *
 * @see BigDecimal
 * @see Locale
 * @see Currency
 */
public class MonetaryValue 
implements Comparable <MonetaryValue> {
    public static final Locale   DEFAULT_LOCALE   = Locale.getDefault();
    public static final Currency DEFAULT_CURRENCY =
        Currency.getInstance(DEFAULT_LOCALE);

    public static final MonetaryValue ZERO_US_DOLLARS =
        new MonetaryValue(DEFAULT_LOCALE, "0", "USD");

    private static final MathContext MC = MathContext.DECIMAL64;

    private Locale     locale   = DEFAULT_LOCALE;
    private Currency   currency;
    private BigDecimal value;

    /**
     * @param value The monetary figure as a string.
     */
    public MonetaryValue (final String value) {
        this.currency = DEFAULT_CURRENCY;
        this.value    = new BigDecimal(value, MC).setScale(
            this.currency.getDefaultFractionDigits()
            );
    }

    /**
     * @param value The monetary figure.
     */
    public MonetaryValue (final BigDecimal value) {
        this.currency = DEFAULT_CURRENCY;
        this.value    = value.setScale(
            this.currency.getDefaultFractionDigits()
            );
    }

    /**
     * @param value The monetary figure as a BigDecimal.
     * @param currency The currency this value represents.
     */
    public MonetaryValue (final BigDecimal value, final Currency currency) {
        if (currency == null) {
            throw new IllegalArgumentException(
                "May not create MonetaryValue without a currency"
                );
        }

        this.currency = currency;
        this.value    = value.setScale(
            this.currency.getDefaultFractionDigits()
            );
    }

    /**
     * @param locale The locale to format this currency for.
     * @param value The monetary value.
     * @param currency The currency this value represents.
     *
     * @throws IllegalArgumentException If the provided locale is null or
     * invalid.
     * @throws IllegalArgumentException If the provided currency is null or
     * invalid.
     */
    public MonetaryValue (final Locale locale,
                          final BigDecimal value, 
                          final Currency currency) {
        if (locale == null) {
            throw new IllegalArgumentException(
                "May not create MonetaryValue without a locale"
                );
        }
        if (currency == null) {
            throw new IllegalArgumentException(
                "May not create MonetaryValue without a currency"
                );
        }

        this.locale   = locale;
        this.currency = currency;
        this.value    = value.setScale(
            this.currency.getDefaultFractionDigits()
            );
    }

    /**
     * @param value The monetary value.
     * @param currency The currency this value represents.
     *
     * @throws NullPointerException If the provided currency is null.
     * @throws IllegalArgumentException If the provided currency points to a
     * currency which does not exist.
     */
    public MonetaryValue (final String value, final String currency) {
        this.value    = new BigDecimal(value, MC).setScale(2);
        this.currency = Currency.getInstance(currency);

        if (currency == null) {
            throw new IllegalArgumentException(
                "May not create MonetaryValue without a currency"
                );
        }
    }

    /**
     * @param locale The locale to format this currency for.
     * @param value The monetary value.
     * @param currency The currency this value represents.
     */
    public MonetaryValue (final Locale locale, 
                          final String value, 
                          final String currency) 
    {
        this.locale   = locale;
        this.currency = Currency.getInstance(currency);

        /* Scale the currency to the appropriate value for this currency. */
        this.value    = new BigDecimal(value).setScale(
            this.currency.getDefaultFractionDigits()
            );
    }

    /**
     * Determine if the given monetary value is of a compatible currency.
     *
     * <p>Currently, no currency conversion is supported.  This method
     * determines if the given monetary value is of the same currency as this
     * value.</p>
     *
     * @return <code>true</code> if the given value is of the same currency as
     * this one, <code>false</code> otherwise.
     */
    public boolean isCompatibleCurrency (final MonetaryValue value) {
        return this.currency == value.currency;
    }

    /**
     * Validate the given amount is compatible with this value's currency.
     *
     * @param amount The monetary value to validate.
     *
     * @throws IllegalArgumentException If the given amount isn't of a
     * compatible currency.
     */
    private void validateCurrency (final MonetaryValue amount) 
    throws IllegalArgumentException {
        if (!isCompatibleCurrency(amount)) {
            throw new IllegalArgumentException(String.format(
                    "Cannot add values of currency %s and %s",
                    this.currency, amount.currency));
        }
    }

    /**
     * Generate an appropriate MonetaryValue result from a BigDecimal result.
     */
    private MonetaryValue result (final BigDecimal amount) {
        return new MonetaryValue(amount, currency);
    }

    /**
     * Creates a new MonetaryValue who's value is the sum of this value and the
     * supplied augend.
     *
     * @param augend Value to be added to this MonetaryValue.
     * @return this + augend.
     */
    public MonetaryValue add (final BigDecimal augend) {
        return result(value.add(augend, MC));
    }

    /**
     * Creates a new MonetaryValue who's value is the sum of this value and the
     * supplied augend.
     *
     * @param augend Value to be added to this MonetaryValue.
     * @return this + augend.
     * @throws IllegalArgumentException If the given MonetaryValue is not
     * currency compatible with this value.
     */
    public MonetaryValue add (final MonetaryValue augend) {
        validateCurrency(augend);

        return add(augend.value);
    }

    /**
     * Creates a new MonetaryValue who's value is the difference between this
     * value and the supplied subtrahend.
     *
     * @param augend Value to be added to this MonetaryValue.
     * @return this + subtrahend.
     */
    public MonetaryValue subtract (final BigDecimal subtrahend) {
        return result(value.subtract(subtrahend, MC));
    }

    /**
     * Creates a new MonetaryValue who's value is the difference between this
     * value and the supplied subtrahend.
     *
     * @param augend Value to be added to this MonetaryValue.
     * @return this + subtrahend.
     * @throws IllegalArgumentException If the given MonetaryValue is not
     * currency compatible with this value.
     */
    public MonetaryValue subtract (final MonetaryValue subtrahend) {
        validateCurrency(subtrahend);

        return subtract(subtrahend.value);
    }

    /**
     * Creates a new MonetaryValue who's value the division of this value by
     * the given divisor.
     *
     * @param divisor value by which this value is to be divided.
     *
     * @return this / divisor.
     *
     * @throws ArithmeticException if divisor is zero.
     */
    public MonetaryValue divide (final BigDecimal divisor) {
        return result(value.divide(divisor, MC));
    }

    /**
     * Creates a new MonetaryValue who's value the division of this value by
     * the given divisor.
     *
     * @param divisor value by which this value is to be divided.
     *
     * @return this / divisor.
     * @throws ArithmeticException if divisor is zero.
     * @throws IllegalArgumentException If the given MonetaryValue is not
     * currency compatible with this value.
     */
    public MonetaryValue divide (final MonetaryValue divisor) {
        validateCurrency(divisor);

        return divide(divisor.value);
    }

    /**
     * Returns a MonetaryValue whose value is (this × multiplicand).
     *
     * @param multiplicand The value to be multiplied by this MonetaryValue.
     */
    public MonetaryValue multiply (final BigDecimal multiplicand) {
        return result( value.multiply(multiplicand, MC) );
    }

    /**
     * Returns a MonetaryValue whose value is (this × multiplicand).
     *
     * @param multiplicand The value to be multiplied by this MonetaryValue.
     * @throws IllegalArgumentException If the given MonetaryValue is not
     * currency compatible with this value.
     */
    public MonetaryValue multiply (final MonetaryValue multiplicand) {
        validateCurrency(multiplicand);

        return multiply(multiplicand.value);
    }

    /**
     * Return the absolute value of this value.
     * @return The absolute value of this value.
     */
    public MonetaryValue abs () {
        return result( value.abs(MC) );
    }

    /**
     * Return this value as a negative value (unary -).
     * @return A negative value which where
     * <code>this.abs().equals(result.abs())</code> evaluates as
     * <code>true</code>.
     */
    public MonetaryValue negate () {
        return result( value.negate(MC) );
    }

    /**
     * Equvilent to unary plus (nonoperation).
     *
     * @return this value.
     */
    public MonetaryValue plus () {
        return this;
    }

    /**
     * Return the exponentation of this value and the given exponent.
     *
     * @param exponent The exponent.
     */
    public MonetaryValue pow (int exponent) {
        return result( value.pow(exponent, MC) );
    }

    /**
     * Return the modulous of this value and a divisor as an appropriate
     * monetary value.
     *
     * @param divisor The divisor.
     * @return The remainder after dividing this value by the given divisor.
     * @throws ArithmeticException if divisor is zero.
     */
    public MonetaryValue remainder (BigDecimal divisor) {
        return result( value.remainder(divisor, MC) );
    }

    /**
     * Return the modulous of this value and a divisor as an appropriate
     * monetary value.
     *
     * @param divisor The divisor.
     * @return The remainder after dividing this value by the given divisor.
     * @throws ArithmeticException if divisor is zero.
     */
    public MonetaryValue remainder (MonetaryValue divisor) {
        validateCurrency(divisor);

        return result( value.remainder(divisor.value, MC) );
    }

    /**
     * Create a hash code of this value.
     *
     * <p>Hash algorithm piggy backs on integral hashing algorithm provided by
     * {@link BigDecimal}, and factors the currency code into the left 4 most
     * bits.</p>
     */
    public int hashCode () {
        return (value.hashCode() | 
                ((currency.getCurrencyCode().hashCode() << 4) & 0xFFFF0000));
    }

    /**
     * Return the currency code for this currency as a string.po
     *
     * <p>This method simply delegates to {@link
     * Currency.getCurrencyCode()}.</p>
     *
     * @return A currency code, never null.
     * @see Currency
     */
    public String getCurrencyCode () {
        return currency.getCurrencyCode();
    }

    /**
     * Compare this amount to the given amount.
     *
     * <p>Delegates to {@link BigDecimal.compareTo(BigDecimal)}.  This method
     * is provided in preference to individual methods for the standard
     * numerical comparison operators <code>(&lt;, &gt;, * &gt;=, &lt;=).</p>
     */
    public int compareTo (MonetaryValue amount) {
        validateCurrency(amount);
        return value.compareTo(amount.value);
    }

    /**
     * Determine if this amount is equal to a given amount.
     *
     * <p>Given another object of the same type, this method will determine if
     * that object is equal to this one in both currency (or compatible
     * currency) and value, ignoring localization settings.</p>
     *
     * @param object The object to compare to.
     *
     * @return <code>true</code> if they are equal, <code>false</code>
     * otherwise.
     */
    public boolean equals (Object object) {
        if (object instanceof MonetaryValue) {
            MonetaryValue amount = (MonetaryValue) object;

            return isCompatibleCurrency(amount) && value.equals(amount.value);
        }
        else {
            return false;
        }
    }

    /**
     * Return the greatest of two given amounts.
     *
     * <p>Given two monetary values, determine which one is of greatest value
     * and return it.</p>
     *
     * @param a Amount one.
     * @param b Amount two.
     */
    public static MonetaryValue max (MonetaryValue a, MonetaryValue b) {
        a.validateCurrency(b);

        return a.value.compareTo(b.value) > 0 ? a : b;
    }

    /**
     * Return the least of two given amounts.
     *
     * <p>Given two monetary values, determine which one is of least value
     * and return it.</p>
     *
     * @param a Amount one.
     * @param b Amount two.
     */
    public static MonetaryValue min (MonetaryValue a, MonetaryValue b) {
        a.validateCurrency(b);

        return a.value.compareTo(b.value) < 0 ? a : b;
    }

    /**
     * Return the big decimal representation of this value.
     *
     * @return This value as a BigDecimal.
     */
    public BigDecimal asBigDecimal () {
        return value;
    }

    /**
     * Return a properly formatted money-string.
     *
     * <p>Returns a formatted money-string with the localized symbol for the
     * currency this value represents as the prefix, and the value formatted
     * with an appropriate localized number format.</p>
     *
     * @return A string representation of this value in this currency,
     * appropriate for this locale.
     */
    public String toString () {
        NumberFormat format = NumberFormat.getCurrencyInstance(locale);

        format.setCurrency(currency);

        return format.format(value);
    }


    /**
     * Create a MonetaryValue representation of the given value in the
     * specified currency.
     *
     * @return A new monetary value.
     * @param locale The localization settings for representation.
     * @param value The value.
     * @param currency The currency.
     */
    public static MonetaryValue valueOf (Locale locale, 
                                         BigDecimal value, 
                                         Currency currency) 
    {
        return new MonetaryValue(locale, value, currency);
    }

    /**
     * Create a MonetaryValue representation of the given value in the
     * specified currency.
     * @return A new monetary value.
     * @param value The value.
     * @param currency The currency.
     */
    public static MonetaryValue valueOf (BigDecimal value, Currency currency) {
        return new MonetaryValue(value, currency);
    }

    /**
     * Create a MonetaryValue representation of the given value in the
     * the default currency.
     *
     * @return A new monetary value.
     * @param value The value.
     */
    public static MonetaryValue valueOf (BigDecimal value) {
        return valueOf(value, DEFAULT_CURRENCY);
    }

    /**
     * Create a MonetaryValue representation of the given value in the
     * specified currency.
     * @return A new monetary value.
     * @param value The value.
     * @param currency The currency.
     */
    public static MonetaryValue valueOf (double value, Currency currency) {
        return valueOf(BigDecimal.valueOf(value), currency);
    }
}
