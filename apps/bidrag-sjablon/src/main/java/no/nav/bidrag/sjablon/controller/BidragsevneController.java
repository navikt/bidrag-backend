package no.nav.bidrag.sjablon.controller;

import java.util.List;
import no.nav.bidrag.sjablon.entity.Bidragsevne;
import no.nav.bidrag.sjablon.service.BidragsevneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bidrag-sjablon")
public class BidragsevneController {

  @Autowired private BidragsevneService bidragsevneService;

  @GetMapping("/bidragsevner/all")
  public List<Bidragsevne> getAllBidragevne() {
    return bidragsevneService.getAllBidragsevne();
  }

  @GetMapping("/bidragsevner")
  public List<Bidragsevne> getCurrentBidragsevne() {
    return bidragsevneService.getCurrentBidragsevne();
  }
}
