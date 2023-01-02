package br.fracture.gradient.calculation.service;

import br.fracture.gradient.calculation.model.ResultData;
import br.fracture.gradient.calculation.request.DataEntryRequest;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor
@Service
@Slf4j
public class CalculationService {

  private static final double DENSITY_COEFFICIENT = 0.23;
  private static final double PSI_TO_KG_BY_M2 = 1.422;
  private static final double WATER_DENSITY = 1.03;
  private static final double FT_TO_M = 0.1704;
  private static final double SALT_WATER_CONVERSION = 8.5;

  public ResultData calculation(DataEntryRequest dataEntryRequest) {

    var transitTimes = dataEntryRequest.getObservedTransitTime();
    var incrementalDeep = dataEntryRequest.getDeepIntervalSedimentSeismicData();
    var waterDeep = dataEntryRequest.getWaterDeepLayer();
    var rigHeight = dataEntryRequest.getRigType().getHeight();

    var transitTimeByDeep = collectTransitTimeByDeep(transitTimes, incrementalDeep, waterDeep)
      .collect(Collectors.toList());
    var normalEquationData = normalEquationCalculus(new ArrayList<>(transitTimeByDeep));

    var dataPoints = transitTimeByDeep
      .stream()
      .map(statisticData(normalEquationData))
      .collect(SumPressureCollector.toSumPressureCollector(incrementalDeep))
      .stream()
      .map(overBurdenPressure(waterDeep))
      .map(overBurdenGradient(rigHeight))
      .map(hydrostaticPressure())
      .map(porePressure())
      .map(porePressureGradient())
      .map(fractureGradient())
      .collect(Collectors.toList());

    var deeps = dataPoints.stream().map(dataCalculus -> (-1) * dataCalculus.getCurrentDeepSediment()).toArray(Double[]::new);
    var porePressureGradient = dataPoints.stream().map(DataCalculus::getPorePressureGradient).toArray(Double[]::new);
    var overBurdenGradient = dataPoints.stream().map(DataCalculus::getOverBurdenGradient).toArray(Double[]::new);
    var fractureGradient = dataPoints.stream().map(DataCalculus::getFractureGradient).toArray(Double[]::new);
    var finalTransitTimes = dataPoints.stream().map(DataCalculus::getTransitTime).toArray(Double[]::new);

    var choseNormalTransitTimes = dataPoints.iterator().next().getChooseNormalTransitTimesByDeep()
                                      .stream()
                                      .map(pair -> (long) pair.getFirst()).toArray(Long[]::new);
    var choseNormalDeeps = dataPoints.iterator().next().getChooseNormalTransitTimesByDeep()
                                     .stream()
                                     .map(pair -> (-1) * pair.getSecond()).toArray(Long[]::new);

    var normalTransitTimeByDeeps = new Long [] [] {choseNormalTransitTimes, choseNormalDeeps};

    return ResultData.builder()
                     .porePressureGradient(porePressureGradient)
                     .overBurdenGradient(overBurdenGradient)
                     .fractureGradient(fractureGradient)
                     .transitTime(finalTransitTimes)
                     .deeps(deeps)
                     .normalTransitTimeByDeeps(normalTransitTimeByDeeps)
                     .build();
  }

  private Function<Pair<Integer, Long>, DataCalculus> statisticData(Triple<Double, Double, List<Pair<Integer, Long>>> normalEquationData) {
    var equationConstant = normalEquationData.getMiddle();
    var equationInclination = normalEquationData.getLeft();
    var chooseNormalTransitTimesByDeep = normalEquationData.getRight();

    return pair -> {
      double tt = pair.getFirst();
      long deep = pair.getSecond();
      var ttn = Math.round((deep - equationConstant) / equationInclination);
      var dc = DENSITY_COEFFICIENT * Math.pow(Math.pow(10, 6) / tt, 0.25);
      return DataCalculus.builder().transitTime(tt)
                         .densityCoefficient(dc)
                         .normalTransitTime(ttn)
                         .currentDeepSediment(pair.getSecond())
                         .chooseNormalTransitTimesByDeep(chooseNormalTransitTimesByDeep)
                         .build();
    };
  }

  private Function<DataCalculus, DataCalculus> overBurdenPressure(Long waterDeep) {
    return dataCalculus -> {
      var obp = PSI_TO_KG_BY_M2 * WATER_DENSITY * waterDeep + dataCalculus.getCurrentSumPressure();
      return dataCalculus.toBuilder().overBurdenPressure(obp).build();// PS
    };
  }

  private Function<DataCalculus, DataCalculus> overBurdenGradient(long rigHeight) {
    return dataCalculus -> {
      var obg = dataCalculus.getOverBurdenPressure() / (FT_TO_M * (dataCalculus.getCurrentDeepSediment() + rigHeight));
      return dataCalculus.toBuilder().overBurdenGradient(obg).build(); // GS
    };
  }

