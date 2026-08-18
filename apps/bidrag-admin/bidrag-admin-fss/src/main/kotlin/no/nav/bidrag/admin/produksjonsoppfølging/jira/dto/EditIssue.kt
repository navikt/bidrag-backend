package no.nav.bidrag.admin.produksjonsoppfølging.jira.dto

data class EditIssue(
    val update: Update = Update(),
) {
    data class Update(
        val labels: MutableList<LabelOperation> = mutableListOf(),
    ) {
        fun addLabel(label: String) = apply { labels.add(AddLabel(label)) }
    }

    sealed interface LabelOperation

    data class AddLabel(
        val add: String,
    ) : LabelOperation
}
