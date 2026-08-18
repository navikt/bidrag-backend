package no.nav.bidrag.sjablon.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Barnetilsyn {
  String typeStonad;
  String typeTilsyn;
  LocalDate datoFom;
  LocalDate datoTom;
  BigDecimal belopBarneTilsyn;
  String brukerid;
  LocalDateTime tidspktEndret;
}
