package no.nav.bidrag.sjablon.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MaksFradrag {
  int antBarnTom;
  LocalDate datoFom;
  LocalDate datoTom;
  BigDecimal maksBelopFradrag;
  String brukerid;
  LocalDateTime tidspktEndret;
}
