package no.nav.bidrag.dokument.journalpost;

public class UrlsForApplication {
  private static UrlsForApplication instance;
  private final String bidragOrganisasjonUrl;
  private final String norgUrl;
  private final String oppgaverUrl;

  public UrlsForApplication(String bidragOrganisasjonUrl, String norgUrl, String oppgaverUrl) {
    this.bidragOrganisasjonUrl = bidragOrganisasjonUrl;
    this.norgUrl = norgUrl;
    this.oppgaverUrl = oppgaverUrl;
    instance = this;
  }

  public static String hentUrlForBidragOrganisasjon() {
    validateNonNull();
    return instance.bidragOrganisasjonUrl;
  }

  public static String hentUrlForNorg() {
    validateNonNull();
    return instance.norgUrl;
  }

  public static String hentUrlForOppgaver() {
    validateNonNull();
    return instance.oppgaverUrl;
  }

  private static void validateNonNull() {
    if (instance == null) {
      throw new IllegalStateException("Ingen urler for spring context!");
    }
  }
}
