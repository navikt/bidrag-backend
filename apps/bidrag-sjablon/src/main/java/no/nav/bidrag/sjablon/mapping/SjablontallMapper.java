package no.nav.bidrag.sjablon.mapping;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.Getter;
import no.nav.bidrag.sjablon.entity.Sjablontall;
import no.nav.bidrag.sjablon.yaml.BeløpYaml;
import no.nav.bidrag.sjablon.yaml.MultiplikatorYaml;
import no.nav.bidrag.sjablon.yaml.ProsentYaml;
import no.nav.bidrag.sjablon.yaml.TidslinjeYaml;
import no.nav.bidrag.sjablon.yaml.TidslinjeYaml.Periode;

public class SjablontallMapper {
  public static final LocalDate EVIGHETSDATO = LocalDate.of(9999, 12, 31);
  @Getter private final List<Sjablontall> liste = new ArrayList<>();

  public <V> SjablontallMapper add(
      String sjablonType, TidslinjeYaml<V> tidslinje, Function<V, BigDecimal> mapper) {
    for (Periode<V> periode : tidslinje.getPerioder()) {
      BigDecimal verdi = mapper.apply(periode.getVerdi());
      if (verdi != null) {
        Sjablontall sjablontall = new Sjablontall();
        sjablontall.setTypeSjablon(sjablonType);
        sjablontall.setDatoFom(periode.getPeriodeFra());
        sjablontall.setDatoTom(defaultIfNull(periode.getPeriodeTom(), EVIGHETSDATO));
        sjablontall.setVerdi(verdi);
        liste.add(sjablontall);
      }
    }
    return this;
  }

  public SjablontallMapper addBeløp(String sjablontype, TidslinjeYaml<BeløpYaml> tidslinje) {
    return add(sjablontype, tidslinje, nullSafe(BeløpYaml::getBeløp));
  }

  public <V> SjablontallMapper addBeløp(
      String sjablonType, TidslinjeYaml<V> tidslinje, Function<V, BeløpYaml> mapper) {
    return add(sjablonType, tidslinje, mapper.andThen(nullSafe(BeløpYaml::getBeløp)));
  }

  public SjablontallMapper addMultiplikator(
      String sjablontype, TidslinjeYaml<MultiplikatorYaml> tidslinje) {
    return add(sjablontype, tidslinje, nullSafe(MultiplikatorYaml::getMultiplikator));
  }

  public <V> SjablontallMapper addMultiplikator(
      String sjablonType, TidslinjeYaml<V> tidslinje, Function<V, MultiplikatorYaml> mapper) {
    return add(
        sjablonType, tidslinje, mapper.andThen(nullSafe(MultiplikatorYaml::getMultiplikator)));
  }

  public SjablontallMapper addProsent(String sjablontype, TidslinjeYaml<ProsentYaml> tidslinje) {
    return add(sjablontype, tidslinje, nullSafe(ProsentYaml::getProsentsats));
  }

  public <V> SjablontallMapper addProsent(
      String sjablonType, TidslinjeYaml<V> tidslinje, Function<V, ProsentYaml> mapper) {
    return add(sjablonType, tidslinje, mapper.andThen(nullSafe(ProsentYaml::getProsentsats)));
  }

  private static <I, O> Function<I, O> nullSafe(Function<I, O> function) {
    return input -> {
      return input != null ? function.apply(input) : null;
    };
  }
}
