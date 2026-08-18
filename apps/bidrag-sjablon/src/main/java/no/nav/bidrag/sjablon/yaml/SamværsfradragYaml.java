package no.nav.bidrag.sjablon.yaml;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Data;
import no.nav.bidrag.sjablon.validering.Validerbar;
import no.nav.bidrag.sjablon.yaml.AldersgrupperYaml.Aldersgruppe;
import no.nav.bidrag.sjablon.yaml.TidslinjeYaml.Periode;

@Data
public class SamværsfradragYaml implements Validerbar {
  @JsonProperty("Samværsklasser")
  private Map<String, Samværsklasse> samværsklasser;

  @JsonProperty("Samværsfradrag")
  private TidslinjeYaml<AldersgrupperYaml<Map<String, BeløpYaml>>> samværsfradrag;

  @Data
  public static class Samværsklasse {
    @JsonProperty("Antall dager")
    private int antallDager;

    @JsonProperty("Antall netter")
    private int antallNetter;
  }

  @Override
  public void valider() {
    for (var periode : samværsfradrag.getPerioder()) {
      for (var aldersgruppe : periode.getVerdi().getGrupper()) {
        validerSamværsklasser(periode, aldersgruppe);
      }
    }
  }

  private void validerSamværsklasser(
      Periode<?> periode, Aldersgruppe<? extends Map<String, ?>> aldersgruppe) {
    validerSamværsklasse(periode, aldersgruppe, "00");
    validerSamværsklasse(periode, aldersgruppe, "01");
    validerSamværsklasse(periode, aldersgruppe, "02");
    validerSamværsklasse(periode, aldersgruppe, "03");
    validerSamværsklasse(periode, aldersgruppe, "04");
  }

  private void validerSamværsklasse(
      Periode<?> periode,
      Aldersgruppe<? extends Map<String, ?>> aldersgruppe,
      String samværsklasse) {
    checkArgument(
        aldersgruppe.getVerdi().containsKey(samværsklasse),
        "Periode "
            + periode.getPeriodeFra()
            + " aldersgruppe "
            + aldersgruppe.getÅrFra()
            + " mangler samværsklasse "
            + samværsklasse);
  }

  public static SamværsfradragYaml lastInn() {
    return SjablonerYaml.lastInn("sjabloner/samværsfradrag.yaml", SamværsfradragYaml.class);
  }
}
