package no.nav.bidrag.sjablon.yaml;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.Iterators;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.Getter;

@Getter
public class AntallBarnGrupper<T> {
  private static final String ANTALL_BARN_KEY_PREFIX = "Fra ";
  private static final String ANTALL_BARN_KEY_POSTFIX = " barn";
  private static final int MAKS_ANTALL_BARN_TIL = 100;
  private final List<AntallBarnGruppe<T>> grupper = new ArrayList<>();

  @JsonCreator
  public AntallBarnGrupper(LinkedHashMap<String, T> values) {
    var it = Iterators.peekingIterator(values.entrySet().iterator());
    while (it.hasNext()) {
      var entry = it.next();
      Integer fra = antallBarn(entry);
      Integer til = it.hasNext() ? antallBarn(it.peek()) : null;
      grupper.add(new AntallBarnGruppe<>(fra, til, entry.getValue()));
    }
  }

  private static int antallBarn(Map.Entry<String, ?> entry) {
    String key = entry.getKey();
    checkArgument(
        key.startsWith(ANTALL_BARN_KEY_PREFIX) && key.endsWith(ANTALL_BARN_KEY_POSTFIX),
        "Aldersgruppen må starte med '"
            + ANTALL_BARN_KEY_PREFIX
            + "' og slutte med '"
            + ANTALL_BARN_KEY_POSTFIX
            + "'");
    return Integer.parseInt(
        key.substring(
            ANTALL_BARN_KEY_PREFIX.length(), key.length() - ANTALL_BARN_KEY_POSTFIX.length()));
  }

  @Data
  public static class AntallBarnGruppe<T> {
    private final int fra;
    private final Integer til;
    private final T verdi;

    public int getTom() {
      return (til != null ? til : MAKS_ANTALL_BARN_TIL) - 1;
    }
  }
}
