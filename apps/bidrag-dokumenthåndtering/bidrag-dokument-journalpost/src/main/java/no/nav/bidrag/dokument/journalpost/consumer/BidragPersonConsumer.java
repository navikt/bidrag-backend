package no.nav.bidrag.dokument.journalpost.consumer;


import static no.nav.bidrag.dokument.journalpost.configuration.CacheConfig.PERSON_CACHE;

import java.util.Optional;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.dokument.journalpost.model.BidragPerson;
import no.nav.bidrag.domene.ident.Personident;
import no.nav.bidrag.transport.person.PersonDto;
import no.nav.bidrag.transport.person.PersonRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class BidragPersonConsumer {

  private final RestTemplate restTemplate;

  public BidragPersonConsumer(HttpHeaderRestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Cacheable(PERSON_CACHE)
  public Optional<PersonDto> hentPerson(String id) {
    var personResponse =
        restTemplate.exchange(String.format(BidragPerson.HENT_PERSON_INFO_URL, id),
        HttpMethod.POST,
        new HttpEntity<>(new PersonRequest(new Personident(id))),
        PersonDto.class);
    return Optional.of(personResponse).map(ResponseEntity::getBody);
  }
}
