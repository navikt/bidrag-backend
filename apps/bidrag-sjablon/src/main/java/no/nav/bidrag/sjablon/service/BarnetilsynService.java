package no.nav.bidrag.sjablon.service;

import static no.nav.bidrag.sjablon.mapping.SjablontallMapper.EVIGHETSDATO;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.ArrayList;
import java.util.List;
import no.nav.bidrag.sjablon.entity.Barnetilsyn;
import no.nav.bidrag.sjablon.mapping.CurrentSjablonFilter;
import no.nav.bidrag.sjablon.yaml.BarnetilsynYaml;
import no.nav.bidrag.sjablon.yaml.BarnetilsynYaml.HeltidDeltid;
import no.nav.bidrag.sjablon.yaml.BeløpYaml;
import no.nav.bidrag.sjablon.yaml.SjablonerYaml;
import no.nav.bidrag.sjablon.yaml.TidslinjeYaml.Periode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BarnetilsynService {

  @Autowired private SjablonerYaml sjabloner;

  public List<Barnetilsyn> getAllBarnetilsyn() {
    List<Barnetilsyn> liste = new ArrayList<>();
    for (var periode : sjabloner.getBarnetilsyn().getBarnetilsyn().getPerioder()) {
      leggTilBarnetilsyn(liste, periode, "D ", "H ", periode.getVerdi().getUavhengigAvAlder());
      leggTilBarnetilsyn(liste, periode, "DU", "HU", periode.getVerdi().getUnderSkolealder());
      leggTilBarnetilsyn(liste, periode, "DO", "HO", periode.getVerdi().getISkolealder());
    }
    return liste;
  }

  private static void leggTilBarnetilsyn(
      List<Barnetilsyn> list,
      Periode<BarnetilsynYaml.Barnetilsyn> periode,
      String typeTilsynDeltid,
      String typeTilsynHeltid,
      HeltidDeltid heltidDeltid) {
    if (heltidDeltid != null) {
      leggTilBarnetillegg(list, periode, typeTilsynDeltid, heltidDeltid.getDeltid());
      leggTilBarnetillegg(list, periode, typeTilsynHeltid, heltidDeltid.getHeltid());
    }
  }

  private static void leggTilBarnetillegg(
      List<Barnetilsyn> list,
      Periode<BarnetilsynYaml.Barnetilsyn> periode,
      String typeTilsyn,
      BeløpYaml beløp) {
    if (beløp != null && beløp.getBeløp() != null) {
      no.nav.bidrag.sjablon.entity.Barnetilsyn entity =
          new no.nav.bidrag.sjablon.entity.Barnetilsyn();
      entity.setTypeStonad("64");
      entity.setTypeTilsyn(typeTilsyn);
      entity.setDatoFom(periode.getPeriodeFra());
      entity.setDatoTom(defaultIfNull(periode.getPeriodeTom(), EVIGHETSDATO));
      entity.setBelopBarneTilsyn(beløp.getBeløp());
      list.add(entity);
    }
  }

  public List<Barnetilsyn> getCurrentBarnetilsyn() {
    return getAllBarnetilsyn().stream()
        .filter(new CurrentSjablonFilter<>(Barnetilsyn::getDatoFom, Barnetilsyn::getDatoTom))
        .toList();
  }
}
