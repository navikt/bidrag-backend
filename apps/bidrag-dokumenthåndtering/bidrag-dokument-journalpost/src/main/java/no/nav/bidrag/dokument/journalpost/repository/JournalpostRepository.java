package no.nav.bidrag.dokument.journalpost.repository;

import java.util.List;
import java.util.Optional;
import no.nav.bidrag.dokument.journalpost.entity.Journalpost;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JournalpostRepository extends CrudRepository<Journalpost, Integer> {
  Optional<Journalpost> findByDokumentreferanse(String dokref);

  @Query("select j from Journalpost j where j.journalstatus = 'D' and j.dokStatusSjekket is null and j.rensetDokRef is null order by j.journalpostId desc offset :pageStart rows fetch next :pageSize rows only")
  List<Journalpost> hentJournalposterMedStatusUnderProduksjon(Integer pageStart, Integer pageSize);

  @Query("select j from Journalpost j where j.dokStatusSjekket is not null order by j.journalpostId desc offset :pageStart rows fetch next :pageSize rows only")
  List<Journalpost> hentJournalposterMarkertSjekket(Integer pageStart, Integer pageSize);


}
