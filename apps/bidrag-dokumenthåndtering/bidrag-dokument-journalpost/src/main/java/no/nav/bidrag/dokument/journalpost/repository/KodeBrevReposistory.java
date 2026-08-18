package no.nav.bidrag.dokument.journalpost.repository;

import java.util.Optional;
import no.nav.bidrag.dokument.journalpost.entity.KodeBrev;
import org.springframework.data.repository.Repository;

@org.springframework.stereotype.Repository
public interface KodeBrevReposistory extends Repository<KodeBrev, String> {

  Optional<KodeBrev> findByKode(String kode);
}
