package no.nav.bidrag.sjablon.controller;

import java.util.List;
import no.nav.bidrag.sjablon.entity.TrinnvisSkattesats;
import no.nav.bidrag.sjablon.service.TrinnvisSkattesatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bidrag-sjablon")
public class TrinnvisSkattesatsController {

  @Autowired private TrinnvisSkattesatsService trinnvisSkattesatsService;

  @GetMapping("/trinnvisskattesats/all")
  public List<TrinnvisSkattesats> getAllTrinnvisSkattesats() {
    return trinnvisSkattesatsService.getAllTrinnvisSkattesats();
  }

  @GetMapping("/trinnvisskattesats")
  public List<TrinnvisSkattesats> getCurrentTrinnvisSkattesats() {
    return trinnvisSkattesatsService.getCurrentTrinnvisSkattesats();
  }
}
