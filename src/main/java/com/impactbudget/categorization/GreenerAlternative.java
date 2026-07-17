package com.impactbudget.categorization;

import java.util.List;

/** A higher-sustainability merchant suggested as a swap for a low-scoring one. */
public record GreenerAlternative(String merchant, int sustainabilityScore, List<String> flags) {
}
