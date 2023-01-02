package br.fracture.gradient.calculation.controler;

import br.fracture.gradient.calculation.model.DataEntry;
import br.fracture.gradient.calculation.model.ResultData;
import br.fracture.gradient.calculation.request.DataEntryRequest;
import br.fracture.gradient.calculation.service.CalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
public class ControllerFracture {

  private final CalculationService calculationService;

  @PostMapping("/calculation")
  public ResponseEntity<ResultData> calculation(@Valid @RequestBody DataEntry dataEntry) {

    var dataRequest = DataEntryRequest.buildDataEntryRequest(dataEntry);

    var result = calculationService.calculation(dataRequest);

    return new ResponseEntity<>(result, HttpStatus.OK);
  }
}