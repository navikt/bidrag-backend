package no.nav.bidrag.commons.util

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.examples.Example
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType

@Configuration
class OpenApiExamplesConfig {
    @Bean
    fun openApiCustomiser(examples: Collection<OpenApiExample>): OpenApiCustomizer =
        OpenApiCustomizer { openAPI ->
            examples.forEach { example ->
                openAPI.components.addExamples(example.name, example.example)
                examples.groupBy { Pair(it.path, it.method) }.entries.forEach {
                    openAPI.addExamplesToPath(it.key, it.value)
                }
            }
        }

    fun OpenAPI.addExamplesToPath(
        operation: Pair<String, HttpMethod>,
        openApiExamples: List<OpenApiExample>,
    ) {
        val requestBody =
            paths[operation.first]
                ?.getOperation(operation.second)
                ?.requestBody

        val jsonBody = requestBody?.content?.get(MediaType.APPLICATION_JSON_VALUE)

        openApiExamples
            .forEach {
                jsonBody?.addExamples(it.name, it.example)
            }
    }

    fun PathItem.getOperation(httpMethod: HttpMethod) =
        when (httpMethod) {
            HttpMethod.POST -> post
            HttpMethod.PUT -> put
            HttpMethod.PATCH -> patch
            HttpMethod.DELETE -> delete
            HttpMethod.OPTIONS -> options
            HttpMethod.GET -> get
            else -> null
        }
}

data class OpenApiExample(
    val example: Example,
    val method: HttpMethod,
    val path: String,
    val name: String = example.description,
)
