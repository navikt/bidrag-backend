package no.nav.bidrag.grunnlag.consumer.valutakurser.api

data class SdmxSimplified(val data: SdmxData)

data class SdmxData(val dataSets: List<SdmxDataSet>, val structure: SdmxStructure)

data class SdmxDataSet(val series: Map<String, SdmxSeries>)

data class SdmxSeries(val observations: Map<String, List<String>>)

data class SdmxStructure(val dimensions: SdmxDimensions)

data class SdmxDimensions(val series: List<SdmxDimension>)

data class SdmxDimension(val id: String, val values: List<SdmxValue>)

data class SdmxValue(val id: String)
