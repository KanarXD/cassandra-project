package edu.put.backend;

import java.util.List;
import java.util.Map;

public class Common {
    public static final Map<String, List<String>> foodMap = Map.of(
            "kebab", List.of("doner", "small", "medium", "large", "xxl", "spicy"),
            "pizza", List.of("margherita", "neapolitana", "jalapeno", "piccante", "salami", "hawaii"),
            "drink", List.of("tequila sunrise", "sex on the beach", "blue lagoon", "margarita", "cosmopolitan")
    );

}
