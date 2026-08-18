package no.nav.bidrag.grunnlag.comparator

import no.nav.bidrag.grunnlag.SECURE_LOGGER
import no.nav.bidrag.grunnlag.bo.SkattegrunnlagBo
import no.nav.bidrag.grunnlag.bo.SkattegrunnlagspostBo
import no.nav.bidrag.transport.felles.commonObjectmapper

class SkattegrunnlagPeriodComparator : AbstractPeriodComparator<PeriodComparable<SkattegrunnlagBo, SkattegrunnlagspostBo>>() {
    override fun isEntitiesEqual(
        newEntity: PeriodComparable<SkattegrunnlagBo, SkattegrunnlagspostBo>,
        existingEntity: PeriodComparable<SkattegrunnlagBo, SkattegrunnlagspostBo>,
    ): Boolean {
        val newSkattegrunnlagsposter = sortSkattegrunnlagsposter(newEntity.children!!)
        val existingSkattegrunnlagsposter = sortSkattegrunnlagsposter(existingEntity.children!!)
        if (newSkattegrunnlagsposter.size != existingSkattegrunnlagsposter.size) {
            return false
        }
        val differences = mutableMapOf<String, String>()
        for (i in newSkattegrunnlagsposter.indices) {
            val newSkattegrunnlagspost = newSkattegrunnlagsposter[i]
            val existingSkattegrunnlagspost = existingSkattegrunnlagsposter[i]

            differences.putAll(compareFields(newSkattegrunnlagspost.inntektType, existingSkattegrunnlagspost.inntektType, "inntektType"))
            differences.putAll(
                compareFields(newSkattegrunnlagspost.skattegrunnlagType, existingSkattegrunnlagspost.skattegrunnlagType, "skattegrunnlagType"),
            )
            differences.putAll(compareFields(newSkattegrunnlagspost.belop, existingSkattegrunnlagspost.belop, "belop"))
        }
        if (differences.isNotEmpty()) {
            SECURE_LOGGER.debug(commonObjectmapper.findAndRegisterModules().writeValueAsString(differences))
        }
        return differences.isEmpty()
    }

    private fun sortSkattegrunnlagsposter(ainntektsposter: List<SkattegrunnlagspostBo>): List<SkattegrunnlagspostBo> = ainntektsposter.sortedWith(
        compareBy({
            it.inntektType
        }, { it.skattegrunnlagType }, { it.belop }),
    )
}
