package no.nav.bidrag.sjablon.controller;

import java.util.List;
import no.nav.bidrag.sjablon.entity.MaksTilsyn;
import no.nav.bidrag.sjablon.service.MaksTilsynService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bidrag-sjablon")
public class MaksTilsynController {

  @Autowired private MaksTilsynService maksTilsynService;

  @GetMapping("/makstilsyn/all")
  public List<MaksTilsyn> getAllMaksTilsyn() {
    return maksTilsynService.getAllMaksTilsyn();
  }

  @GetMapping("/makstilsyn")
  public List<MaksTilsyn> getCurrentMaksTilsyn() {
    return maksTilsynService.getCurrentMaksTilsyn();
  }
}
