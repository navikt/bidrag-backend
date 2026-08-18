package no.nav.bidrag.sjablon.controller;

import java.util.List;
import no.nav.bidrag.sjablon.entity.Sjablontall;
import no.nav.bidrag.sjablon.service.SjablontallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bidrag-sjablon")
public class SjablontallController {

  @Autowired private SjablontallService sjablontallService;

  @GetMapping("/sjablontall/all")
  public List<Sjablontall> getAllSjablontall() {
    return sjablontallService.getAllSjablontall();
  }

  @GetMapping("/sjablontall")
  public List<Sjablontall> getCurrentSjablontall() {
    return sjablontallService.getCurrentSjablontall();
  }
}
