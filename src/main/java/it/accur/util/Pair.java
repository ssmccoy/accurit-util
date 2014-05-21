package it.accur.util;

/**
 * A tuple.
 *
 * <p>This class provides a very rudementary two value tuple.</p>
 */
public final class Pair <L, R> {
    private L left;
    private R right;

    /**
     * Private constructor.
     *
     * @param left Left side of the tuple.
     * @param right Right side of the tuple.
     */
    private Pair (final L left, final R right) {
        this.left  = left;
        this.right = right;
    }

    /**
     * The left side of the tuple.
     *
     * @return The left value of the tuple.
     */
    public L left () {
        return left;
    }

    /**
     * The right side of the tuple.
     *
     * @return The right value of the tuple.
     */
    public R right () {
        return right;
    }

    /**
     * Create a new tuple.
     *
     * <p>Create a new tuple.</p>
     *
     * @param left The left side of the tuple.
     * @param right The right side of the tuple.
     *
     * @throws IllegalArgumentException If either of the given values are null.
     */
    public static <L, R> Pair <L, R> create (final L left, final R right)
    throws IllegalArgumentException {
        if (left == null || right == null) {
            throw new IllegalArgumentException(
                "No value of this tuple is allowed to be null"
                );
        }

        return new Pair (left, right);
    }

    /**
     * The hash code of the tuple.
     *
     * <p>The hash code of this tuple is the equivalent of the lower two bytes
     * of each side's hash code.  Given a well distributed hashing function as
     * input for both the left and right sides, this should result in a high
     * entropy hash code representing both the left and right values with a
     * distribution of <code><em>ƒ(d) = ∑(d) / n</em></code>, or the sum of the
     * distribution of all parts of the tuple divided by the number of parts.
     * This is the bitwise eqivalent of <code><em>ƒ(l,r) = (h(l) mod (n/2) +
     * (n/2)) + h(r) mod m/2</em></code>, where mod is formally defined as
     * <code><em>ƒ(a,n) = a - [a ÷ n] × n, n ≠ 0</em></code>.</p>
     *
     * @return a hash code.
     */
    public int hashCode () {
        return (left.hashCode() << 16) ^ (right.hashCode() & 0x0000FFFF);
    }

    /**
     * Determine the equality with another pair.
     *
     * <p>Given another object of this type, the left and right sides are
     * compared.  If the object is not this type, then the result is always
     * false.</p>
     *
     * @return true if the given value is an instance of {@link Pair} and both
     * sides are equal, false otherwise.
     */
    public boolean equals (final Object object) {
        if (!(object instanceof Pair)) {
            return false;
        }

        Pair pair = (Pair) object;

        return left.equals(pair.left) &&
            right.equals(pair.right);
    }
}
