package co.edu.udes.algoritmos.u9.task;

import co.edu.udes.algoritmos.u9.model.Record;
import co.edu.udes.algoritmos.u9.service.RecordProcessor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

/**
 * Fork-join task that applies the pipeline using divide and conquer.
 */
public class RecordProcessorTask extends RecursiveTask<List<Record>> {
  private static final int THRESHOLD = 50;

  private final List<String> lines;
  private final RecordProcessor processor;

  public RecordProcessorTask(List<String> lines, RecordProcessor processor) {
    this.lines = lines;
    this.processor = processor;
  }

  @Override
  protected List<Record> compute() {
    if (lines.size() <= THRESHOLD) {
      return lines.stream()
          .map(processor::parse)
          .map(processor::enrich)
          .map(processor::transform)
          .filter(processor::validate)
          .collect(Collectors.toList());
    }

    int mid = lines.size() / 2;
    RecordProcessorTask left = new RecordProcessorTask(lines.subList(0, mid), processor);
    RecordProcessorTask right = new RecordProcessorTask(lines.subList(mid, lines.size()), processor);

    left.fork();
    List<Record> rightResult = right.compute();
    List<Record> leftResult = left.join();

    List<Record> combined = new ArrayList<>(leftResult);
    combined.addAll(rightResult);
    return combined;
  }
}
