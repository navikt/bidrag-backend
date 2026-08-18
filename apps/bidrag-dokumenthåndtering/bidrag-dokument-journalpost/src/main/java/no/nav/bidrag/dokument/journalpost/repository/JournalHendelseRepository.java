package no.nav.bidrag.dokument.journalpost.repository;

import java.util.List;
import no.nav.bidrag.dokument.journalpost.entity.JournalHendelse;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JournalHendelseRepository extends CrudRepository<JournalHendelse, Integer> {
  List<JournalHendelse> findByJournalpostId(Integer journalpostId);
}
