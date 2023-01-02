package br.fracture.gradient.calculation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class DataEntry {

  @JsonProperty("water-deep-layer")
  private Long waterDeepLayer;
  @JsonProperty("deep-interval-data")
  private Long deepIntervalSedimentSeismicData;
  @NotEmpty
  @JsonProperty(value = "observed-transit-time", required = true)
  private List<Integer> observedTransitTime;

  @RequiredArgsConstructor
  @Getter
  public enum RigType {
    OFF_SHORE(30),
    ON_SHORE(5);

    private final long height;

    public static RigType findRigType(long height) {
      if (height == 0) {
        return ON_SHORE;
      } else if (height > 0) {
        return OFF_SHORE;
      } else {
        throw new IllegalArgumentException("height must be positive");
      }
    }
  }
}