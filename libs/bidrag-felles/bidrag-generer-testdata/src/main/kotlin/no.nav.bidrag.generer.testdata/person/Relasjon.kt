package no.nav.bidrag.generer.testdata.person

@Suppress("unused")
fun interface Relasjon {
    fun relater(
        testPerson: TestPerson?,
        tilPerson: TestPersonBuilder?,
    )

    companion object {
        val SAMME_ETTERNAVN: Relasjon =
            Relasjon { person: TestPerson?, builder: TestPersonBuilder? -> builder?.etternavn(person!!.etternavn) }
        val SAMME_ADRESSE: Relasjon =
            Relasjon { person: TestPerson?, builder: TestPersonBuilder? ->
                builder?.boadresse(person?.boadresse)
                builder?.postadresse(person?.postadresse)
            }

        fun relater(
            testPerson: TestPerson?,
            tilPerson: TestPersonBuilder?,
            relasjoner: Array<out Relasjon>?,
        ) {
            if (relasjoner != null) {
                for (relasjon in relasjoner) {
                    relasjon.relater(testPerson, tilPerson)
                }
            }
        }
    }
}
