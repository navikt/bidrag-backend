package no.nav.bidrag.sjablon.yaml;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.Data;

@Data
public class TidslinjeYaml<T> {
  private final List<Periode<T>> perioder = new ArrayList<>();

  @JsonCreator
  public TidslinjeYaml(LinkedHashMap<String, T> values) {
    LocalDate sisteFraDato = null;
    for (Map.Entry<String, T> entry : values.entrySet()) {
      LocalDate fraDato = LocalDate.parse(entry.getKey());

      if (sisteFraDato != null && !fraDato.isBefore(sisteFraDato)) {
        throw new IllegalArgumentException(
            "Periodene må legges inn i omvendt kronologisk rekkefølge. Periode "
                + fraDato
                + " er ikke før "
                + sisteFraDato
                + ".");
      }
      perioder.add(new Periode<>(fraDato, sisteFraDato, entry.getValue()));
      sisteFraDato = fraDato;
    }
  }

  @JsonValue
  public Map<String, T> toMap() {
    LinkedHashMap<String, T> periodeMap = new LinkedHashMap<>();
    perioder.stream().forEach(p -> periodeMap.put(p.getPeriodeFra().toString(), p.getVerdi()));
    return periodeMap;
  }

  public <R> List<R> map(Function<Periode<T>, R> mapper) {
    return perioder.stream().map(mapper).toList();
  }

  public <R> List<R> flatMap(Function<Periode<T>, Stream<R>> mapper) {
    return perioder.stream().flatMap(periode -> mapper.apply(periode)).toList();
  }

  public <V, R> List<R> mapPeriodeVerdier(
      Function<T, Collection<V>> verdiGetter, BiFunction<Periode<T>, V, R> mapper) {
    List<R> resultat = new ArrayList<>();
    for (Periode<T> periode : perioder) {
      Collection<V> verdier = verdiGetter.apply(periode.getVerdi());
      for (V verdi : verdier) {
        resultat.add(mapper.apply(periode, verdi));
      }
    }
    return resultat;
  }

  public <R> List<R> toList(BiConsumer<List<R>, Periode<T>> mapper) {
    List<R> list = new ArrayList<>();
    perioder.forEach(p -> mapper.accept(list, p));
    return list;
  }

  @Data
  public static class Periode<T> {
    private final LocalDate periodeFra;
    private final LocalDate periodeTil;
    private final T verdi;

    @Deprecated
    public LocalDate getPeriodeTom() {
      return periodeTil != null ? periodeTil.minusDays(1) : null;
    }
  }

  public void validerHverPeriode(Consumer<T> validator) {}
}
