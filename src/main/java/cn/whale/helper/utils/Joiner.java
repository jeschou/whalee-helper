package cn.whale.helper.utils;

import java.util.function.BinaryOperator;

/**
 * use for stream api, reduce
 */
public class Joiner implements BinaryOperator<String> {

    private final String sep;

    private Joiner(String sep) {
        this.sep = sep;
    }

    public static Joiner on(String sep) {
        return new Joiner(sep);
    }

    @Override
    public String apply(String s, String s2) {
        return s + sep + s2;
    }
}
