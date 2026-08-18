package no.nav.bidrag.sjablon.service;

import static no.nav.bidrag.sjablon.mapping.SjablontallMapper.EVIGHETSDATO;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import no.nav.bidrag.sjablon.entity.Bidragsevne;
import no.nav.bidrag.sjablon.mapping.CurrentSjablonFilter;
import no.nav.bidrag.sjablon.yaml.SjablonerYaml;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BidragsevneService {

  @Autowired private SjablonerYaml sjabloner;

  public List<Bidragsevne> getAllBidragsevne() {
    List<Bidragsevne> liste = new ArrayList<>();
    for (var periode : sjabloner.getBidragsevne().getBidragspliktigsKostnader().getPerioder()) {
      LocalDate fom = periode.getPeriodeFra();
      LocalDate tom = periode.getPeriodeTom();

      Bidragsevne bidragsevneBorAlene = new Bidragsevne();
      bidragsevneBorAlene.setDatoFom(fom);
      bidragsevneBorAlene.setDatoTom(defaultIfNull(tom, EVIGHETSDATO));
      bidragsevneBorAlene.setBostatus("EN");
      bidragsevneBorAlene.setBelopBoutgift(
          periode.getVerdi().getBoutgifter().getBorAlene().getBeløp());
      bidragsevneBorAlene.setBelopUnderhold(
          periode.getVerdi().getUnderhold().getBorAlene().getBeløp());
      liste.add(bidragsevneBorAlene);

      Bidragsevne bidragsevneDelerBolig = new Bidragsevne();
      bidragsevneDelerBolig.setDatoFom(fom);
      bidragsevneDelerBolig.setDatoTom(defaultIfNull(tom, EVIGHETSDATO));
      bidragsevneDelerBolig.setBostatus("GS");
      bidragsevneDelerBolig.setBelopBoutgift(
          periode.getVerdi().getBoutgifter().getDelerBolig().getBeløp());
      bidragsevneDelerBolig.setBelopUnderhold(
          periode.getVerdi().getUnderhold().getDelerBolig().getBeløp());
      liste.add(bidragsevneDelerBolig);
    }
    return liste;
  }

  public List<Bidragsevne> getCurrentBidragsevne() {
    return getAllBidragsevne().stream()
        .filter(new CurrentSjablonFilter<>(Bidragsevne::getDatoFom, Bidragsevne::getDatoTom))
        .toList();
  }
}
