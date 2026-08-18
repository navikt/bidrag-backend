package no.nav.bidrag.dokument.journalpost.controller;

import static java.util.stream.Collectors.toList;
import static no.nav.bidrag.commons.util.KildesystemIdenfikator.PREFIX_BIDRAG_COMPLETE;
import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpost.SECURE_LOGGER;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import java.util.Optional;
import no.nav.bidrag.commons.util.KildesystemIdenfikator;
import no.nav.bidrag.commons.web.EnhetFilter;
import no.nav.bidrag.commons.web.WebUtil;
import no.nav.bidrag.transport.dokument.AvvikType;
import no.nav.bidrag.transport.dokument.Avvikshendelse;
import no.nav.bidrag.transport.dokument.BehandleAvvikshendelseResponse;
import no.nav.bidrag.transport.dokument.DistribuerJournalpostResponse;
import no.nav.bidrag.transport.dokument.EndreJournalpostCommand;
import no.nav.bidrag.transport.dokument.JournalpostDto;
import no.nav.bidrag.transport.dokument.JournalpostResponse;
import no.nav.bidrag.transport.dokument.OpprettJournalpostRequest;
import no.nav.bidrag.transport.dokument.OpprettJournalpostResponse;
import no.nav.bidrag.dokument.journalpost.dto.EndreJournalpostCommandIntern;
import no.nav.bidrag.dokument.journalpost.dto.JournalpostIntern;
import no.nav.bidrag.dokument.journalpost.dto.JournalpostResponseIntern;
import no.nav.bidrag.dokument.journalpost.dto.Sakjournal;
import no.nav.bidrag.dokument.journalpost.exception.JournalpostIkkeFunnetException;
import no.nav.bidrag.dokument.journalpost.model.Avvikstype;
import no.nav.bidrag.dokument.journalpost.model.BehandleAvvikRequest;
import no.nav.bidrag.dokument.journalpost.model.BehandleAvvikResponse;
import no.nav.bidrag.dokument.journalpost.model.FinnAvvik;
import no.nav.bidrag.dokument.journalpost.model.StatusAvviksbehandling;
import no.nav.bidrag.dokument.journalpost.service.AvvikService;
import no.nav.bidrag.dokument.journalpost.service.DistribuerService;
import no.nav.bidrag.dokument.journalpost.service.JournalpostService;
import no.nav.bidrag.dokument.journalpost.service.OpprettJournalpostService;
import no.nav.bidrag.dokument.journalpost.service.TilgangskontrollService;
import no.nav.security.token.support.core.api.Protected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Protected
public class JournalpostController {

  private static final Logger LOGGER = LoggerFactory.getLogger(JournalpostController.class);

  public static final String ROOT_SAK = "/sak";
  public static final String ROOT_JOURNAL = "/journal";

  private final DistribuerService distribuerService;
  private final JournalpostService journalpostService;
  private final TilgangskontrollService tilgangskontrollService;
  private final OpprettJournalpostService opprettJournalpostService;
  private final AvvikService avvikService;

  public JournalpostController(DistribuerService distribuerService, JournalpostService journalpostService, TilgangskontrollService tilgangskontrollService,
      OpprettJournalpostService opprettJournalpostService, AvvikService avvikService) {
    this.distribuerService = distribuerService;
    this.journalpostService = journalpostService;
    this.tilgangskontrollService = tilgangskontrollService;
    this.opprettJournalpostService = opprettJournalpostService;
    this.avvikService = avvikService;
  }

