package no.nav.bidrag.sjablon.service;

import static no.nav.bidrag.sjablon.mapping.SjablontallMapper.EVIGHETSDATO;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.ArrayList;
import java.util.List;
import no.nav.bidrag.sjablon.entity.MaksFradrag;
import no.nav.bidrag.sjablon.mapping.CurrentSjablonFilter;
import no.nav.bidrag.sjablon.yaml.SjablonerYaml;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MaksFradragService {

  @Autowired private SjablonerYaml sjabloner;

  public List<MaksFradrag> getAllMaksFradrag() {
    ArrayList<MaksFradrag> liste = new ArrayList<>();
    for (var periode : sjabloner.getBidragsberegning().getMaksFradrag().getPerioder()) {
      for (var gruppe : periode.getVerdi().getGrupper()) {
        MaksFradrag maksFradrag = new MaksFradrag();
        maksFradrag.setDatoFom(periode.getPeriodeFra());
        maksFradrag.setDatoTom(defaultIfNull(periode.getPeriodeTom(), EVIGHETSDATO));
        maksFradrag.setAntBarnTom(gruppe.getTom());
        maksFradrag.setMaksBelopFradrag(gruppe.getVerdi().getBeløp());
        liste.add(maksFradrag);
      }
    }
    return liste;
  }

  public List<MaksFradrag> getCurrentMaksFradrag() {
    return getAllMaksFradrag().stream()
        .filter(new CurrentSjablonFilter<>(MaksFradrag::getDatoFom, MaksFradrag::getDatoTom))
        .toList();
  }
}
