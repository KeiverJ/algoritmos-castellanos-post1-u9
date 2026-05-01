package co.edu.udes.algoritmos.u9.service;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.udes.algoritmos.u9.model.Record;
import co.edu.udes.algoritmos.u9.task.RecordProcessorTask;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class RecordProcessorTest {
  private final RecordProcessor processor = new RecordProcessor();

  @Test
  void parseBuildsRecordWithSource() {
    Record parsedRecord = processor.parse("1,raw,src-a");

    assertThat(parsedRecord.id()).isEqualTo("1");
    assertThat(parsedRecord.rawData()).isEqualTo("raw");
    assertThat(parsedRecord.metadata()).containsEntry("source", "src-a");
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

  @Test
  void allPipelinesReturnSameCount()
      throws ExecutionException, InterruptedException, TimeoutException {
    List<String> lines = IntStream.range(0, 200)
        .mapToObj(i -> i + ",rawdata-" + i + ",src")
        .collect(Collectors.toList());

    int sequentialSize = processor.processSequential(lines).size();
    int parallelSize = processor.processParallelStream(lines).size();
    int asyncSize = processor.processAsync(lines).size();

    ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
    try {
      int forkJoinSize = pool.invoke(new RecordProcessorTask(lines, processor)).size();

      assertThat(parallelSize).isEqualTo(sequentialSize);
      assertThat(asyncSize).isEqualTo(sequentialSize);
      assertThat(forkJoinSize).isEqualTo(sequentialSize);
    } finally {
      pool.shutdown();
    }
  }

  @Test
  void throughputRegressionTest()
      throws ExecutionException, InterruptedException, TimeoutException {
    RecordProcessor localProcessor = new RecordProcessor();
    List<String> data = IntStream.range(0, 1000)
        .mapToObj(i -> i + ",data-" + i + ",src")
        .collect(Collectors.toList());

    long start = System.currentTimeMillis();
    List<Record> results = localProcessor.processAsync(data);
    long elapsed = System.currentTimeMillis() - start;

    assertThat(results).isNotEmpty();
    assertThat(elapsed)
        .as("Regression: processAsync must complete 1000 records in under 3000ms")
        .isLessThan(3000L);
  }
}
