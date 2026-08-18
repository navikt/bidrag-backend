package no.nav.bidrag.sjablon.yaml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import no.nav.bidrag.sjablon.validering.Validerbar;

@Data
public class BidragsevneYaml implements Validerbar {

  @JsonProperty("Bidragspliktigs kostnader")
  private TidslinjeYaml<BidragspliktigsUtgifter> bidragspliktigsKostnader;

  @Data
  public static class BidragspliktigsUtgifter {
    @JsonProperty("Boutgifter")
    private BoforholdsavhengigUtgift boutgifter;

    @JsonProperty("Underhold")
    private BoforholdsavhengigUtgift underhold;

    @JsonProperty("Underhold egne barn")
    private BeløpYaml underholdEgneBarn;
  }

  @Data
  public static class BoforholdsavhengigUtgift {
    @JsonProperty("Bor alene")
    private BeløpYaml borAlene;

    @JsonProperty("Deler bolig")
    private BeløpYaml delerBolig;
  }

  @Override
  public void valider() {
    // TODO
  }

  public static BidragsevneYaml lastInn() {
    return SjablonerYaml.lastInn("sjabloner/bidragsevne.yaml", BidragsevneYaml.class);
  }
}
