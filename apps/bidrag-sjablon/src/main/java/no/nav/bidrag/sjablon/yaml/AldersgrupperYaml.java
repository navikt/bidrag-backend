package no.nav.bidrag.sjablon.yaml;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.Iterators;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class AldersgrupperYaml<T> {
  private static final String ALDER_KEY_PREFIX = "Fra ";
  private static final String ALDER_KEY_POSTFIX = " år";
  private static final int MAKS_ALDER_TIL = 100;
  private final List<Aldersgruppe<T>> grupper = new ArrayList<>();

  @JsonCreator
  public AldersgrupperYaml(LinkedHashMap<String, T> values) {
    var it = Iterators.peekingIterator(values.entrySet().iterator());
    while (it.hasNext()) {
      var entry = it.next();
      Integer fra = aldersgruppe(entry);
      Integer til = it.hasNext() ? aldersgruppe(it.peek()) : null;
      grupper.add(new Aldersgruppe<>(fra, til, entry.getValue()));
    }
  }

  private static int aldersgruppe(Map.Entry<String, ?> entry) {
    String key = entry.getKey();
    checkArgument(
        key.startsWith(ALDER_KEY_PREFIX) && key.endsWith(ALDER_KEY_POSTFIX),
        "Aldersgruppen må starte med '"
            + ALDER_KEY_PREFIX
            + "' og slutte med '"
            + ALDER_KEY_POSTFIX
            + "'");
    return Integer.parseInt(
        key.substring(ALDER_KEY_PREFIX.length(), key.length() - ALDER_KEY_POSTFIX.length()));
  }

  @Data
  public static class Aldersgruppe<T> {
    private final Integer årFra;
    private final Integer årTil;
    private final T verdi;

    public int getTom() {
      return (årTil != null ? årTil : MAKS_ALDER_TIL) - 1;
    }
  }
}
