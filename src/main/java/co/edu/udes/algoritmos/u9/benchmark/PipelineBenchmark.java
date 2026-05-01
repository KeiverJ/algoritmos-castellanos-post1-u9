package co.edu.udes.algoritmos.u9.benchmark;

import co.edu.udes.algoritmos.u9.model.Record;
import co.edu.udes.algoritmos.u9.service.RecordProcessor;
import co.edu.udes.algoritmos.u9.task.RecordProcessorTask;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * JMH baseline benchmark for the sequential pipeline.
 */
@State(Scope.Benchmark)
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class PipelineBenchmark {
  @Param({"100", "1000", "5000"})
  private int recordCount;

  private List<String> data;
  private RecordProcessor processor;

  @Setup
  public void setup() {
    processor = new RecordProcessor();
    data = IntStream.range(0, recordCount)
        .mapToObj(i -> i + ",rawdata-" + i + ",src-" + (i % 5))
        .collect(Collectors.toList());
  }

  @Benchmark
  public List<Record> sequential() {
    return processor.processSequential(data);
  }

  @Benchmark
  public List<Record> parallelStream() {
    return processor.processParallelStream(data);
  }

  @Benchmark
  public List<Record> processAsync()
      throws ExecutionException, InterruptedException, TimeoutException {
    return processor.processAsync(data);
  }

  @Benchmark
  public List<Record> forkJoin() {
    ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
    try {
      return pool.invoke(new RecordProcessorTask(data, processor));
    } finally {
      pool.shutdown();
    }
  }
}
