package no.nav.bidrag.sjablon.service;

import static no.nav.bidrag.sjablon.mapping.SjablontallMapper.EVIGHETSDATO;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.List;
import no.nav.bidrag.sjablon.entity.TrinnvisSkattesats;
import no.nav.bidrag.sjablon.mapping.CurrentSjablonFilter;
import no.nav.bidrag.sjablon.yaml.ProsentYaml;
import no.nav.bidrag.sjablon.yaml.SjablonerYaml;
import no.nav.bidrag.sjablon.yaml.TidslinjeYaml.Periode;
import no.nav.bidrag.sjablon.yaml.TrinnvisSkattesatsYaml.Trinn;
import no.nav.bidrag.sjablon.yaml.TrinnvisSkattesatsYaml.Trinnskatt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TrinnvisSkattesatsService {

  @Autowired private SjablonerYaml sjabloner;

  public List<TrinnvisSkattesats> getAllTrinnvisSkattesats() {
    return sjabloner
        .getTrinnvisSkattesats()
        .getTrinnvisSkattesats()
        .mapPeriodeVerdier(Trinnskatt::getTrinn, this::tilTrinnvisSkattesats);
  }

  public List<TrinnvisSkattesats> getCurrentTrinnvisSkattesats() {
    return getAllTrinnvisSkattesats().stream()
        .filter(
            new CurrentSjablonFilter<>(
                TrinnvisSkattesats::getDatoFom, TrinnvisSkattesats::getDatoTom))
        .toList();
  }

  private TrinnvisSkattesats tilTrinnvisSkattesats(Periode<Trinnskatt> periode, Trinn trinn) {
    TrinnvisSkattesats trinnvisSkattesats = new TrinnvisSkattesats();
    trinnvisSkattesats.setDatoFom(periode.getPeriodeFra());
    trinnvisSkattesats.setDatoTom(defaultIfNull(periode.getPeriodeTom(), EVIGHETSDATO));
    trinnvisSkattesats.setInntektgrense(trinn.getBeløpFra());
    trinnvisSkattesats.setSats(trinn.getFaktor().multiply(ProsentYaml.PROSENT_NEVNER));
    return trinnvisSkattesats;
  }
}
