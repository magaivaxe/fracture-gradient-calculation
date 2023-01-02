package br.fracture.gradient.calculation;

import br.fracture.gradient.calculation.model.DataEntry;
import br.fracture.gradient.calculation.request.DataEntryRequest;
import br.fracture.gradient.calculation.service.CalculationService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class FractureGradientCalculationApplicationTests extends Assertions {

	private static final List<Integer> TRANSIT_TIME = List.of(
	  150,150,150,150,165,158,155,148,149,145,142,141,149,140,138,137,135,
    133,132,126,123,125,124,121,118,119,115,105,104,110,119,113,112,109,
    96,105,97,101,95,94,98,96,100,97,101,98,102,99,105,100,110,109,97,95,
    96,98,100,105,99,110,102,110,105,115,108,106,105,103,102,101,99);
	private final CalculationService calculationService = new CalculationService();

	@Test
	void contextLoads() {
		// Arrange
		var data = DataEntry.builder()
												.waterDeepLayer(1000L)
												.deepIntervalSedimentSeismicData(50L)
												.observedTransitTime(TRANSIT_TIME)
												.build();
		var request = DataEntryRequest.buildDataEntryRequest(data);
		// Act
		var result = calculationService.calculation(request);
		assertThat(result).isNotNull();
	}

}