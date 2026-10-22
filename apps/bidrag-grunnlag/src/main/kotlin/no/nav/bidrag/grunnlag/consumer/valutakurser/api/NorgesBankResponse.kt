package no.nav.bidrag.grunnlag.consumer.valutakurser.api

data class SdmxSimplified(val data: SdmxData)

data class SdmxData(val dataSets: List<SdmxDataSet>, val structure: SdmxStructure)

data class SdmxDataSet(val series: Map<String, SdmxSeries?>)

data class SdmxSeries(val observations: Map<String, List<String>?>?, val attributes: List<Int?> = emptyList())

data class SdmxStructure(val dimensions: SdmxDimensions, val attributes: SdmxAttributes = SdmxAttributes())

data class SdmxAttributes(val series: List<SdmxDimension> = emptyList())

data class SdmxDimensions(val series: List<SdmxDimension>, val observation: List<SdmxDimension> = emptyList())

data class SdmxDimension(val id: String, val values: List<SdmxValue>)

data class SdmxValue(val id: String?)
