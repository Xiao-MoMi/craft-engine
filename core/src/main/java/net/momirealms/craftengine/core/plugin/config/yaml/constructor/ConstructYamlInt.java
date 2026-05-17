package net.momirealms.craftengine.core.plugin.config.yaml.constructor;

import org.snakeyaml.engine.v2.constructor.ConstructScalar;
import org.snakeyaml.engine.v2.exceptions.ConstructorException;
import org.snakeyaml.engine.v2.nodes.Node;

import java.math.BigInteger;

public class ConstructYamlInt extends ConstructScalar {
    private static final int[][] RADIX_MAX = new int[17][2];
    static {
        int[] radixList = new int[] {8, 10, 16};
        for (int radix : radixList) {
            RADIX_MAX[radix] =
                    new int[] {maxLen(Integer.MAX_VALUE, radix), maxLen(Long.MAX_VALUE, radix)};
        }
    }

    private static int maxLen(final int max, final int radix) {
        return Integer.toString(max, radix).length();
    }

    private static int maxLen(final long max, final int radix) {
        return Long.toString(max, radix).length();
    }

    @Override
    public Object construct(Node node) {
        String value = constructScalar(node);
        if (value.isEmpty()) {
            throw new ConstructorException("while constructing an int", node.getStartMark(),
                    "found empty value", node.getStartMark());
        }
        return createIntNumber(value);
    }

    public Number createIntNumber(String value) {
        int sign = +1;
        char first = value.charAt(0);
        if (first == '-') {
            sign = -1;
            value = value.substring(1);
        } else if (first == '+') {
            value = value.substring(1);
        }
        int base;
        if ("0".equals(value)) {
            return Integer.valueOf(0);
        } else if (value.startsWith("0x")) {
            value = value.substring(2);
            base = 16;
        } else if (value.startsWith("0o")) {
            value = value.substring(2);
            base = 8;
        } else {
            return createNumber(sign, value, 10);
        }
        return createNumber(sign, value, base);
    }

    private Number createNumber(int sign, String number, int radix) {
        final int len = number != null ? number.length() : 0;
        if (sign < 0) {
            number = "-" + number;
        }
        final int[] maxArr = radix < RADIX_MAX.length ? RADIX_MAX[radix] : null;
        if (maxArr != null) {
            final boolean gtInt = len > maxArr[0];
            if (gtInt) {
                if (len > maxArr[1]) {
                    return new BigInteger(number, radix);
                }
                return createLongOrBigInteger(number, radix);
            }
        }
        Number result;
        try {
            result = Integer.valueOf(number, radix);
        } catch (NumberFormatException e) {
            result = createLongOrBigInteger(number, radix);
        }
        return result;
    }

    protected static Number createLongOrBigInteger(final String number, final int radix) {
        try {
            return Long.valueOf(number, radix);
        } catch (NumberFormatException e1) {
            return new BigInteger(number, radix);
        }
    }
}