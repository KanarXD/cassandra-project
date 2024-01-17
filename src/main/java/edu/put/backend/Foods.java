package edu.put.backend;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Foods {
    // @formatter:off
    private static final Map<String, List<String>> foods = Map.of(
            "kebab", List.of("doner", "small", "medium", "large", "xxl", "spicy"),
            "pizza", List.of("margherita", "neapolitana", "jalapeno", "piccante", "salami", "hawaii"),
            "drink", List.of("tequila sunrise", "sex on the beach", "blue lagoon", "margarita", "cosmopolitan")
    );
    // @formatter:on

    public static List<String> categories() {
        return foods.keySet().stream().toList();
    }

    public static List<String> variants(String category) {
        return Objects.requireNonNullElseGet(foods.get(category), List::of);
    }
}
