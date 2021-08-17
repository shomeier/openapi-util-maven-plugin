package io.github.shomeier.maven.plugin.openapi.util;

import java.util.function.Predicate;

public final class Util {

    private Util() {}

    public static <T> boolean withNonNull(T input, Predicate<T> predicate, boolean defaultValue) {
        if (input != null) {
            return predicate.test(input);
        }

        return defaultValue;
    }
}
