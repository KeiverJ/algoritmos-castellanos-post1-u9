package co.edu.udes.algoritmos.u9.service;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.udes.algoritmos.u9.model.Record;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RecordProcessorTest {
  private final RecordProcessor processor = new RecordProcessor();

  @Test
  void parseBuildsRecordWithSource() {
    Record record = processor.parse("1,raw,src-a");

    assertThat(record.id()).isEqualTo("1");
    assertThat(record.rawData()).isEqualTo("raw");
    assertThat(record.metadata()).containsEntry("source", "src-a");
  }

  @Test
  void enrichAddsRegion() {
    Record base = processor.parse("2,raw,src-b");
    Record enriched = processor.enrich(base);

    assertThat(enriched.metadata()).containsKey("region");
    assertThat(enriched.metadata()).containsEntry("source", "src-b");
  }

  @Test
  void transformUppercasesAndRemovesSpaces() {
    Record base = new Record("3", "a b c", Map.of("source", "src-c"));
    Record transformed = processor.transform(base);

    assertThat(transformed.rawData()).isEqualTo("ABC");
  }

  @Test
  void validateRejectsInvalidRecords() {
    Record blankId = new Record("", "data", Map.of());
    Record shortData = new Record("1", "ab", Map.of());

    assertThat(processor.validate(blankId)).isFalse();
    assertThat(processor.validate(shortData)).isFalse();
  }

  @Test
  void processSequentialFiltersInvalidRecords() {
    List<String> lines = List.of(
        "1,good-data,src",
        "2,ab,src",
        "3,also-good,src"
    );

    List<Record> results = processor.processSequential(lines);

    assertThat(results).hasSize(2);
  }
}
