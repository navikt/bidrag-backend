package no.nav.bidrag.sjablon.mapping;

import java.time.LocalDate;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CurrentSjablonFilter<T> implements Predicate<T> {
  private final LocalDate currentAt;
  private final Function<? super T, LocalDate> fomFunction;
  private final Function<? super T, LocalDate> tomFunction;

  public CurrentSjablonFilter(
      Function<? super T, LocalDate> fomFunction, Function<? super T, LocalDate> tomFunction) {
    this(LocalDate.now(), fomFunction, tomFunction);
  }

  @Override
  public boolean test(T sjablon) {
    LocalDate fom = fomFunction.apply(sjablon);
    LocalDate tom = tomFunction.apply(sjablon);

    return !fom.isAfter(currentAt) && (tom == null || !tom.isBefore(currentAt));
  }
}
