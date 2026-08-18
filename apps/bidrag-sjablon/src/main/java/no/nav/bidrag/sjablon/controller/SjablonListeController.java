package no.nav.bidrag.sjablon.controller;

import java.util.List;
import no.nav.bidrag.sjablon.entity.*;
import no.nav.bidrag.sjablon.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/bidrag-sjablon")
public class SjablonListeController {

  @Autowired private BarnetilsynService barnetilsynService;
  @Autowired private BidragsevneService bidragsevneService;
  @Autowired private ForbruksutgifterService forbruksutgifterService;
  @Autowired private MaksFradragService maksFradragService;
  @Autowired private MaksTilsynService maksTilsynService;
  @Autowired private SamvaersfradragService samvaersfradragService;
  @Autowired private SjablontallService sjablontallService;
  @Autowired private TrinnvisSkattesatsService trinnvisSkattesatsService;
  @Autowired private SjablontypeService sjablontypeService;

  @RequestMapping(value = "/sjabloner", method = RequestMethod.GET)
  public String sjabloner(Model model) {
    List<Barnetilsyn> barnetilsyn = barnetilsynService.getCurrentBarnetilsyn();
    List<Bidragsevne> bidragsevne = bidragsevneService.getCurrentBidragsevne();
    List<Forbruksutgifter> forbruksutgifter = forbruksutgifterService.getCurrentForbruksutgifter();
    List<MaksFradrag> maksFradrag = maksFradragService.getCurrentMaksFradrag();
    List<MaksTilsyn> maksTilsyn = maksTilsynService.getCurrentMaksTilsyn();
    List<Samvaersfradrag> samvaersfradrag = samvaersfradragService.getCurrentSamvaersfradrag();
    List<Sjablontall> sjablontall = sjablontallService.getCurrentSjablontall();
    List<TrinnvisSkattesats> trinnvisSkattesats =
        trinnvisSkattesatsService.getCurrentTrinnvisSkattesats();
    List<Sjablontype> sjablontype = sjablontypeService.getAllSjablontype();

    model.addAttribute("gjeldendeBarnetilsyn", barnetilsyn);
    model.addAttribute("gjeldendeBidragsevne", bidragsevne);
    model.addAttribute("gjeldendeForbruksutgifter", forbruksutgifter);
    model.addAttribute("gjeldendeMaksFradrag", maksFradrag);
    model.addAttribute("gjeldendeMaksTilsyn", maksTilsyn);
    model.addAttribute("gjeldendeSamvaersfradrag", samvaersfradrag);
    model.addAttribute("gjeldendeSjablontall", sjablontall);
    model.addAttribute("gjeldendeSjablontype", sjablontype);
    model.addAttribute("gjeldendeTrinnvisSkattesats", trinnvisSkattesats);

    return "sjabloner";
  }
}
