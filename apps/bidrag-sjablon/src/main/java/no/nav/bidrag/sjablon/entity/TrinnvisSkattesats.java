package no.nav.bidrag.sjablon.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class TrinnvisSkattesats {
  private LocalDate datoFom;
  private LocalDate datoTom;
  private BigDecimal inntektgrense;
  private BigDecimal sats;
  private String brukerid;
  private LocalDateTime tidspktReg;
}
