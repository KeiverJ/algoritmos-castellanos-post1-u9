package co.edu.udes.algoritmos.u9.model;

import java.util.Map;

/**
 * Immutable data record that flows through the processing pipeline.
 */
public record Record(String id, String rawData, Map<String, String> metadata) {
}
