package no.nav.bidrag.grunnlag.comparator

import no.nav.bidrag.grunnlag.SECURE_LOGGER
import no.nav.bidrag.grunnlag.bo.AinntektBo
import no.nav.bidrag.grunnlag.bo.AinntektspostBo
import no.nav.bidrag.grunnlag.util.toJsonString

class AinntektPeriodComparator : AbstractPeriodComparator<PeriodComparable<AinntektBo, AinntektspostBo>>() {
    override fun isEntitiesEqual(
        newEntity: PeriodComparable<AinntektBo, AinntektspostBo>,
        existingEntity: PeriodComparable<AinntektBo, AinntektspostBo>,
    ): Boolean {
        val newAinntektsposter = sortAinntektsposter(newEntity.children!!)
        val existingAinntektsposter = sortAinntektsposter(existingEntity.children!!)
        if (newAinntektsposter.size != existingAinntektsposter.size) {
            return false
        }
        val differences = mutableMapOf<String, String>()
        for (i in newAinntektsposter.indices) {
            differences.putAll(compareFields(newAinntektsposter[i].inntektType, existingAinntektsposter[i].inntektType, "inntektType"))
            differences.putAll(compareFields(newAinntektsposter[i].beskrivelse, existingAinntektsposter[i].beskrivelse, "beskrivelse"))
            differences.putAll(compareFields(newAinntektsposter[i].belop, existingAinntektsposter[i].belop, "belop"))
            differences.putAll(compareFields(newAinntektsposter[i].fordelType, existingAinntektsposter[i].fordelType, "fordelType"))
            differences.putAll(
                compareFields(newAinntektsposter[i].opplysningspliktigId, existingAinntektsposter[i].opplysningspliktigId, "opplysningspliktigId"),
            )
            differences.putAll(
                compareFields(newAinntektsposter[i].opptjeningsperiodeFra, existingAinntektsposter[i].opptjeningsperiodeFra, "opptjeningsperiodeFra"),
            )
            differences.putAll(
                compareFields(newAinntektsposter[i].opptjeningsperiodeTil, existingAinntektsposter[i].opptjeningsperiodeTil, "opptjeningsperiodeTil"),
            )
            differences.putAll(
                compareFields(newAinntektsposter[i].utbetalingsperiode, existingAinntektsposter[i].utbetalingsperiode, "utbetalingsperiode"),
            )
            differences.putAll(compareFields(newAinntektsposter[i].virksomhetId, existingAinntektsposter[i].virksomhetId, "virksomhetId"))
            differences.putAll(
                compareFields(
                    newAinntektsposter[i].etterbetalingsperiodeFra,
                    existingAinntektsposter[i].etterbetalingsperiodeFra,
                    "etterbetalingsperiodeFom",
                ),
            )
            differences.putAll(
                compareFields(
                    newAinntektsposter[i].etterbetalingsperiodeTil,
                    existingAinntektsposter[i].etterbetalingsperiodeTil,
                    "etterbetalingsperiodeTom",
                ),
            )
        }
        if (differences.isNotEmpty()) {
            SECURE_LOGGER.debug(toJsonString(differences))
        }
        return differences.isEmpty()
    }

    private fun sortAinntektsposter(ainntektsposter: List<AinntektspostBo>): List<AinntektspostBo> = ainntektsposter.sortedWith(
        compareBy(
            { it.utbetalingsperiode },
            { it.opptjeningsperiodeFra },
            { it.opptjeningsperiodeTil },
            { it.opplysningspliktigId },
            { it.virksomhetId },
            { it.inntektType },
            { it.fordelType },
            { it.beskrivelse },
            { it.belop },
            { it.etterbetalingsperiodeFra },
            { it.etterbetalingsperiodeTil },
        ),
    )
}