  @PostMapping("/journalpost")
  @Operation(
      security = {@SecurityRequirement(name = "bearer-key")},
      description = "Opprett notat eller utgående journalpost i midlertidlig brevlager"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "400", description = "Opprett journalpost kalt med ugyldig data"),
  })
  public ResponseEntity<OpprettJournalpostResponse> opprettJournalpost(@RequestBody OpprettJournalpostRequest opprettJournalpostRequest) {
    SECURE_LOGGER.info("Oppretter journalpost {}", opprettJournalpostRequest);

    return ResponseEntity.ok(opprettJournalpostService.opprettJournalpost(opprettJournalpostRequest));
  }


  @GetMapping(ROOT_SAK + "/{saksnummer}/journal")
  @Operation(
      security = {@SecurityRequirement(name = "bearer-key")},
      summary = "Finn journalposter for et saksnummer og fagområde. Parameter fagomrade=BID er bidragjournal og fagomrade=FAR er farskapsjournal"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Fant journalposter for saksnummer"),
      @ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "403", description = "Saksbehandler har ikke tilgang til aktuell journalpost", content = @Content(schema = @Schema(hidden = true)))
  })
  public ResponseEntity<List<JournalpostDto>> hentJournal(
      @PathVariable String saksnummer, @RequestParam List<String> fagomrade, @RequestParam(required = false) Boolean medFeilforte
  ) {
    LOGGER.info("Henter journal for sak {} med fagområder {} og medFeilforte {}", saksnummer, fagomrade, medFeilforte);

    // Kaster exception med HttpStatus.FORBIDDEN (403) hvis tilgangskontroll feiler. 204 hvis sak mangler
    tilgangskontrollService.sjekkTilgangSak(saksnummer);

    var sakjournal = new Sakjournal(saksnummer, fagomrade, medFeilforte);

    List<JournalpostDto> journalposter = journalpostService.hentJournal(sakjournal).stream()
        .map(JournalpostIntern::tilJournalpostDto)
        .collect(toList());

    return new ResponseEntity<>(journalposter, HttpStatus.OK);
  }

  @GetMapping(ROOT_JOURNAL + "/{bidJournalpostId}")
  @Operation(
      security = {@SecurityRequirement(name = "bearer-key")},
      summary = "Hent en journalpost for en id på formatet '" + PREFIX_BIDRAG_COMPLETE + "<journalpostId> og valgfritt saksnummer'"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Journalpost er hentet"),
      @ApiResponse(responseCode = "400", description = "Ukjent/ugyldig journalpostId som har/mangler prefix", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "403", description = "Saksbehandler har ikke tilgang til aktuell journalpost", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "404", description = "Journalposten som skal hentes eksisterer ikke eller det er feil prefix/id på journalposten", content = @Content(schema = @Schema(hidden = true)))
  })
  public ResponseEntity<JournalpostResponse> hentJournalpost(
      @PathVariable String bidJournalpostId,
      @Parameter(name = "saksnummer", description = "journalposten tilhører sak") @RequestParam(required = false) String saksnummer
  ) {
    var muligSaksnummer = Optional.ofNullable(saksnummer);
    var kildesystemIdenfikator = new KildesystemIdenfikator(bidJournalpostId);

    if (kildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix()) {
      return new ResponseEntity<>(WebUtil.INSTANCE.initHttpHeadersWith(HttpHeaders.WARNING, "Ugyldig prefix på journalpostId"), HttpStatus.BAD_REQUEST);
    }

    if (kildesystemIdenfikator.erKjentKildesystemMedIdMedIdSomOverstigerInteger()) {
      return new ResponseEntity<>(
          WebUtil.INSTANCE.initHttpHeadersWith(HttpHeaders.WARNING, "Tall i %s kan ikke parses til int".formatted(bidJournalpostId)), HttpStatus.NOT_FOUND
      );
    }

    JournalpostResponseIntern journalpostResponseIntern;

    if (muligSaksnummer.isPresent()) {
      LOGGER.info("Henter journalpost {} med saksnummer {}", bidJournalpostId, saksnummer);

      // Kaster exception med HttpStatus.FORBIDDEN (403) hvis tilgangskontroll feiler. 204 hvis sak mangler
      tilgangskontrollService.sjekkTilgangSak(saksnummer);
      journalpostResponseIntern = journalpostService.hentJournalpost(saksnummer, kildesystemIdenfikator.hentJournalpostId());
    } else {
      LOGGER.info("Henter journalpost {}", bidJournalpostId);
      journalpostResponseIntern = journalpostService.hentJournalpost(kildesystemIdenfikator.hentJournalpostId());

      // Kaster exception med HttpStatus.FORBIDDEN (403) hvis tilgangskontroll feiler. 204 hvis sak mangler
      journalpostResponseIntern.getJournalsaker().forEach(tilgangskontrollService::sjekkTilgangSak);

      if (journalpostResponseIntern.harIngenTilknyttedeSakerMenGjelderIdent()) {
        tilgangskontrollService.sjekkTilgangPerson(journalpostResponseIntern.hentGjelderAktorIdent());
      }
    }

    if (journalpostResponseIntern.harIkkeFunnetJournalpost()) {
      throw new JournalpostIkkeFunnetException("Fant ikke journalpost med id: " + kildesystemIdenfikator.getPrefiksetJournalpostId());
    }

    SECURE_LOGGER.info("Hentet journalpost med innhold: {}", journalpostResponseIntern.getJournalpost());

    return new ResponseEntity<>(journalpostResponseIntern.tilJournalpostResponse(), HttpStatus.OK);
  }

  @GetMapping(ROOT_JOURNAL + "/{bidJournalpostId}/avvik")
  @Operation(
      security = {@SecurityRequirement(name = "bearer-key")},
      summary = "Henter mulige avvik for en journalpost, id på formatet '" + PREFIX_BIDRAG_COMPLETE + "<journalpostId>'"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Tilgjengelig avvik for journalpost er hentet"),
      @ApiResponse(responseCode = "401", description = "Du mangler sikkerhetstoken", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "403", description = "Sikkerhetstoken er ikke gyldig", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "404", description = "Fant ikke journalpost som det skal hentes avvik på", content = @Content(schema = @Schema(hidden = true)))
  })
  public ResponseEntity<List<AvvikType>> hentAvvik(
      @PathVariable String bidJournalpostId,
      @Parameter(name = "saksnummer", description = "journalposten tilhører sak") @RequestParam(required = false) String saksnummer
  ) {
    var muligSak = Optional.ofNullable(saksnummer);

    if (muligSak.isPresent()) {
      LOGGER.info("Henter avvik for journalpost {} og saksnummer {}", bidJournalpostId, saksnummer);
    } else {
      LOGGER.info("Henter avvik for journalpost {}", bidJournalpostId);
    }

    var kildesystemIdenfikator = new KildesystemIdenfikator(bidJournalpostId);

    if (kildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix()) {
      return new ResponseEntity<>(WebUtil.INSTANCE.initHttpHeadersWith(HttpHeaders.WARNING, "Ugyldig prefix på journalpostId"), HttpStatus.BAD_REQUEST);
    }

    if (kildesystemIdenfikator.erKjentKildesystemMedIdMedIdSomOverstigerInteger()) {
      return new ResponseEntity<>(
          WebUtil.INSTANCE.initHttpHeadersWith(HttpHeaders.WARNING, "Tall i %s kan ikke parses til int".formatted(bidJournalpostId)), HttpStatus.NOT_FOUND
      );
    }

    FinnAvvik finnAvvik;

    if (saksnummer == null) {
      finnAvvik = avvikService.finnAvvik(kildesystemIdenfikator.hentJournalpostId());
    } else {
      finnAvvik = avvikService.finnAvvik(kildesystemIdenfikator.hentJournalpostId(), saksnummer);
    }

    var listeMedAvvikForJournalpost = finnAvvik.hentListeMedAvvik();

    LOGGER.info("Hentet avvik {} for journalpost {}", listeMedAvvikForJournalpost, bidJournalpostId);

    return new ResponseEntity<>(listeMedAvvikForJournalpost, HttpStatus.OK);
  }

  @PostMapping(value = ROOT_JOURNAL + "/{bidJournalpostId}/avvik", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      security = {@SecurityRequirement(name = "bearer-key")},
      summary = "Behandler et avvik for en journalpost, id på formatet '" + PREFIX_BIDRAG_COMPLETE + "<journalpostId>'"
  )
  @Transactional
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Avvik på journalpost er behandlet"),
      @ApiResponse(responseCode = "400", description = """
          En av følgende:
          - prefiks på journalpostId er ugyldig
          - avvikstypen mangler i avvikshendelsen
          - enhetsnummer i header (X_ENHET) mangler
          - ugyldig behandling av avvikshendelse (som bla. inkluderer):
            - oppretting av oppgave feiler
            - BESTILL_SPLITTING: beskrivelse må være i avvikshendelsen
            - OVERFOR_TIL_ANNEN_ENHET: nyttEnhetsnummer og gammeltEnhetsnummer må være i detaljer map
          """),
      @ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
      @ApiResponse(responseCode = "404", description = "Fant ikke journalpost som det skal lages avvik på eller feil prefix/id på journalposten"),
      @ApiResponse(responseCode = "503", description = "Oppretting av oppgave for avviket feilet")
  })
  public ResponseEntity<BehandleAvvikshendelseResponse> behandleAvvik(
      @PathVariable String bidJournalpostId,
      @RequestBody Avvikshendelse avvikshendelse,
      @RequestHeader(EnhetFilter.X_ENHET_HEADER) List<String> enheter
  ) {
    String enhet = !enheter.isEmpty() ? enheter.get(0) : null;
    LOGGER.info("Behandler avvik for journalpost {} med avvikType {}", bidJournalpostId, avvikshendelse.getAvvikType());
    SECURE_LOGGER.info("Behandler avvik for journalpost {} med avvikHendelse {}", bidJournalpostId, avvikshendelse);

    var kildesystemIdenfikator = new KildesystemIdenfikator(bidJournalpostId);

    if (kildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix()) {
      return new ResponseEntity<>(WebUtil.INSTANCE.initHttpHeadersWith(HttpHeaders.WARNING, "Ugyldig prefix på journalpostId"), HttpStatus.BAD_REQUEST);
    }

    if (kildesystemIdenfikator.erKjentKildesystemMedIdMedIdSomOverstigerInteger()) {
      return new ResponseEntity<>(
          WebUtil.INSTANCE.initHttpHeadersWith(HttpHeaders.WARNING, "Tall i %s kan ikke parses til int".formatted(bidJournalpostId)), HttpStatus.NOT_FOUND
      );
    }

    var muligAvvikstype = Optional.ofNullable(avvikshendelse.hent())
        .map(type -> Avvikstype.valueOf(type.name()));

    if (muligAvvikstype.isEmpty() || enhet == null || enhet.isBlank()) {
      var message = String.format(
          "Ugyldig avvik: avvikshendelse: %s, mulig avvik: %s, enhet: %s", avvikshendelse, muligAvvikstype, enhet
      );

      LOGGER.warn(message);

      return new ResponseEntity<>(WebUtil.INSTANCE.initHttpHeadersWith(HttpHeaders.WARNING, message), HttpStatus.BAD_REQUEST);
    }

    var behandleAvvikRequest = new BehandleAvvikRequest(avvikshendelse, enhet, kildesystemIdenfikator.hentJournalpostId());
    SECURE_LOGGER.info("Behandler avvik {}", behandleAvvikRequest);
    var behandleAvvikResponse = avvikService.behandleAvvik(behandleAvvikRequest);

    if (behandleAvvikResponse.erUgyldig()) {
      return behandletUgyldigAvvik(behandleAvvikResponse);
    }

    var opprettAvvikshendelseResponse = behandleAvvikResponse.tilBehandleAvvikshendelseResponse();

    return new ResponseEntity<>(opprettAvvikshendelseResponse, HttpStatus.OK);
  }

  private ResponseEntity<BehandleAvvikshendelseResponse> behandletUgyldigAvvik(BehandleAvvikResponse behandleAvvikResponse) {
    var message = "Kunne ikke opprette avvik: " + behandleAvvikResponse;
    LOGGER.warn(message);

    if (behandleAvvikResponse.erStatus(StatusAvviksbehandling.UGYLDIG)) {
      return new ResponseEntity<>(WebUtil.INSTANCE.initHttpHeadersWith(HttpHeaders.WARNING, message), HttpStatus.BAD_REQUEST);
    }

    var ugyldigBehandling = "Ugyldig behandling: ";

    if (behandleAvvikResponse.harUgyldigForklaring()) {
      ugyldigBehandling += behandleAvvikResponse.getUgyldigForklaring();
    } else {
      ugyldigBehandling += behandleAvvikResponse.getStatusAvviksbehandling();
    }

    return new ResponseEntity<>(WebUtil.INSTANCE.initHttpHeadersWith(HttpHeaders.WARNING, ugyldigBehandling), HttpStatus.BAD_REQUEST);
  }

  @PatchMapping(ROOT_JOURNAL + "/{bidJournalpostId}")
  @Transactional
  @Operation(
      security = {@SecurityRequirement(name = "bearer-key")},
      summary = "endre eksisterende journalpost med journalpostId på formatet '" + PREFIX_BIDRAG_COMPLETE + "<journalpostId>'"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Journalpost er endret (eller registrert/journalført når payload inkluderer \"skalJournalfores\":\"true\")"),
      @ApiResponse(responseCode = "400", description = """
          En av følgende:
          - prefiks på journalpostId er ugyldig
          - EndreJournalpostCommandDto.gjelder er ikke satt og det finnes dokumenter tilknyttet journalpost
          - enhet mangler/ugyldig (fra header)
          - journalpost skal journalføres, men har ikke sakstilknytninger
          """),
      @ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
      @ApiResponse(responseCode = "403", description = "Saksbehandler har ikke tilgang til aktuell journalpost/sak"),
      @ApiResponse(responseCode = "404", description = "Fant ikke journalpost som skal endres")
  })
  public ResponseEntity<Void> patchJournalpost(
      @RequestBody EndreJournalpostCommand endreJournalpostCommand,
      @PathVariable String bidJournalpostId,
      @RequestHeader(EnhetFilter.X_ENHET_HEADER) List<String> enheter
  ) {
    String enhet = !enheter.isEmpty() ? enheter.get(0) : null;
    LOGGER.info("Patch journalpost {} av enhet {}", bidJournalpostId, enhet);

    // Kaster exception med HttpStatus.FORBIDDEN (403) hvis tilgangskontroll feiler.
    endreJournalpostCommand.getTilknyttSaker().forEach(tilgangskontrollService::sjekkTilgangSak);

    var kildesystemIdenfikator = new KildesystemIdenfikator(bidJournalpostId);

    if (kildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix()) {
      var message = String.format("Id har ikke riktig prefix: %s", bidJournalpostId);
      LOGGER.warn(message);

      return new ResponseEntity<>(WebUtil.INSTANCE.initHttpHeadersWith(HttpHeaders.WARNING, message), HttpStatus.BAD_REQUEST);
    }

    if (kildesystemIdenfikator.erKjentKildesystemMedIdMedIdSomOverstigerInteger()) {
      return new ResponseEntity<>(
          WebUtil.INSTANCE.initHttpHeadersWith(HttpHeaders.WARNING, "Tall i %s kan ikke parses til int".formatted(bidJournalpostId)), HttpStatus.NOT_FOUND
      );
    }

    var journalpostId = kildesystemIdenfikator.hentJournalpostId(); // bruker id i fra path...
    var muligEndretJournalpost = journalpostService.endre(new EndreJournalpostCommandIntern(journalpostId, enhet, endreJournalpostCommand));

    muligEndretJournalpost.ifPresent(jp -> SECURE_LOGGER.info("Endret journalpost: {}", jp));

    return new ResponseEntity<>(muligEndretJournalpost.isPresent() ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
  }

  @PostMapping(ROOT_JOURNAL+"/distribuer/{bidJournalpostId}")
  @Operation(description = "Marker journalpost manuelt printet og sendt til mottaker")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Journalpost status er satt til EKSPEDERT"),
      @ApiResponse(responseCode = "400", description = "Journalpost har ugyldig status"),
      @ApiResponse(responseCode = "401", description = "Sikkerhetstoken er ikke gyldig"),
      @ApiResponse(responseCode = "403", description = "Sikkerhetstoke1n er ikke gyldig, eller det er ikke gitt adgang til kode 6 og 7 (nav-ansatt)"),
      @ApiResponse(responseCode = "404", description = "Fant ikke journalpost som skal distribueres")
  })
  @ResponseBody
  public ResponseEntity<DistribuerJournalpostResponse> distribuerJournalpost(@PathVariable String bidJournalpostId) {
    LOGGER.info("Marker journalpost {} sendt med lokal utksrift til mottaker", bidJournalpostId);
    KildesystemIdenfikator kildesystemIdenfikator = new KildesystemIdenfikator(bidJournalpostId);

    if (kildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix()) {
      var msgBadRequest = String.format("Id har ikke riktig prefix: %s", bidJournalpostId);

      LOGGER.warn(msgBadRequest);

      return ResponseEntity
          .badRequest()
          .header(HttpHeaders.WARNING, msgBadRequest)
          .build();
    }

    distribuerService.settStatusEkspedert(kildesystemIdenfikator.hentJournalpostId());
    return ResponseEntity.ok(new DistribuerJournalpostResponse(bidJournalpostId, null, null));
  }

  @GetMapping("/journal/distribuer/{journalpostId}/enabled")
  @Operation(description = "Sjekk om distribusjon av journalpost kan bestilles")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Distribusjon av journalpost kan bestilles"),
      @ApiResponse(responseCode = "406", description = "Distribusjon av journalpost kan ikke bestilles"),
      @ApiResponse(responseCode = "401", description = "Sikkerhetstoken er ikke gyldig"),
      @ApiResponse(responseCode = "403", description = "Sikkerhetstoke1n er ikke gyldig, eller det er ikke gitt adgang til kode 6 og 7 (nav-ansatt)"),
      @ApiResponse(responseCode = "404", description = "Fant ikke journalpost som skal distribueres")
  })
  @ResponseBody
  public ResponseEntity<Void> kanDistribuerJournalpost(
      @PathVariable String journalpostId,
      @RequestHeader(EnhetFilter.X_ENHET_HEADER) List<String> enheter
  ) {
    String enhet = !enheter.isEmpty() ? enheter.get(0) : null;
    LOGGER.info("Sjekker om journalpost {} for enhet {} kan distribueres", journalpostId, enhet);
    KildesystemIdenfikator kildesystemIdenfikator = new KildesystemIdenfikator(journalpostId);

    if (kildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix()) {
      var msgBadRequest = String.format("Id har ikke riktig prefix: %s", journalpostId);

      LOGGER.warn(msgBadRequest);

      return ResponseEntity
          .badRequest()
          .header(HttpHeaders.WARNING, msgBadRequest)
          .build();
    }

    try {
      distribuerService.kanDistribuereJournalpost(kildesystemIdenfikator.hentJournalpostId(), enhet);
      return ResponseEntity.ok().build();
    } catch (IllegalArgumentException e){
      LOGGER.warn("Ikke gyldig journalpost for distribusjon, begrunnelse {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
          .header(HttpHeaders.WARNING, e.getMessage())
          .build();
    }
  }
}
