package co.edu.udes.algoritmos.u9.service;

import co.edu.udes.algoritmos.u9.model.Record;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Processing pipeline for text records.
 *
 * <p>Overall complexity for sequential processing is O(n * m), where n is the
 * number of records and m is the average length of the raw data string.</p>
 */
public class RecordProcessor {
  /**
   * Stage 1: Parse (light CPU-bound, O(n) on the input string length).
   */
  public Record parse(String line) {
    String[] parts = line.split(",", -1);
    return new Record(
        parts[0],
        parts[1],
        Map.of("source", parts.length > 2 ? parts[2] : "unknown")
    );
  }

  /**
   * Stage 2: Enrich (simulates I/O-bound lookup).
   */
  public Record enrich(Record r) {
    try {
      Thread.sleep(0, 500_000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    var enriched = new HashMap<>(r.metadata());
    enriched.put("region", resolveRegion(r.id()));
    return new Record(r.id(), r.rawData(), Collections.unmodifiableMap(enriched));
  }

  /**
   * Stage 3: Transform (heavy CPU-bound work).
   */
  public Record transform(Record r) {
    String transformed = r.rawData().chars()
        .filter(c -> c != ' ')
        .map(Character::toUpperCase)
        .collect(StringBuilder::new,
            (sb, c) -> sb.append((char) c),
            StringBuilder::append)
        .toString();
    return new Record(r.id(), transformed, r.metadata());
  }

  /**
   * Stage 4: Validate (light CPU-bound check).
   */
  public boolean validate(Record r) {
    return r.id() != null && !r.id().isBlank()
        && r.rawData() != null && r.rawData().length() >= 3;
  }

  /**
   * Full sequential pipeline.
   */
  public List<Record> processSequential(List<String> lines) {
    return lines.stream()
        .map(this::parse)
        .map(this::enrich)
        .map(this::transform)
        .filter(this::validate)
        .collect(Collectors.toList());
  }

  private String resolveRegion(String id) {
    return switch (id.charAt(0) % 3) {
      case 0 -> "us-east";
      case 1 -> "eu-west";
      default -> "ap-south";
    };
  }
}
