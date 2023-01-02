package br.fracture.gradient.calculation.request;

import br.fracture.gradient.calculation.model.DataEntry;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class DataEntryRequest {
  private final Long waterDeepLayer;
  private final Long deepIntervalSedimentSeismicData;
  private final DataEntry.RigType rigType;
  private final Long sedimentDeepLayer;
  private final List<Double> leakOfTestPressures;
  private final List<Double> leakOfTestDeeps;
  private final List<Integer> observedTransitTime;

  public long getTotalDeepWell() {
    return rigType.getHeight() + waterDeepLayer + sedimentDeepLayer;
  }

  public static DataEntryRequest buildDataEntryRequest(DataEntry dataEntry) {


    return DataEntryRequest.builder()
                           .waterDeepLayer(dataEntry.getWaterDeepLayer())
                           .deepIntervalSedimentSeismicData(dataEntry.getDeepIntervalSedimentSeismicData())
                           .rigType(DataEntry.RigType.findRigType(dataEntry.getWaterDeepLayer()))
                           .sedimentDeepLayer(dataEntry.getObservedTransitTime().size() * dataEntry.getDeepIntervalSedimentSeismicData())
                           .observedTransitTime(dataEntry.getObservedTransitTime())
                           .build();
  }

}