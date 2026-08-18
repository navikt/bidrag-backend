package no.nav.bidrag.sjablon.service;

import static no.nav.bidrag.sjablon.mapping.SjablontallMapper.EVIGHETSDATO;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.ArrayList;
import java.util.List;
import no.nav.bidrag.sjablon.entity.Samvaersfradrag;
import no.nav.bidrag.sjablon.mapping.CurrentSjablonFilter;
import no.nav.bidrag.sjablon.yaml.SamværsfradragYaml.Samværsklasse;
import no.nav.bidrag.sjablon.yaml.SjablonerYaml;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SamvaersfradragService {

  @Autowired private SjablonerYaml sjabloner;

  public List<Samvaersfradrag> getAllSamvaersfradrag() {
    List<Samvaersfradrag> liste = new ArrayList<>();
    for (var periode : sjabloner.getSamværsfradrag().getSamværsfradrag().getPerioder()) {
      for (var gruppe : periode.getVerdi().getGrupper()) {
        for (var samværsklasse : gruppe.getVerdi().entrySet()) {
          Samvaersfradrag samvaersfradrag = new Samvaersfradrag();
          samvaersfradrag.setDatoFom(periode.getPeriodeFra());
          samvaersfradrag.setDatoTom(defaultIfNull(periode.getPeriodeTom(), EVIGHETSDATO));
          samvaersfradrag.setAlderTom(gruppe.getTom());
          samvaersfradrag.setSamvaersklasse(samværsklasse.getKey());
          samvaersfradrag.setBelopFradrag(samværsklasse.getValue().getBeløp());
          Samværsklasse samværsklasseInfo =
              sjabloner.getSamværsfradrag().getSamværsklasser().get(samværsklasse.getKey());
          if (samværsklasseInfo != null) {
            samvaersfradrag.setAntDagerTom(samværsklasseInfo.getAntallDager());
            samvaersfradrag.setAntNetterTom(samværsklasseInfo.getAntallNetter());
          }
          liste.add(samvaersfradrag);
        }
      }
    }
    return liste;
  }

  public List<Samvaersfradrag> getCurrentSamvaersfradrag() {
    return getAllSamvaersfradrag().stream()
        .filter(
            new CurrentSjablonFilter<>(Samvaersfradrag::getDatoFom, Samvaersfradrag::getDatoTom))
        .toList();
  }
}
