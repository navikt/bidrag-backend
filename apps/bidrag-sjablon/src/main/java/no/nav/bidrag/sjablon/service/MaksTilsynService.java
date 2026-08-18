package no.nav.bidrag.sjablon.service;

import static no.nav.bidrag.sjablon.mapping.SjablontallMapper.EVIGHETSDATO;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.ArrayList;
import java.util.List;
import no.nav.bidrag.sjablon.entity.MaksTilsyn;
import no.nav.bidrag.sjablon.mapping.CurrentSjablonFilter;
import no.nav.bidrag.sjablon.yaml.SjablonerYaml;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MaksTilsynService {

  @Autowired private SjablonerYaml sjabloner;

  public List<MaksTilsyn> getAllMaksTilsyn() {
    List<MaksTilsyn> liste = new ArrayList<>();
    for (var periode : sjabloner.getBidragsberegning().getMaksTilsyn().getPerioder()) {
      for (var gruppe : periode.getVerdi().getGrupper()) {
        MaksTilsyn maksTilsyn = new MaksTilsyn();
        maksTilsyn.setDatoFom(periode.getPeriodeFra());
        maksTilsyn.setDatoTom(defaultIfNull(periode.getPeriodeTom(), EVIGHETSDATO));
        maksTilsyn.setAntBarnTom(gruppe.getTom());
        maksTilsyn.setMaksBelopTilsyn(gruppe.getVerdi().getBeløp());
        liste.add(maksTilsyn);
      }
    }
    return liste;
  }

  public List<MaksTilsyn> getCurrentMaksTilsyn() {
    return getAllMaksTilsyn().stream()
        .filter(new CurrentSjablonFilter<>(MaksTilsyn::getDatoFom, MaksTilsyn::getDatoTom))
        .toList();
  }
}
