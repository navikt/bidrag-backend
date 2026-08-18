package no.nav.bidrag.sjablon.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Forbruksutgifter {
  int alderTom;
  LocalDate datoFom;
  LocalDate datoTom;
  BigDecimal belopForbrukTot;
  BigDecimal belopIndivid;
  BigDecimal belopHusholdning;
  BigDecimal belopTransport;
  String brukerid;
  LocalDateTime tidspktEndret;
}
