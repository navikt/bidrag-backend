package no.nav.bidrag.statistikk.util

import no.nav.bidrag.transport.felles.commonObjectmapper

open class StatistikkUtil {

    companion object {
        fun tilJson(json: Any): String = commonObjectmapper.writeValueAsString(json)
    }
}
