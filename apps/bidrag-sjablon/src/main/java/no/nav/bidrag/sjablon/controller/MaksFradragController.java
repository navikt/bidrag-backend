package no.nav.bidrag.sjablon.controller;

import java.util.List;
import no.nav.bidrag.sjablon.entity.MaksFradrag;
import no.nav.bidrag.sjablon.service.MaksFradragService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bidrag-sjablon")
public class MaksFradragController {

  @Autowired private MaksFradragService maksFradragService;

  @GetMapping("/maksfradrag/all")
  public List<MaksFradrag> getAllMaksFradrag() {
    return maksFradragService.getAllMaksFradrag();
  }

  @GetMapping("/maksfradrag")
  public List<MaksFradrag> getCurrentMaksFradrag() {
    return maksFradragService.getCurrentMaksFradrag();
  }
}