  private Function<DataCalculus, DataCalculus> hydrostaticPressure() {
    return dataCalculus -> {
      var hp = FT_TO_M * SALT_WATER_CONVERSION * dataCalculus.getCurrentDeepSediment();
      return dataCalculus.toBuilder().hydrostaticPressure(hp).build(); // Ph
    };
  }

  private Function<DataCalculus, DataCalculus> porePressure() {
    return dataCalculus -> {
      var obp = dataCalculus.getOverBurdenPressure();
      var hp = dataCalculus.getHydrostaticPressure();
      var tt = dataCalculus.getTransitTime();
      var ttn = (double) dataCalculus.getNormalTransitTime();
      var pp = obp - (obp - hp) * (Math.pow(ttn / tt, 2));
      return dataCalculus.toBuilder().porePressure(pp).build();
    };
  }

  private Function<DataCalculus, DataCalculus> fractureGradient() {
    return dataCalculus -> {
      var ppg = dataCalculus.getPorePressureGradient();
      var obg = dataCalculus.getOverBurdenGradient();
      var fg = (3 * ppg - obg + Math.sqrt(5 * Math.pow(ppg - obg, 2))) / 2;
      return dataCalculus.toBuilder().fractureGradient(fg).build();
    };
  }

  private Function<DataCalculus, DataCalculus> porePressureGradient() {
    return dataCalculus -> {
      var obg = dataCalculus.getOverBurdenGradient();
      var tt = dataCalculus.getTransitTime();
      var ttn = (double) dataCalculus.getNormalTransitTime();
      var ppg = obg - (obg - SALT_WATER_CONVERSION) * (Math.pow(ttn / tt, 2));
      return dataCalculus.toBuilder().porePressureGradient(Math.max(ppg, SALT_WATER_CONVERSION)).build();
    };
  }

  /**
   * y = mx + b <br>
   * m = (n * Sxy - Sx * Sy) / n * S(x^2) - (Sx) ^ 2 <br>
   * b = (S(x^2) * Sy - Sx * Sxy) / (n * (x^2) - (Sx) ^ 2) <br>
   * @return Triple of (m, b, chose points)
   */
  private Triple<Double, Double, List<Pair<Integer, Long>>> normalEquationCalculus(List<Pair<Integer, Long>> normalTransitTimeByDeep) {
    var n = normalTransitTimeByDeep.size();

    var sum_x = normalTransitTimeByDeep.stream().mapToDouble(Pair::getFirst).reduce(Double::sum).orElseThrow();
    var sum_y = normalTransitTimeByDeep.stream().mapToDouble(Pair::getSecond).reduce(Double::sum).orElseThrow();
    var sum_x_y = normalTransitTimeByDeep
      .stream().mapToDouble(pair -> pair.getFirst() * pair.getSecond()).reduce(Double::sum).orElseThrow();
    var sum_x2 = normalTransitTimeByDeep
      .stream().mapToDouble(pair -> Math.pow(pair.getFirst(), 2)).reduce(Double::sum).orElseThrow();
    var sum_2_x = Math.pow(sum_x, 2);

    var inclination = (n * sum_x_y - sum_x * sum_y) / (n * sum_x2 - sum_2_x);
    var constant = (sum_x2 * sum_y - sum_x * sum_x_y) / (n * sum_x2 - sum_2_x);

    var validError = isValidErrorCalculus(inclination, constant, normalTransitTimeByDeep.get(normalTransitTimeByDeep.size() - 1));

    if (validError) {
      return Triple.of(inclination, constant, normalTransitTimeByDeep);
    }

    normalTransitTimeByDeep.remove(normalTransitTimeByDeep.size() - 1);
    return normalEquationCalculus(normalTransitTimeByDeep);
  }

  private boolean isValidErrorCalculus(double inclination, double constant, Pair<Integer, Long> lastTransitTimeByDeep) {
    var lastDeep = lastTransitTimeByDeep.getSecond();
    double lastTransitTime = lastTransitTimeByDeep.getFirst();
    var normalTransitTime = (lastDeep - constant) / inclination;
    var error = (lastTransitTime - normalTransitTime) / normalTransitTime * 100;
    return error < 5;
  }

  private Stream<Pair<Integer, Long>> collectTransitTimeByDeep(List<Integer> transitTime, long incrementalDeep,
                                                               long waterDeep) {
    var count = new AtomicInteger(0);
    return transitTime.stream().map(tt -> Pair.of(tt, (count.incrementAndGet() * incrementalDeep) + waterDeep));
  }

  @Getter
  @Builder(toBuilder = true)
  static class DataCalculus {
    private final double transitTime;
    private final long normalTransitTime;
    private final double densityCoefficient;
    private final double currentDeepSediment;
    private final double currentSumPressure;
    private final double overBurdenPressure;
    private final double overBurdenGradient;
    private final double hydrostaticPressure;
    private final double porePressure;
    private final double porePressureGradient;
    private final double fractureGradient;
    private final List<Pair<Integer, Long>> chooseNormalTransitTimesByDeep;

  }


}