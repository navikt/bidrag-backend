package no.nav.bidrag.sjablon.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class Sjablontype {
  private String type;
  private String beskrivelse;
}
