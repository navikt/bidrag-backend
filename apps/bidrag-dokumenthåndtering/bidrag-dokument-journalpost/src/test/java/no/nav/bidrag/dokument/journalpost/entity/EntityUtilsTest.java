package no.nav.bidrag.dokument.journalpost.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EntityUtilsTest {


  @Test
  public void shouldTruncateBrukerId(){
    assertThat(EntityUtils.truncateBrukerId("bidrag-dokument-arkivering", 15)).isEqualTo("biddokarkiverin");
    assertThat(EntityUtils.truncateBrukerId("", 15)).isEqualTo("");
    assertThat(EntityUtils.truncateBrukerId(null, 15)).isEqualTo(null);
    assertThat(EntityUtils.truncateBrukerId("Z123123", 15)).isEqualTo("Z123123");
    assertThat(EntityUtils.truncateBrukerId("Z", 15)).isEqualTo("z");
    assertThat(EntityUtils.truncateBrukerId("bidrag-person", 15)).isEqualTo("bidperson");
    assertThat(EntityUtils.truncateBrukerId("bidrag-dokument", 15)).isEqualTo("biddok");
    assertThat(EntityUtils.truncateBrukerId("bidrag-dokument-journalpost", 15)).isEqualTo("biddokjournalpo");
    assertThat(EntityUtils.truncateBrukerId("srvbdarkivering", 15)).isEqualTo("srvbdarkivering");
    assertThat(EntityUtils.truncateBrukerId("srvbidrag", 15)).isEqualTo("srvbidrag");
  }
}
