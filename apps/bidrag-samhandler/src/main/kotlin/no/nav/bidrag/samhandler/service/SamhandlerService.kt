package no.nav.bidrag.samhandler.service

import jakarta.persistence.EntityManager
import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.ident.Ident
import no.nav.bidrag.domene.land.Landkode3
import no.nav.bidrag.samhandler.mapper.SamhandlerMapper
import no.nav.bidrag.samhandler.persistence.repository.SamhandlerRepository
import no.nav.bidrag.samhandler.persistence.repository.SamhandlerSøkSpec
import no.nav.bidrag.samhandler.util.ConflictException
import no.nav.bidrag.samhandler.util.DuplikatSamhandlerMap
import no.nav.bidrag.samhandler.util.KontonummerUtils
import no.nav.bidrag.samhandler.util.add
import no.nav.bidrag.samhandler.util.getPath
import no.nav.bidrag.samhandler.util.kontonummerNumerisk
import no.nav.bidrag.samhandler.util.nullIfEmpty
import no.nav.bidrag.transport.dokument.isNumeric
import no.nav.bidrag.transport.felles.commonObjectmapper
import no.nav.bidrag.transport.samhandler.AdresseDto
import no.nav.bidrag.transport.samhandler.DuplikatSamhandler
import no.nav.bidrag.transport.samhandler.FeltValideringsfeil
import no.nav.bidrag.transport.samhandler.KontonummerDto
import no.nav.bidrag.transport.samhandler.SamhandlerDto
import no.nav.bidrag.transport.samhandler.SamhandlerKafkaHendelsestype
import no.nav.bidrag.transport.samhandler.SamhandlerSøk
import no.nav.bidrag.transport.samhandler.SamhandlerValideringsfeil
import no.nav.bidrag.transport.samhandler.SamhandlersøkeresultatDto
import no.nav.bidrag.transport.samhandler.leggTil
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException
import java.nio.charset.Charset
import kotlin.jvm.optionals.getOrNull

