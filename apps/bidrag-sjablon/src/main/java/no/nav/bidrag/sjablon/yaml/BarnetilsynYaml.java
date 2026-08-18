package no.nav.bidrag.sjablon.yaml;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import no.nav.bidrag.sjablon.validering.Validerbar;

@Data
public class BarnetilsynYaml implements Validerbar {
  @JsonProperty("Barnetilsyn")
  private TidslinjeYaml<Barnetilsyn> barnetilsyn;

  @Data
  public static class Barnetilsyn implements Validerbar {
    @JsonProperty("Under skolealder")
    private HeltidDeltid underSkolealder;

    @JsonProperty("I skolealder")
    private HeltidDeltid iSkolealder;

    @JsonProperty("Uavhengig av alder")
    private HeltidDeltid uavhengigAvAlder;

    @Override
    public void valider() {
      if (uavhengigAvAlder != null) {
        checkState(
            underSkolealder == null,
            "Under skolealder kan ikke være angitt når Uavhengig av alder er angitt");
        checkState(
            iSkolealder == null,
            "I skolealder kan ikke være angitt når Uavhengig av alder er angitt");
      } else {
        checkState(
            underSkolealder != null,
            "Under skolealder må være angitt når Uavhengig av alder er angitt");
        checkState(
            iSkolealder != null, "I skolealder må være angitt når Uavhengig av alder er angitt");
      }
    }
  }

  @Data
  public static class HeltidDeltid implements Validerbar {
    @JsonProperty("Deltid")
    private BeløpYaml deltid;

    @JsonProperty("Heltid")
    private BeløpYaml heltid;

    @Override
    public void valider() {
      checkNotNull(deltid, "Deltid må være angitt");
      checkNotNull(heltid, "Heltid må være angitt");
    }
  }

  @Override
  public void valider() {
    // TODO
  }

  public static BarnetilsynYaml lastInn() {
    return SjablonerYaml.lastInn("sjabloner/barnetilsyn.yaml", BarnetilsynYaml.class);
  }
}
