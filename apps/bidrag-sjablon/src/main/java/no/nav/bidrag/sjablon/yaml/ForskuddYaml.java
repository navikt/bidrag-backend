package no.nav.bidrag.sjablon.yaml;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.Data;
import no.nav.bidrag.sjablon.validering.Validerbar;

@Data
public class ForskuddYaml implements Validerbar {
  @JsonProperty("Multiplikator for øvre inntektsgrense")
  private TidslinjeYaml<MultiplikatorYaml> multiplikatorØvreInntektsgrense;

  @JsonProperty("Forskudd")
  private TidslinjeYaml<Forskudd> forskudd;

  @Data
  public static class Forskudd {
    @JsonProperty("Inntektsgrenser")
    private Inntektsgrenser inntektsgrenser;

    @JsonProperty("Sats forhøyet forskudd")
    private BeløpYaml satsForhøyetForskudd;

    public BigDecimal getBeregnetOrdinærtForskudd() {
      if (satsForhøyetForskudd == null || satsForhøyetForskudd.getBeløp() == null) {
        return null;
      }
      BigDecimal forhøyetForskudd = satsForhøyetForskudd.getBeløp();
      return forhøyetForskudd
          // Gange med 75%
          .multiply(BigDecimal.valueOf(75, 2))
          // Runde av til nærmeste 10
          .setScale(-1, RoundingMode.HALF_UP);
    }
  }

  @Data
  public static class Inntektsgrenser {
    @JsonProperty("Forhøyet forskudd")
    private BeløpYaml forhøyetForskudd;

    @JsonProperty("Ordinært forskudd gift/samboer")
    private BeløpYaml ordinærtForskuddGiftSamboer;

    @JsonProperty("Ordinært forskudd enslig")
    private BeløpYaml ordinærtForskuddEnslig;

    @JsonProperty("Inntektsinterval")
    private BeløpYaml inntektsinterval;
  }

  @Override
  public void valider() {
    // TODO
  }

  public static ForskuddYaml lastInn() {
    return SjablonerYaml.lastInn("sjabloner/forskudd.yaml", ForskuddYaml.class);
  }
}
