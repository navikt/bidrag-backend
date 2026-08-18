package no.nav.bidrag.sjablon.yaml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import no.nav.bidrag.sjablon.validering.Validerbar;

@Data
public class StønaderYaml implements Validerbar {

  @JsonProperty("Barnetrygd")
  private TidslinjeYaml<BeløpYaml> barnetrygd;

  @JsonProperty("Forhøyet barnetrygd")
  private TidslinjeYaml<BeløpYaml> forhøyetBarnetrygd;

  @JsonProperty("Utvidet barnetrygd")
  private TidslinjeYaml<BeløpYaml> utvidetBarnetrygd;

  @Deprecated
  @JsonProperty("Småbarnstillegg")
  private TidslinjeYaml<BeløpYaml> småbarnstillegg;

  @JsonProperty("Ekstra småbarnstillegg")
  private TidslinjeYaml<BeløpYaml> ekstraSmåbarnstillegg;

  @JsonProperty("Kontantstøtte")
  private TidslinjeYaml<BeløpYaml> kontantstøtte;

  @JsonProperty("Daglig sats barnetillegg")
  private TidslinjeYaml<BeløpYaml> dagligSatsBarnetillegg;

  @Deprecated
  @JsonProperty("Skatteprosent barnetillegg")
  private TidslinjeYaml<ProsentYaml> skatteprosentBarnetillegg;

  @JsonProperty("Barnetillegg fra Forsvaret")
  private TidslinjeYaml<ForsvaretsBarnetillegg> forsvaretsBarnetillegg;

  @Data
  public static class ForsvaretsBarnetillegg {
    @JsonProperty("Første barn")
    private BeløpYaml førsteBarn;

    @JsonProperty("Øvrige barn")
    private BeløpYaml øvrigeBarn;
  }

  @Override
  public void valider() {
    // TODO Auto-generated method stub

  }

  public static StønaderYaml lastInn() {
    return SjablonerYaml.lastInn("sjabloner/stønader.yaml", StønaderYaml.class);
  }
}
