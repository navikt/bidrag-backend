package no.nav.bidrag.sjablon.yaml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import no.nav.bidrag.sjablon.validering.Validerbar;

@Data
public class TilleggsbidragYaml implements Validerbar {
  @JsonProperty("Tilleggsbidrag")
  private TidslinjeYaml<Tilleggsbidrag> tilleggsbidrag;

  @Data
  public static class Tilleggsbidrag {
    @JsonProperty("Inntektsinterval")
    private BeløpYaml inntektsinterval;

    @JsonProperty("Prosentsats")
    private ProsentYaml prosentsats;
  }

  @Override
  public void valider() {
    // TODO
  }

  public static TilleggsbidragYaml lastInn() {
    return SjablonerYaml.lastInn("sjabloner/tilleggsbidrag.yaml", TilleggsbidragYaml.class);
  }
}
