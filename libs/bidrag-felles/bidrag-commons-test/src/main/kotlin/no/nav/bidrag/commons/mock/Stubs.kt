package no.nav.bidrag.commons.web.mock

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.mockkStatic
import no.nav.bidrag.commons.service.KodeverkProvider
import no.nav.bidrag.commons.service.finnVisningsnavn
import no.nav.bidrag.commons.service.finnVisningsnavnForKode
import no.nav.bidrag.commons.service.finnVisningsnavnLønnsbeskrivelse
import no.nav.bidrag.commons.service.finnVisningsnavnSkattegrunnlag
import no.nav.bidrag.commons.service.sjablon.Barnetilsyn
import no.nav.bidrag.commons.service.sjablon.Bidragsevne
import no.nav.bidrag.commons.service.sjablon.Forbruksutgifter
import no.nav.bidrag.commons.service.sjablon.MaksFradrag
import no.nav.bidrag.commons.service.sjablon.MaksTilsyn
import no.nav.bidrag.commons.service.sjablon.Samværsfradrag
import no.nav.bidrag.commons.service.sjablon.SjablonProvider
import no.nav.bidrag.commons.service.sjablon.SjablonService
import no.nav.bidrag.commons.service.sjablon.Sjablontall
import no.nav.bidrag.commons.service.sjablon.TrinnvisSkattesats
import no.nav.bidrag.transport.felles.commonObjectmapper

class Stubs

fun stubKodeverkProvider() {
    mockkObject(KodeverkProvider)
    mockkStatic(::finnVisningsnavnSkattegrunnlag)
    mockkStatic(::finnVisningsnavnLønnsbeskrivelse)
    mockkStatic(::finnVisningsnavn)
    mockkStatic(::finnVisningsnavnForKode)
    every { finnVisningsnavnForKode(any(), any()) } returns "Visningsnavn"
    every { finnVisningsnavn(any()) } returns "Visningsnavn"
    every {
        finnVisningsnavnLønnsbeskrivelse(any())
    } returns "Visningsnavn lønnsbeskrivelse"
    every { finnVisningsnavnSkattegrunnlag(any()) } returns "Visningsnavn skattegrunnlag"
}

fun stubSjablonService(): SjablonService {
    val sjablonService = mockkClass(SjablonService::class)
    every {
        sjablonService.hentSjablontall()
    } returns sjablonTallResponse()

    every {
        sjablonService.hentSjablonSamværsfradrag()
    } returns sjablonSamværsfradragResponse()

    every {
        sjablonService.hentSjablonBidragsevne()
    } returns sjablonBidragsevneResponse()

    every {
        sjablonService.hentSjablonTrinnvisSkattesats()
    } returns sjablonTrinnvisSkattesatsResponse()

    every {
        sjablonService.hentSjablonBarnetilsyn()
    } returns sjablonBarnetilsynResponse()

    every {
        sjablonService.hentSjablonForbruksutgifter()
    } returns sjablonForbruksutgifterResponse()

    every {
        sjablonService.hentSjablonMaksFradrag()
    } returns sjablonMaksFradragResponse()

    every {
        sjablonService.hentSjablonMaksTilsyn()
    } returns sjablonMaksTilsynResponse()
    return sjablonService
}

fun stubSjablonProvider() {
    stubSjablonService()
    mockkObject(SjablonProvider)
    every {
        SjablonProvider.hentSjablontall()
    } returns sjablonTallResponse()

    every {
        SjablonProvider.hentSjablonSamværsfradrag()
    } returns sjablonSamværsfradragResponse()

    every {
        SjablonProvider.hentSjablonBidragsevne()
    } returns sjablonBidragsevneResponse()

    every {
        SjablonProvider.hentSjablonTrinnvisSkattesats()
    } returns sjablonTrinnvisSkattesatsResponse()

    every {
        SjablonProvider.hentSjablonBarnetilsyn()
    } returns sjablonBarnetilsynResponse()

    every {
        SjablonProvider.hentSjablonForbruksutgifter()
    } returns sjablonForbruksutgifterResponse()

    every {
        SjablonProvider.hentSjablonMaksFradrag()
    } returns sjablonMaksFradragResponse()

    every {
        SjablonProvider.hentSjablonMaksTilsyn()
    } returns sjablonMaksTilsynResponse()
}

fun sjablonTallResponse(): List<Sjablontall> {
    val fil = hentFil("/__files/sjablontall.json")
    return commonObjectmapper.readValue(fil)
}

fun sjablonSamværsfradragResponse(): List<Samværsfradrag> {
    val fil = hentFil("/__files/sjablonSamværsfradrag.json")
    return commonObjectmapper.readValue(fil)
}

fun sjablonBidragsevneResponse(): List<Bidragsevne> {
    val fil = hentFil("/__files/sjablonBidragsevne.json")
    return commonObjectmapper.readValue(fil)
}

fun sjablonTrinnvisSkattesatsResponse(): List<TrinnvisSkattesats> {
    val fil = hentFil("/__files/sjablonTrinnvisSkattesats.json")
    return commonObjectmapper.readValue(fil)
}

fun sjablonBarnetilsynResponse(): List<Barnetilsyn> {
    val fil = hentFil("/__files/sjablonBarnetilsyn.json")
    return commonObjectmapper.readValue(fil)
}

fun sjablonForbruksutgifterResponse(): List<Forbruksutgifter> {
    val fil = hentFil("/__files/sjablonForbruksutgifter.json")
    return commonObjectmapper.readValue(fil)
}

fun sjablonMaksFradragResponse(): List<MaksFradrag> {
    val fil = hentFil("/__files/sjablonMaksFradrag.json")
    return commonObjectmapper.readValue(fil)
}

fun sjablonMaksTilsynResponse(): List<MaksTilsyn> {
    val fil = hentFil("/__files/sjablonMaksTilsyn.json")
    return commonObjectmapper.readValue(fil)
}

fun hentFil(filsti: String) =
    Stubs::class.java.getResource(
        filsti,
    ) ?: throw RuntimeException("Fant ingen fil på sti $filsti")
