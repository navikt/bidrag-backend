package no.nav.bidrag.sjablon.yaml;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import no.nav.bidrag.sjablon.validering.Validerbar;

@Data
public class SkattYaml implements Validerbar {

  @JsonProperty("Fordel skatteklasse 2")
  private TidslinjeYaml<BeløpYaml> fordelSkatteklasse2;

  @JsonProperty("Skatt alminnelig inntekt")
  private TidslinjeYaml<ProsentYaml> skattAlminneligInntekt;

  @JsonProperty("Personfradrag")
  private TidslinjeYaml<Personfradrag> personfradrag;

  @JsonProperty("Minstefradrag")
  private Minstefradrag minstefradrag;

  @JsonProperty("Trygdeavgift")
  private TidslinjeYaml<ProsentYaml> trygdeavgift;

  @JsonProperty("Skatt alminnelig inntekt for beregning av barnetilsyn")
  private TidslinjeYaml<ProsentYaml> skattAlminneligInntektForBeregningAvBarnetilsyn;

  @Data
  public static class Personfradrag {
    @JsonProperty("Klasse 1")
    private BeløpYaml beløpKlasse1;

    @JsonProperty("Klasse 2")
    private BeløpYaml beløpKlasse2;
  }

  @Data
  public static class Minstefradrag {
    @JsonProperty("Andel av inntekt")
    private TidslinjeYaml<ProsentYaml> andelAvInntekt;

    @JsonProperty("Øvre grense")
    private TidslinjeYaml<BeløpYaml> øvreGrense;
  }

  @Override
  public void valider() {
    checkNotNull(fordelSkatteklasse2, "Fordel skatteklasse 2 må angis");
    fordelSkatteklasse2.validerHverPeriode(p -> p.erNullEllerOver());

    checkNotNull(skattAlminneligInntekt, "Skatt alminnelig inntekt må angis");
  }

  public static SkattYaml lastInn() {
    return SjablonerYaml.lastInn("sjabloner/skatt.yaml", SkattYaml.class);
  }
}
