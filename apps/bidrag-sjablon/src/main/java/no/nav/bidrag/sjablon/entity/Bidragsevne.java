package no.nav.bidrag.sjablon.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Bidragsevne {
  String bostatus;
  LocalDate datoFom;
  LocalDate datoTom;
  BigDecimal belopBoutgift;
  BigDecimal belopUnderhold;
  String brukerid;
  LocalDateTime tidspktEndret;
}
