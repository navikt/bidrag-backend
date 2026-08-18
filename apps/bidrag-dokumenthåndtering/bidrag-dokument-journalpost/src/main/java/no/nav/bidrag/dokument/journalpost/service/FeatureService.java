package no.nav.bidrag.dokument.journalpost.service;

import java.util.Locale;
import java.util.stream.Collectors;
import no.nav.bidrag.dokument.journalpost.configuration.BidragDokumentJournalpostConfig.SaksbehandlerOidcTokenManager;
import no.nav.bidrag.dokument.journalpost.entity.FeatureAccess;
import no.nav.bidrag.dokument.journalpost.repository.FeatureAccessRepository;
import org.springframework.stereotype.Service;

@Service
public class FeatureService {

  public static final String FEATURE_DISTRIBUTE_DOCUMENT_BUTTON = "DISTRIBUTE_DOCUMENT_BUTTON";

  private final FeatureAccessRepository featureAccessRepository;
  private final SaksbehandlerOidcTokenManager saksbehandlerOidcTokenManager;

  public FeatureService(FeatureAccessRepository featureAccessRepository, SaksbehandlerOidcTokenManager saksbehandlerOidcTokenManager) {
    this.featureAccessRepository = featureAccessRepository;
    this.saksbehandlerOidcTokenManager = saksbehandlerOidcTokenManager;
  }

  public boolean kanDistribuereJournalpost(String enhetsnr){
    var featureAccessList = featureAccessRepository.findByFeatureName(FEATURE_DISTRIBUTE_DOCUMENT_BUTTON);
    if (featureAccessList.isEmpty()){
      return true;
    }
    var saksbehandler = saksbehandlerOidcTokenManager.hentSaksbehandler();
    if (isSaksbehandlerServiceBruker(saksbehandler)){
      return true;
    }

    var filteredFeatureAccessList =  featureAccessList.stream().filter(FeatureAccess::featureEnabled).collect(Collectors.toList());

    return filteredFeatureAccessList.isEmpty() || filteredFeatureAccessList.stream().anyMatch(featureAccess -> featureAccess.isEnabled(saksbehandler, enhetsnr));
  }

  private boolean isSaksbehandlerServiceBruker(String saksbehandler){
    return saksbehandler != null && saksbehandler.toLowerCase(Locale.ROOT).startsWith("srv");
  }
}
