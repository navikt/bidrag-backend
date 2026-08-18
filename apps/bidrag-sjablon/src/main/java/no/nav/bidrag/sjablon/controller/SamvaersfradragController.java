package no.nav.bidrag.sjablon.controller;

import java.util.List;
import no.nav.bidrag.sjablon.entity.Samvaersfradrag;
import no.nav.bidrag.sjablon.service.SamvaersfradragService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bidrag-sjablon")
public class SamvaersfradragController {

  @Autowired private SamvaersfradragService samvaersfradragService;

  @GetMapping("/samvaersfradrag/all")
  public List<Samvaersfradrag> getAllSamvaersfradrag() {
    return samvaersfradragService.getAllSamvaersfradrag();
  }

  @GetMapping("/samvaersfradrag")
  public List<Samvaersfradrag> getCurrentSamvaersfradrag() {
    return samvaersfradragService.getCurrentSamvaersfradrag();
  }
}