@Service
class SamhandlerService(
    private val samhandlerRepository: SamhandlerRepository,
    private val kafkaService: KafkaService,
    private val entityManager: EntityManager,
) {
    @Transactional
    fun hentSamhandler(
        ident: Ident,
        inkluderAuditLog: Boolean,
    ): SamhandlerDto? {
        samhandlerRepository.findByIdent(ident.verdi)?.let { samhandler ->
            return SamhandlerMapper.mapTilSamhandlerDto(samhandler, inkluderAuditLog)
        }
        return null
    }

    fun samhandlerSøk(samhandlerSøk: SamhandlerSøk): SamhandlersøkeresultatDto {
        secureLogger.info {
            "${"Samhandlersøk utført av {}. Input: {}"} ${TokenUtils.hentSaksbehandlerIdent() ?: TokenUtils.hentApplikasjonsnavn() ?: "ukjent"} $samhandlerSøk"
        }
        val samhandlere = samhandlerRepository.findAll(SamhandlerSøkSpec.søkPåAlleParameter(samhandlerSøk))
        secureLogger.info { "${"Samhandlersøk returnerte følgende samhandlere: {}"} ${samhandlere.map { it.ident }}" }
        return SamhandlerMapper.mapTilSamhandlersøkeresultatDto(samhandlere)
    }

    @Transactional
    fun opprettSamhandler(samhandlerDto: SamhandlerDto): Int {
        validerSamhandlerIkkeFinnesFraFør(samhandlerDto)
        val samhandler = SamhandlerMapper.mapTilSamhandler(samhandlerDto)
        secureLogger.info {
            "${"OpprettSamhandler for {} utført av {} med følgende data: {}"} ${
                arrayOf<String?>(
                    samhandlerDto.navn,
                    TokenUtils.hentSaksbehandlerIdent() ?: TokenUtils.hentApplikasjonsnavn() ?: "ukjent",
                    samhandlerDto.toString(),
                )
            }"
        }
        try {
            val opprettetSamhandler = samhandlerRepository.save(samhandler)
            entityManager.refresh(opprettetSamhandler)
            kafkaService.sendSamhandlerMelding(opprettetSamhandler, SamhandlerKafkaHendelsestype.OPPRETTET)

            return opprettetSamhandler.id!!
        } catch (e: DataIntegrityViolationException) {
            behandleDataIntegrityException(e, samhandlerDto)
        }
    }

    private fun validerSamhandlerIkkeFinnesFraFør(samhandlerDto: SamhandlerDto) {
        if (samhandlerDto.offentligId == null) return
        val samhandlerMedSammeOffentligId =
            samhandlerRepository.findAllByOffentligIdAndErOpphørtIsFalse(samhandlerDto.offentligId!!)
        if (samhandlerMedSammeOffentligId.isEmpty()) return
        val identtiskeSamhandlere: DuplikatSamhandlerMap = mutableMapOf()
        samhandlerMedSammeOffentligId.filter { it.ident != samhandlerDto.samhandlerId?.verdi }.forEach {
            if (it.norskkontonr.kontonummerNumerisk != null &&
                it.norskkontonr.kontonummerNumerisk == samhandlerDto.kontonummer?.norskKontonummer.kontonummerNumerisk
            ) {
                identtiskeSamhandlere.add(it.ident!!, "kontonummer.norskKontonummer")
            }
            if (it.iban != null && it.iban == samhandlerDto.kontonummer?.iban.nullIfEmpty()) {
                identtiskeSamhandlere.add(it.ident!!, "kontonummer.iban")
            }
        }

        if (identtiskeSamhandlere.isNotEmpty()) {
            throw ConflictException(
                "Feil ved " +
                    (samhandlerDto.samhandlerId?.let { "oppdatering" } ?: "opprettelse") +
                    " av samhandler: Samhandler med samme offentlig ID og kontonummer finnes fra før.",
                SamhandlerValideringsfeil(
                    identtiskeSamhandlere.entries.map { (samhandlerId, felter) ->
                        DuplikatSamhandler(
                            "Samhandler med samme offentlig ID og kontonummer finnes fra før",
                            samhandlerId,
                            felter,
                        )
                    },
                ),
            )
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun hentSamhandlerPåId(samhandlerId: Int) = samhandlerRepository.findById(samhandlerId).getOrNull()?.let {
        SamhandlerMapper.mapTilSamhandlerDto(it)
    }

    @Transactional
    fun oppdaterSamhandler(samhandlerDto: SamhandlerDto): ResponseEntity<*> {
        validerSamhandlerIkkeFinnesFraFør(samhandlerDto)
        val samhandlerIdent =
            samhandlerDto.samhandlerId?.verdi
                ?: throw HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "Oppdatering av samhandler må ha angitt samhandlerId!",
                    commonObjectmapper.writeValueAsBytes(
                        SamhandlerValideringsfeil(
                            ugyldigInput =
                            listOf(
                                FeltValideringsfeil(
                                    SamhandlerDto::samhandlerId.name,
                                    "SamhandlerId må angis ved oppdatering av samhandler",
                                ),
                            ),
                        ),
                    ),
                    Charset.defaultCharset(),
                )
        val samhandler =
            samhandlerRepository.findByIdent(samhandlerIdent) ?: return ResponseEntity.notFound().build<Any>()

        val oppdatertSamhandler =
            samhandler.copy(
                navn =
                samhandlerDto.navn,
                offentligId = samhandlerDto.offentligId,
                offentligIdType = samhandlerDto.offentligIdType,
                områdekode = samhandlerDto.områdekode,
                språk = samhandlerDto.språk,
                norskkontonr = samhandlerDto.kontonummer?.norskKontonummer.nullIfEmpty(),
                iban = samhandlerDto.kontonummer?.iban.nullIfEmpty(),
                swift = samhandlerDto.kontonummer?.swift.nullIfEmpty(),
                banknavn = samhandlerDto.kontonummer?.banknavn.nullIfEmpty(),
                banklandkode =
                samhandlerDto.kontonummer
                    ?.landkodeBank
                    ?.verdi
                    .nullIfEmpty(),
                valutakode = samhandlerDto.kontonummer?.valutakode,
                bankcode = samhandlerDto.kontonummer?.bankCode.nullIfEmpty(),
                adresselinje1 = samhandlerDto.adresse?.adresselinje1.nullIfEmpty(),
                adresselinje2 = samhandlerDto.adresse?.adresselinje2.nullIfEmpty(),
                adresselinje3 = samhandlerDto.adresse?.adresselinje3.nullIfEmpty(),
                postnummer = samhandlerDto.adresse?.postnummer.nullIfEmpty(),
                poststed = samhandlerDto.adresse?.poststed.nullIfEmpty(),
                land =
                samhandlerDto.adresse
                    ?.land
                    ?.verdi
                    .nullIfEmpty(),
                kontaktperson = samhandlerDto.kontaktperson.nullIfEmpty(),
                kontaktEpost = samhandlerDto.kontaktEpost.nullIfEmpty(),
                kontaktTelefon = samhandlerDto.kontaktTelefon.nullIfEmpty(),
                notat = samhandlerDto.notat.nullIfEmpty(),
                erOpphørt = samhandlerDto.erOpphørt ?: false,
            )

        val lagretSamhandler = samhandlerRepository.save(oppdatertSamhandler)
        kafkaService.sendSamhandlerMelding(lagretSamhandler, SamhandlerKafkaHendelsestype.OPPDATERT)

        secureLogger.info {
            "${"OppdaterSamhandler for {} utført av {} fra data: {}"} ${
                arrayOf(
                    samhandlerDto.samhandlerId,
                    TokenUtils.hentSaksbehandlerIdent() ?: TokenUtils.hentApplikasjonsnavn() ?: "ukjent",
                    samhandlerDto.toString(),
                )
            }"
        }

        return ResponseEntity.ok(SamhandlerMapper.mapTilSamhandlerDto(oppdatertSamhandler))
    }

    fun validerInput(
        samhandlerDto: SamhandlerDto,
        opprettSamhandler: Boolean = false,
    ) {
        if (samhandlerDto.adresse?.postnummer == null && samhandlerDto.adresse?.postnr != null) {
            throw IllegalArgumentException("Postnummer må angis som postnummer, ikke postnr. Postnr er avviklet.")
        }

        val valideringsfeil: MutableList<FeltValideringsfeil> = mutableListOf()
        if (samhandlerDto.adresse != null) {
            validerAdresse(samhandlerDto.adresse!!, valideringsfeil)
        }
        if (samhandlerDto.kontonummer != null) {
            validerKontonummer(samhandlerDto.kontonummer!!, valideringsfeil)
        }
        if (samhandlerDto.språk == null) {
            valideringsfeil.leggTil(
                getPath(SamhandlerDto::språk),
                "Språk må angis.",
            )
        }
        val samhandlerIdInput = samhandlerDto.samhandlerId
        if (opprettSamhandler && samhandlerIdInput != null && samhandlerIdInput.verdi.trim().isNotEmpty()) {
            valideringsfeil.leggTil(
                getPath(SamhandlerDto::samhandlerId),
                "Kan ikke sette samhandlerId ved opprettelse av samhandler.",
            )
        }
        if (valideringsfeil.isNotEmpty()) {
            throw HttpClientErrorException(
                HttpStatus.BAD_REQUEST,
                "Feil ved ${if (opprettSamhandler) "opprettelse" else "oppdatering"} av samhandler ${samhandlerDto.samhandlerId ?: ""}",
                commonObjectmapper.writeValueAsBytes(
                    SamhandlerValideringsfeil(
                        ugyldigInput = valideringsfeil,
                    ),
                ),
                Charset.defaultCharset(),
            )
        }
    }

    private fun validerKontonummer(
        kontonummer: KontonummerDto,
        valideringsfeil: MutableList<FeltValideringsfeil>,
    ) {
        val norskKontonummerNumerisk =
            kontonummer.norskKontonummer.kontonummerNumerisk
        if (norskKontonummerNumerisk != null) {
            if (norskKontonummerNumerisk.length != 11) {
                valideringsfeil.leggTil(
                    getPath(SamhandlerDto::kontonummer, KontonummerDto::norskKontonummer),
                    "Norsk kontonummer må være 11 tegn langt.",
                )
            }
            if (!KontonummerUtils.erGyldigKontonummerMod11(norskKontonummerNumerisk)) {
                valideringsfeil.leggTil(
                    getPath(SamhandlerDto::kontonummer, KontonummerDto::norskKontonummer),
                    "Det er angitt et ugyldig norsk kontonummer.",
                )
            }
        }
        if (kontonummer.landkodeBank != null && kontonummer.landkodeBank?.gyldig() == false) {
            valideringsfeil.leggTil(
                getPath(SamhandlerDto::kontonummer, KontonummerDto::landkodeBank),
                "Landkode for bank ${kontonummer.landkodeBank?.verdi} må ha 3 tegn.",
            )
        }
    }

    private fun validerAdresse(
        adresse: AdresseDto,
        valideringsfeil: MutableList<FeltValideringsfeil>,
    ) {
        if (adresse.adresselinje1.isNullOrBlank() &&
            (
                adresse.adresselinje2?.isNotEmpty() == true ||
                    adresse.adresselinje3?.isNotEmpty() == true
                )
        ) {
            valideringsfeil.leggTil(
                getPath(SamhandlerDto::adresse, AdresseDto::adresselinje1),
                "Adresselinje1 må fylles ut før adresselinje 2 eller adresselinje 3.",
            )
        }
        if (adresse.adresselinje1?.isNotEmpty() == true) {
            if (adresse.land?.verdi.isNullOrBlank()) {
                valideringsfeil.leggTil(
                    getPath(SamhandlerDto::adresse, AdresseDto::land),
                    "Landkode med 3 tegn må angis for adresse.",
                )
            }
            if (adresse.land?.gyldig() == false) {
                valideringsfeil.leggTil(
                    getPath(SamhandlerDto::adresse, AdresseDto::land),
                    "Landkode ${adresse.land?.verdi} må ha 3 tegn, og kan ikke være blank.",
                )
            }
            if (adresse.land?.equals(Landkode3("NOR")) == true &&
                (adresse.postnummer.isNullOrBlank() || adresse.poststed.isNullOrBlank())
            ) {
                valideringsfeil.leggTil(
                    getPath(SamhandlerDto::adresse, AdresseDto::postnummer),
                    "Postnummer og poststed må angis for norske adresser.",
                )
                valideringsfeil.leggTil(
                    getPath(SamhandlerDto::adresse, AdresseDto::poststed),
                    "Postnummer og poststed må angis for norske adresser.",
                )
            }
            if (!adresse.postnummer.isNullOrEmpty() && !adresse.postnummer!!.isNumeric) {
                valideringsfeil.leggTil(
                    getPath(SamhandlerDto::adresse, AdresseDto::postnummer),
                    "Postnummer kan kun inneholde siffer.",
                )
            }
        }
    }

    fun behandleDataIntegrityException(
        e: DataIntegrityViolationException,
        request: SamhandlerDto,
    ): Nothing {
        if (e.cause is ConstraintViolationException) {
            val samhandlerId = request.samhandlerId
            val psqlException = (e.cause as ConstraintViolationException).sqlException
            // 23505 betyr unique violation i postgres
            if (samhandlerId != null && psqlException.sqlState == "23505") {
                val samhandler = samhandlerRepository.findByIdentInNewTransaction(samhandlerId.verdi)

                if (samhandler != null) {
                    secureLogger.error(e) {
                        "Feil ved lagring av samhandler. Det finnes allerede et samhandler med samhandlerId ${request.samhandlerId}. Request: $request"
                    }
                    throw ConflictException(
                        "Et samhandler med angitt samhandlerId finnes allerede",
                        SamhandlerValideringsfeil(
                            duplikatSamhandler =
                            listOf(
                                DuplikatSamhandler(
                                    "Samhandler med samme samhandlerId finnes fra før",
                                    samhandlerId.verdi,
                                    listOf("samhandlerId"),
                                ),
                            ),
                        ),
                    )
                }
            }
        }
        secureLogger.error(e) { "Uventet feil ved lagring av samhandler: ${e.message}" }
        throw e
    }
}
