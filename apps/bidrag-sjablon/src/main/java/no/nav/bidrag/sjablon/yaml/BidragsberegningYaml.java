package no.nav.bidrag.sjablon.yaml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import no.nav.bidrag.sjablon.validering.Validerbar;

@Data
public class BidragsberegningYaml implements Validerbar {

  @JsonProperty("Boutgifter bidragsbarn")
  private TidslinjeYaml<BeløpYaml> boutgifterBidragsbarn;

  @JsonProperty("Kapitalinntekt innslag")
  private TidslinjeYaml<BeløpYaml> kapitalinntektInnslag;

  @JsonProperty("Endringsgrense")
  private TidslinjeYaml<ProsentYaml> endringsgrense;

  @JsonProperty("Virkedager i måneden")
  private TidslinjeYaml<TallYaml> virkedagerIMåneden;

  @JsonProperty("Bidragspliktigs inntekt")
  private TidslinjeYaml<BidragspliktigsInntekt> bidragspliktigsInntekt;

  @JsonProperty("Bidragsbarns inntekt")
  private TidslinjeYaml<BidragsbarnsInntekt> bidragsbarnsInntekt;

  @JsonProperty("Maks fradrag")
  private TidslinjeYaml<AntallBarnGrupper<BeløpYaml>> maksFradrag;

  @JsonProperty("Maks tilsyn")
  private TidslinjeYaml<AntallBarnGrupper<BeløpYaml>> maksTilsyn;

  @JsonProperty("Multiplikator maks bidrag")
  private TidslinjeYaml<MultiplikatorYaml> multiplikatorMaksBidrag;

  @Data
  public static class BidragspliktigsInntekt {
    @JsonProperty("Maks andel")
    private ProsentYaml maksAndel;

    @JsonProperty("Høy inntekt")
    private MultiplikatorYaml høyInntekt;
  }

  @Data
  public static class BidragsbarnsInntekt {
    @JsonProperty("Innslag")
    private MultiplikatorYaml innslag;

    @JsonProperty("Maks inntekt")
    private MultiplikatorYaml maksInntekt;
  }

  @Override
  public void valider() {}

  public static BidragsberegningYaml lastInn() {
    return SjablonerYaml.lastInn("sjabloner/bidragsberegning.yaml", BidragsberegningYaml.class);
  }
}
