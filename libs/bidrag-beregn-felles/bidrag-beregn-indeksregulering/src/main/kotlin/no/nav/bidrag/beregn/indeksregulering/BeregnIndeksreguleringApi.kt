package no.nav.bidrag.beregn.indeksregulering

import no.nav.bidrag.commons.service.sjablon.EnableSjablonProvider
import no.nav.bidrag.beregn.indeksregulering.bo.BeregnIndeksreguleringGrunnlag
import no.nav.bidrag.beregn.indeksregulering.service.BeregnIndeksreguleringService
import org.springframework.stereotype.Service
import java.time.YearMonth

/**
 * BeregnIndeksreguleringApi eksponerer api for å indeksregulere stønad.
 *
 */
@EnableSjablonProvider
@Service
class BeregnIndeksreguleringApi {
    private val service = BeregnIndeksreguleringService()
    fun beregnIndeksregulering(grunnlag: BeregnIndeksreguleringGrunnlag) = service.beregn(grunnlag)
}
