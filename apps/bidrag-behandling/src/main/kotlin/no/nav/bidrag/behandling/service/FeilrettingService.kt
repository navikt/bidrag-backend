package no.nav.bidrag.behandling.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.behandling.database.datamodell.Behandling
import no.nav.bidrag.behandling.database.datamodell.Grunnlag
import no.nav.bidrag.behandling.database.datamodell.Rolle
import no.nav.bidrag.behandling.database.datamodell.konvertereData
import no.nav.bidrag.behandling.database.grunnlag.SkattepliktigeInntekter
import no.nav.bidrag.behandling.dto.v2.behandling.Grunnlagsdatatype
import no.nav.bidrag.behandling.dto.v2.behandling.getOrMigrate
import no.nav.bidrag.behandling.transformers.grunnlag.summertAinntektstyper
import no.nav.bidrag.behandling.transformers.grunnlag.summertSkattegrunnlagstyper
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.diverse.Kilde
import no.nav.bidrag.domene.enums.rolle.Rolletype
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

private val grunnlagstyperAvledetFraSkattepliktigeInntekter =
    setOf(Grunnlagsdatatype.SKATTEPLIKTIGE_INNTEKTER, Grunnlagsdatatype.SUMMERTE_MÅNEDSINNTEKTER)
private val inntektstyperAvledetFraSkattepliktigeInntekter = summertAinntektstyper + summertSkattegrunnlagstyper

@Service
class FeilrettingService {
    /**
     * Sletter skattepliktige inntekter som er lagret på en bidragsmottaker, men som er rapportert for en annen person.
     * Rådata, bearbeidede grunnlag og offentlige inntekter som er avledet fra skattepliktige inntekter slettes for rollen,
     * og grunnlagSistInnhentet nullstilles slik at grunnlaget hentes på nytt.
     *
     * @return id til bidragsmottakerne som ble rettet
     */
    fun slettSkattepliktigeInntekterHentetForAnnenPerson(behandling: Behandling): List<Long> {
        if (behandling.erVedtakFattet) return emptyList()

        val nyesteIdentCache = mutableMapOf<String, String>()
        val identerUtenOppslag = mutableSetOf<String>()
        val hentNyesteIdent: (String) -> String? = { ident ->
            when {
                ident in nyesteIdentCache -> nyesteIdentCache[ident]
                ident in identerUtenOppslag -> null
                else ->
                    hentPerson(ident)?.ident?.verdi.also {
                        if (it == null) {
                            identerUtenOppslag.add(ident)
                        } else {
                            nyesteIdentCache[ident] = it
                        }
                    }
            }
        }

        val rollerMedFeilGrunnlag =
            behandling.grunnlag
                .filter {
                    Rolletype.BIDRAGSMOTTAKER == it.rolle.rolletype &&
                        Grunnlagsdatatype.SKATTEPLIKTIGE_INNTEKTER == it.type.getOrMigrate() &&
                        !it.erBearbeidet &&
                        it.inneholderInntekterForAnnenPerson(hentNyesteIdent)
                }.map { it.rolle }
                .distinct()

        rollerMedFeilGrunnlag.forEach { slettSkattepliktigeInntekterOgAvledetData(behandling, it) }
        if (rollerMedFeilGrunnlag.isNotEmpty()) {
            behandling.grunnlagSistInnhentet = null
        }
        return rollerMedFeilGrunnlag.mapNotNull { it.id }
    }

    private fun Grunnlag.inneholderInntekterForAnnenPerson(hentNyesteIdent: (String) -> String): Boolean {
        val rolleIdent = rolle.ident ?: return false
        val skattepliktigeInntekter = konvertereData<SkattepliktigeInntekter>() ?: return false
        val avvikendeIdenter =
            (skattepliktigeInntekter.ainntekter.map { it.personId } + skattepliktigeInntekter.skattegrunnlag.map { it.personId })
                .toSet() - rolleIdent
        if (avvikendeIdenter.isEmpty()) return false

        val nyesteRolleIdent = hentNyesteIdent(rolleIdent) ?: return false
        val nyesteAvvikendeIdenter = avvikendeIdenter.map { hentNyesteIdent(it) ?: return false }
        val identerForAnnenPerson = nyesteAvvikendeIdenter.filter { it != nyesteRolleIdent }
        if (identerForAnnenPerson.isNotEmpty()) {
            secureLogger.warn {
                "Grunnlag $id (${rolle.rolletype} ${rolle.id}) i behandling ${behandling.id} inneholder skattepliktige inntekter " +
                    "for $identerForAnnenPerson, men tilhører $rolleIdent"
            }
        }
        return identerForAnnenPerson.isNotEmpty()
    }

    private fun slettSkattepliktigeInntekterOgAvledetData(
        behandling: Behandling,
        rolle: Rolle,
    ) {
        val grunnlagSomSkalSlettes =
            behandling.grunnlag
                .filter {
                    it.rolle.erSammeRolle(rolle) &&
                        grunnlagstyperAvledetFraSkattepliktigeInntekter.contains(it.type.getOrMigrate())
                }.toSet()
        val inntekterSomSkalSlettes =
            behandling.inntekter
                .filter {
                    Kilde.OFFENTLIG == it.kilde &&
                        it.erSammeRolle(rolle) &&
                        inntektstyperAvledetFraSkattepliktigeInntekter.contains(it.type)
                }.toSet()

        log.warn {
            "Sletter ${grunnlagSomSkalSlettes.size} grunnlag og ${inntekterSomSkalSlettes.size} offentlige inntekter for " +
                "${rolle.rolletype} ${rolle.id} i behandling ${behandling.id} fordi skattepliktige inntekter " +
                "er hentet for en annen person. Grunnlaget hentes på nytt."
        }

        grunnlagSomSkalSlettes.forEach { grunnlag ->
            grunnlag.rolle.grunnlag.remove(grunnlag)
            grunnlag.gjelderBarnRolle?.grunnlagGjelderBarn?.remove(grunnlag)
        }
        behandling.grunnlag.removeAll(grunnlagSomSkalSlettes)

        inntekterSomSkalSlettes.forEach { it.inntektsposter.clear() }
        behandling.inntekter.removeAll(inntekterSomSkalSlettes)
    }
}
