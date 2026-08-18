package no.nav.bidrag.dokument.journalpost.repository;

import no.nav.bidrag.dokument.journalpost.entity.KodeJournalstatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KodeJournalstatusRepository extends CrudRepository<KodeJournalstatus, String> {

}
