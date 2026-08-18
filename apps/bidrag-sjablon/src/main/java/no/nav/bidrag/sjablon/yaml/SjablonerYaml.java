package no.nav.bidrag.sjablon.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import lombok.Data;
import no.nav.bidrag.sjablon.validering.ValideringModule;
import org.springframework.stereotype.Component;

@Data
@Component
public class SjablonerYaml {
  private final BarnetilsynYaml barnetilsyn = BarnetilsynYaml.lastInn();
  private final BidragsberegningYaml bidragsberegning = BidragsberegningYaml.lastInn();
  private final BidragsevneYaml bidragsevne = BidragsevneYaml.lastInn();
  private final ForskuddYaml forskudd = ForskuddYaml.lastInn();
  private final ForbruksutgifterYaml forbruksutgifter = ForbruksutgifterYaml.lastInn();
  private final GebyrYaml gebyr = GebyrYaml.lastInn();
  private final SamværsfradragYaml samværsfradrag = SamværsfradragYaml.lastInn();
  private final SkattYaml skatt = SkattYaml.lastInn();
  private final StønaderYaml stønader = StønaderYaml.lastInn();
  private final SærbidragYaml særbidrag = SærbidragYaml.lastInn();
  private final SærfradragYaml særfradrag = SærfradragYaml.lastInn();
  private final TilleggsbidragYaml tilleggsbidragYaml = TilleggsbidragYaml.lastInn();
  private final TrinnvisSkattesatsYaml trinnvisSkattesats = TrinnvisSkattesatsYaml.lastInn();
  private final IndeksreguleringYaml indeksregulering = IndeksreguleringYaml.lastInn();

  public static <T> T lastInn(String file, Class<T> type) {
    try {
      return new ObjectMapper(new YAMLFactory())
          .registerModule(new ValideringModule())
          .findAndRegisterModules()
          .readValue(SjablonerYaml.class.getClassLoader().getResourceAsStream(file), type);

    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Kunne ikke laste inn sjabloner av typen " + type.getSimpleName() + " fra fil " + file,
          e);
    }
  }
}
