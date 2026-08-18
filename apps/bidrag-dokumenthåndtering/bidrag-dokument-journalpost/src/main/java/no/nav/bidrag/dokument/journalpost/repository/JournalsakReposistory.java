package no.nav.bidrag.dokument.journalpost.repository;

import no.nav.bidrag.dokument.journalpost.entity.Journalsak;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JournalsakReposistory extends CrudRepository<Journalsak, Integer> {
  List<Journalsak> findBySaksnummer(String saksnummer);
}
