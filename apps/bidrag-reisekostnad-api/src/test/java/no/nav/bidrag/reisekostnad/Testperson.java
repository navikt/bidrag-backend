package no.nav.bidrag.reisekostnad;

import java.time.LocalDate;
import lombok.Value;
import no.nav.bidrag.generer.testdata.person.PersonidentGeneratorKt;

@Value
public class Testperson {

  public static Testperson testpersonGråtass = new Testperson ("Gråtass", 40);
  public static Testperson testpersonStreng = new Testperson ("Streng", 38);
  public static Testperson testpersonSirup = new Testperson ("Sirup", 35);
  public static Testperson testpersonBarn16 = new Testperson ("Grus", 16);
  public static Testperson testpersonBarn10 = new Testperson ("Småstein", 10);
  public static Testperson testpersonIkkeFunnet = new Testperson ("Utenfor", 29);
  public static Testperson testpersonHarDiskresjon = new Testperson ("Diskos", 29);
  public static Testperson testpersonHarMotpartMedDiskresjon = new Testperson ("Tordivel", 44);
  public static Testperson testpersonHarBarnMedDiskresjon = new Testperson ("Kaktus", 48);
  public static Testperson testpersonErDød = new Testperson ("Steindød", 35);
  public static Testperson testpersonHarDødtBarn = new Testperson ("Albueskjell", 53);
  public static Testperson testpersonDødMotpart = new Testperson ("Bunkers", 41);
  public static Testperson testpersonServerfeil = new Testperson ("Feil", 78);

  String ident;
  String fornavn;
  int alder;
  LocalDate fødselsdato;

  public Testperson(String fornavn, int alder) {
    this(fornavn, alder, false);
  }

  // Sett utenIdent=true for å simulere en testperson uten kjent personident (ident settes til null i stedet for å genereres).
  public Testperson(String fornavn, int alder, boolean utenIdent) {
    this.fornavn = fornavn;
    this.alder = alder;
    this.fødselsdato = LocalDate.now().minusYears(alder);
    this.ident = utenIdent ? null : PersonidentGeneratorKt.genererFødselsnummer(this.fødselsdato, null);
  }
}
