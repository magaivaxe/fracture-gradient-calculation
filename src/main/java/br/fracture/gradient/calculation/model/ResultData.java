package br.fracture.gradient.calculation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public class ResultData {
  @JsonProperty("x-porePressureGradient")
  private final Double [] porePressureGradient;
  @JsonProperty("x-overBurdenGradient")
  private final Double [] overBurdenGradient;
  @JsonProperty("x-fractureGradient")
  private final Double [] fractureGradient;
  @JsonProperty("x-transitTime")
  private final Double [] transitTime;
  @JsonProperty("y-deep")
  private final Double [] deeps;
  @JsonProperty("x-normalTransitTime-y-deep")
  private final Long [] [] normalTransitTimeByDeeps;
}