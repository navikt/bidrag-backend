package no.nav.bidrag.sjablon.controller;

import java.util.List;
import no.nav.bidrag.sjablon.entity.Forbruksutgifter;
import no.nav.bidrag.sjablon.service.ForbruksutgifterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bidrag-sjablon")
public class ForbruksutgifterController {

  @Autowired private ForbruksutgifterService forbruksutgifterService;

  @GetMapping("/forbruksutgifter/all")
  public List<Forbruksutgifter> getAllForbruksutgifter() {
    return forbruksutgifterService.getAllForbruksutgifter();
  }

  @GetMapping("/forbruksutgifter")
  public List<Forbruksutgifter> getCurrentForbruksutgifter() {
    return forbruksutgifterService.getCurrentForbruksutgifter();
  }
}
