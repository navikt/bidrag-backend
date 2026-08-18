package no.nav.bidrag.sjablon.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Samvaersfradrag {
  private String samvaersklasse;
  private int alderTom;
  private LocalDate datoFom;
  private LocalDate datoTom;
  private int antDagerTom;
  private int antNetterTom;
  private BigDecimal belopFradrag;
  private String brukerid;
  private LocalDateTime tidspktEndret;
}
