package no.nav.bidrag.sjablon.service;

import static no.nav.bidrag.sjablon.mapping.SjablontallMapper.EVIGHETSDATO;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import no.nav.bidrag.sjablon.entity.Forbruksutgifter;
import no.nav.bidrag.sjablon.mapping.CurrentSjablonFilter;
import no.nav.bidrag.sjablon.yaml.SjablonerYaml;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ForbruksutgifterService {

  @Autowired private SjablonerYaml sjabloner;

  public List<Forbruksutgifter> getAllForbruksutgifter() {
    List<Forbruksutgifter> liste = new ArrayList<>();
    for (var periode : sjabloner.getForbruksutgifter().getForbruksutgifter().getPerioder()) {
      for (var gruppe : periode.getVerdi().getGrupper()) {
        Forbruksutgifter forbruksutgifter = new Forbruksutgifter();
        forbruksutgifter.setDatoFom(periode.getPeriodeFra());
        forbruksutgifter.setDatoTom(defaultIfNull(periode.getPeriodeTom(), EVIGHETSDATO));
        forbruksutgifter.setAlderTom(gruppe.getTom());
        forbruksutgifter.setBelopForbrukTot(gruppe.getVerdi().getBeløp());
        forbruksutgifter.setBelopIndivid(BigDecimal.ZERO);
        forbruksutgifter.setBelopHusholdning(BigDecimal.ZERO);
        forbruksutgifter.setBelopTransport(BigDecimal.ZERO);
        liste.add(forbruksutgifter);
      }
    }
    return liste;
  }

  public List<Forbruksutgifter> getCurrentForbruksutgifter() {
    return getAllForbruksutgifter().stream()
        .filter(
            new CurrentSjablonFilter<>(Forbruksutgifter::getDatoFom, Forbruksutgifter::getDatoTom))
        .toList();
  }
}
