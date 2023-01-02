package br.fracture.gradient.calculation.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class SumPressureCollector implements Collector<CalculationService.DataCalculus, List<CalculationService.DataCalculus>, List<CalculationService.DataCalculus>> {

  private final long intervalDeep;
  private final AtomicReference<Double> sumPressures = new AtomicReference<>(0.0);

  private SumPressureCollector(long intervalDeep) {
    this.intervalDeep = intervalDeep;
  }

  public static SumPressureCollector toSumPressureCollector(long intervalDeep) {
    return new SumPressureCollector(intervalDeep);
  }

  @Override
  public Supplier<List<CalculationService.DataCalculus>> supplier() {
    return ArrayList::new;
  }

  @Override
  public BiConsumer<List<CalculationService.DataCalculus>, CalculationService.DataCalculus> accumulator() {
    return (list, dataCalculus) -> {
      var currentSumPressure = sumPressures.get();
      var newPressure = sumPressures
        .accumulateAndGet(currentSumPressure, (v1, v2) -> v1 + dataCalculus.getDensityCoefficient() * intervalDeep);
      list.add(dataCalculus.toBuilder().currentSumPressure(newPressure).build());
    };
  }

  @Override
  public BinaryOperator<List<CalculationService.DataCalculus>> combiner() {
    return (list1, list2) -> {
      list1.addAll(list2);
      return list1;
    };
  }

  @Override
  public Function<List<CalculationService.DataCalculus>, List<CalculationService.DataCalculus>> finisher() {
    return Collections::unmodifiableList;
  }

  @Override
  public Set<Characteristics> characteristics() {
    return Set.of(Characteristics.CONCURRENT);
  }
}