package no.nav.bidrag.sjablon.controller;

import java.util.List;
import no.nav.bidrag.sjablon.entity.Barnetilsyn;
import no.nav.bidrag.sjablon.service.BarnetilsynService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bidrag-sjablon")
public class BarnetilsynController {

  @Autowired private BarnetilsynService barnetilsynService;

  @GetMapping("/barnetilsyn/all")
  public List<Barnetilsyn> getAllBarnetilsyn() {
    return barnetilsynService.getAllBarnetilsyn();
  }

  @GetMapping("/barnetilsyn")
  public List<Barnetilsyn> getCurrentBarnetilsyn() {
    return barnetilsynService.getCurrentBarnetilsyn();
  }
}
