package no.nav.bidrag.sjablon.controller;

import java.util.List;
import no.nav.bidrag.sjablon.entity.Sjablontype;
import no.nav.bidrag.sjablon.service.SjablontypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bidrag-sjablon")
public class SjablontypeController {

  @Autowired private SjablontypeService sjablontypeService;

  @GetMapping("/sjablontyper")
  public List<Sjablontype> getAllSjablontype() {
    return sjablontypeService.getAllSjablontype();
  }
}
