package no.nav.bidrag.dokument.journalpost.repository;

import java.util.List;
import java.util.Optional;
import no.nav.bidrag.dokument.journalpost.entity.FeatureAccess;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeatureAccessRepository extends CrudRepository<FeatureAccess, Integer> {
  List<FeatureAccess> findByFeatureName(String featureName);
}
