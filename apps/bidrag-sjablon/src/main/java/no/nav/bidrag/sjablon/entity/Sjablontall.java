package no.nav.bidrag.sjablon.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Sjablontall {
  String typeSjablon;
  LocalDate datoFom;
  LocalDate datoTom;
  BigDecimal verdi;
  String brukerid;
  LocalDateTime tidspktEndret;
}
