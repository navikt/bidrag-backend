package no.nav.bidrag.bbm.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.bbm.model.konverterTilBisyskode
import no.nav.bidrag.bbm.model.konverterTilStønadstype
import no.nav.bidrag.bbm.model.søknadIkkeFunnet
import no.nav.bidrag.bbm.persistence.bbm.entity.PeriodeBidrag
import no.nav.bidrag.bbm.persistence.bbm.entity.Samvær
import no.nav.bidrag.bbm.persistence.bbm.repository.PeriodeBidragRepository
import no.nav.bidrag.bbm.persistence.bbm.repository.SamværRepository
import no.nav.bidrag.bbm.persistence.bisys.repository.SøknadRepository
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.felles.BidragBeregningRequestDto
import no.nav.bidrag.transport.behandling.beregning.felles.BidragBeregningResponsDto
import no.nav.bidrag.transport.felles.toYearMonth
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class BBMService(
    private val periodeBidragRepository: PeriodeBidragRepository,
    private val samværRepository: SamværRepository,
    private val søknadRepository: SøknadRepository,
) {
    fun hentAllePeriodeBidragOgSamværsklasseForSaksnummer(saksnummerListe: List<String>): BidragBeregningResponsDto {
        val beregninger =
            saksnummerListe.flatMap { saksnummer ->
                val samvær = samværRepository.finnAlleSamværForSaksnummer(saksnummer)
                val periodeBidrag =
                    periodeBidragRepository
                        .finnAlleBidragPeriodeForSaksnummer(
                            saksnummer,
                        )

                periodeBidrag.mapNotNull {
                    val samværMatch =
                        samvær.firstOrNull { samvær ->
                            samvær.datoSøknad?.equals(it.datoSøknad) == true &&
                                samvær.personidentBarn == it.personidentBarn &&
                                samvær.datoFom?.equals(it.datoFom) == true
                        }
                    Pair(
                        samværMatch,
                        it,
                    ).mapToRespons()
                }
            }

        return BidragBeregningResponsDto(beregninger)
    }

    fun hentSisteBidragOgSamvær(request: BidragBeregningRequestDto): BidragBeregningResponsDto {
        val beregninger =
            request.hentBeregningerFor.mapNotNull {
                val datoSøknad = it.finnDatoSøknad()

                val søknadstyper = it.stønadstype.konverterTilBisyskode()
                val samvær =
                    samværRepository
                        .finnSisteSamvær(it.saksnummer, it.personidentBarn.verdi, datoSøknad, søknadstyper)
                val periodeBidrag =
                    periodeBidragRepository
                        .finnSisteBidragPeriode(
                            it.saksnummer,
                            it.personidentBarn.verdi,
                            datoSøknad,
                            søknadstyper,
                        )
                Pair(samvær, periodeBidrag).mapToRespons()?.let { respons ->
                    secureLogger.info {
                        "Fant bidrag beregning for saksnummer ${it.saksnummer}, personidentBarn ${it.personidentBarn.verdi} og datoSøknad = $datoSøknad - $respons"
                    }
                    respons
                } ?: kotlin.run {
                    log.warn {
                        "Fant ikke bidrag beregning for saksnummer ${it.saksnummer} og datoSøknad = $datoSøknad"
                    }
                    secureLogger.warn {
                        "Fant ikke bidrag beregning for saksnummer ${it.saksnummer}, personidentBarn ${it.personidentBarn.verdi} og datoSøknad = $datoSøknad"
                    }
                    return@mapNotNull null
                }
            }

        return BidragBeregningResponsDto(beregninger)
    }

    fun BidragBeregningRequestDto.HentBidragBeregning.finnDatoSøknad() = datoSøknad ?: run {
        val søknadMottattDato = søknadRepository.finnSøknad(søknadsid!!.toLong())?.søknadMottattDato ?: søknadIkkeFunnet(søknadsid)
        log.info {
            "Fant søknad mottatt dato $søknadMottattDato for søknadsid $søknadsid"
        }
        søknadMottattDato
    }

    fun Pair<Samvær?, PeriodeBidrag?>.mapToRespons(): BidragBeregningResponsDto.BidragBeregning? {
        val (samvær, periodeBidrag) = this
        if (periodeBidrag == null) {
            return null
        }
        if (samvær == null) {
            secureLogger.warn {
                "Fant ikke samvær for saksnummer ${periodeBidrag.saksnummer}, personidentBarn ${periodeBidrag.personidentBarn} og datoSøknad = ${periodeBidrag.datoSøknad}"
            }
            log.warn {
                "Fant ikke samvær for saksnummer ${periodeBidrag.saksnummer} og datoSøknad = ${periodeBidrag.datoSøknad}"
            }
        }
        return BidragBeregningResponsDto.BidragBeregning(
            periode = ÅrMånedsperiode(periodeBidrag.datoFom!!, null),
            saksnummer = periodeBidrag.saksnummer!!,
            personidentBarn = Personident(periodeBidrag.personidentBarn!!),
            datoSøknad = periodeBidrag.datoSøknad!!,
            beregnetBeløp = periodeBidrag.beregnetBeløp!!,
            faktiskBeløp = periodeBidrag.faktiskBeløp!!,
            beløpSamvær = periodeBidrag.beløpSamvær!!,
            stønadstype = konverterTilStønadstype(periodeBidrag.soknadstype!!)!!,
            samværsklasse = samvær?.let { Samværsklasse.fromBisysKode(samvær.samværskode!!) },
        )
    }

    fun hentAlleBeregningerOgSamværForVedtak(request: BidragBeregningRequestDto): BidragBeregningResponsDto {
        val bidragBeregningListe = mutableListOf<BidragBeregningResponsDto.BidragBeregning>()

        request.hentBeregningerFor.map {
            val datoSøknad = it.finnDatoSøknad()

            val søknadstyper = it.stønadstype.konverterTilBisyskode()

            val samværsklasseListe =
                samværRepository
                    .finnAlleSamvær(it.saksnummer, it.personidentBarn.verdi, datoSøknad, søknadstyper)
            secureLogger.info {
                "Fant samværsklasser for saksnummer ${it.saksnummer}, personidentBarn ${it.personidentBarn.verdi} " +
                    "og datoSøknad = $datoSøknad: $samværsklasseListe"
            }

            val periodeBidragListe =
                periodeBidragRepository
                    .finnAlleBidragPeriode(
                        it.saksnummer,
                        it.personidentBarn.verdi,
                        datoSøknad,
                        søknadstyper,
                    )
            secureLogger.info {
                "Fant periode bidrag for saksnummer ${it.saksnummer}, personidentBarn ${it.personidentBarn.verdi} " +
                    "og datoSøknad = $datoSøknad: $periodeBidragListe"
            }

            // Bygger opp en liste med perioder fra periodeBidragListe siden den ikke inneholder datoTom. Listen brukes deretter til å finne
            // matchende samværsklasse siden samværklasser inneholder andre perioder enn periodeBidragListe.
            val periodeliste =
                periodeBidragListe.zipWithNext { current, next ->
                    ÅrMånedsperiode(current.datoFom!!, next.datoFom)
                } + periodeBidragListe.lastOrNull()?.let { listOf(ÅrMånedsperiode(it.datoFom!!, null)) }.orEmpty()

            periodeBidragListe.forEach { periodeBidrag ->
                val periode = periodeliste.find { it.fom.equals(periodeBidrag.datoFom?.toYearMonth()) }
                val samværsklasse =
                    samværsklasseListe
                        .firstOrNull { samvær ->
                            ÅrMånedsperiode(samvær.datoFom!!, samvær.datoTom).overlapper(periode!!)
                        }?.samværskode
                        ?.let { Samværsklasse.fromBisysKode(it) }

                bidragBeregningListe.add(
                    BidragBeregningResponsDto.BidragBeregning(
                        periode = periode,
                        saksnummer = periodeBidrag.saksnummer!!,
                        personidentBarn = Personident(periodeBidrag.personidentBarn!!),
                        datoSøknad = periodeBidrag.datoSøknad!!,
                        beregnetBeløp = periodeBidrag.beregnetBeløp!!,
                        faktiskBeløp = periodeBidrag.faktiskBeløp!!,
                        beløpSamvær = periodeBidrag.beløpSamvær!!,
                        stønadstype = konverterTilStønadstype(periodeBidrag.soknadstype!!)!!,
                        samværsklasse = samværsklasse,
                    ),
                )
            }
        }

        return BidragBeregningResponsDto(bidragBeregningListe)
    }
}
