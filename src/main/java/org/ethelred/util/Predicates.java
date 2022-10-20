package org.ethelred.util;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Predicates {
    private Predicates(){}

    public static <T> Predicate<T> and(Predicate<T> a, Predicate<T> b) {
        return new AndPredicate<>(List.of(a, b));
    }

    public static <T> Predicate<T> alwaysTrue() {
        return new Predicate<>() {
            @Override
            public boolean test(T t) {
                return true;
            }

            @Override
            public String toString() {
                return "True";
            }
        };
    }

    public static <T> Predicate<T> and(List<Predicate<T>> matchers) {
        return new AndPredicate<>(matchers);
    }

    /**
     * wrap as object rather than lambda, so it can have a toString()
     * @param <T>
     */
    private static class AndPredicate<T> implements Predicate<T> {
        private final List<Predicate<T>> predicates;

        private AndPredicate(List<Predicate<T>> predicates) {
            this.predicates = predicates;
        }

        @Override
        public boolean test(T subject) {
            return predicates.stream().allMatch(predicate -> predicate.test(subject));
        }

        @Override
        public String toString() {
            return predicates.stream()
                    .map(Objects::toString)
                    .collect(Collectors.joining(", ", "And[", "]"));
        }
    }
}
