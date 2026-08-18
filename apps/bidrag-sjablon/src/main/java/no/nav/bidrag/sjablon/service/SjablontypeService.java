package no.nav.bidrag.sjablon.service;

import static java.util.Arrays.asList;

import java.util.List;
import no.nav.bidrag.sjablon.entity.Sjablontype;
import org.springframework.stereotype.Service;

@Service
public class SjablontypeService {
  private final List<Sjablontype> sjablontyper =
      asList(
          new Sjablontype("0001", "Beløp ordinær barnetrygd"),
          new Sjablontype("0002", "Beløp ordinært småbarnstillegg"),
          new Sjablontype("0003", "Boutgifter bidragsbarn"),
          new Sjablontype("0004", "Fordel skatteklasse 2"),
          new Sjablontype("0005", "Forskuddssats"),
          new Sjablontype("0006", "Innslag kapitalinntekt"),
          new Sjablontype("0007", "Inntektsintervall tilleggsbidrag"),
          new Sjablontype("0008", "Maks prosent av inntekt bidragspliktig"),
          new Sjablontype("0009", "Multiplikator høy inntekt bidragspliktig"),
          new Sjablontype("0010", "Multiplikator inntektsinnslag bidragsbarn"),
          new Sjablontype("0011", "Multiplikator maks bidrag"),
          new Sjablontype("0012", "Multiplikator maks inntekt bidragsbarn"),
          new Sjablontype("0013", "Multiplikator maks innt.grense forskudd mottaker"),
          new Sjablontype("0014", "Nedre inntektsgrense gebyr"),
          new Sjablontype("0015", "Prosentsats skatt alminnelig inntekt"),
          new Sjablontype("0016", "Prosentsats tilleggsbidrag"),
          new Sjablontype("0017", "Prosentsats trygdeavgift"),
          new Sjablontype("0018", "Skatteprosent barnetillegg"),
          new Sjablontype("0019", "Underhold barn egen hus bidragspliktig"),
          new Sjablontype("0020", "Prosentgrense for endring av bidrag (10%-regel)"),
          new Sjablontype("0021", "Barnetillegg fra forsvaret. Første barn."),
          new Sjablontype("0022", "Barnetillegg fra forsvaret. Øvrige barn."),
          new Sjablontype("0023", "Minstefradrag"),
          new Sjablontype("0024", "Gjennomsnitt antall virkedager pr måned"),
          new Sjablontype("0025", "Minstefradrag prosent inntekt"),
          new Sjablontype("0026", "Daglig sats for barnetillegg"),
          new Sjablontype("0027", "Personfradrag klasse 1"),
          new Sjablontype("0028", "Personfradrag klasse 2"),
          new Sjablontype("0029", "Kontantststøtte"),
          new Sjablontype("0030", "Øvre inntektsgrense for ikke i skatteposisjon"),
          new Sjablontype("0031", "Nedre inntektsgrense for full skatteposisjon"),
          new Sjablontype("0032", "Ekstra småbarnstillegg"),
          new Sjablontype("0033", "Øvre inntektsgrense for fullt forskudd"),
          new Sjablontype("0034", "Øvre inntektsgrense for 75% forskudd (enslig)"),
          new Sjablontype("0035", "Øvre inntektsgrense for 75% forskudd (Gift/Samb)"),
          new Sjablontype("0036", "Inntektsintervall forskudd"),
          new Sjablontype("0037", "Øvre grense for særtilskudd"),
          new Sjablontype("0038", "Forskuddssats 75%"),
          new Sjablontype("0039", "Fordel  særfradrag"),
          new Sjablontype("0040", "Prosentsats alminnelig inntekt"),
          new Sjablontype("0041", "Beløp forhøyet barnetrygd"),
          new Sjablontype("0042", "Utvidet barnetrygd til bidragskalkulator"),
          new Sjablontype("0050", "Sjablon for indeksregulering"),
          new Sjablontype("0100", "Beløp fastsettelsesgebyr"));

  public List<Sjablontype> getAllSjablontype() {
    return sjablontyper;
  }
}
