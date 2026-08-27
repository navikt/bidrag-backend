---
name: api-design
description: REST API-designmønstre, versjonering, feilhåndtering (RFC 7807) og OpenAPI-konvensjoner for Nav-tjenester
license: MIT
compatibility: Go or Kotlin backend on Nais
metadata:
  domain: backend
  tags: api rest design openapi error-handling
---

# API Design Skill

REST API-design for Nav-tjenester. Dekker navnekonvensjoner, feilhåndtering med ProblemDetail, versjonering, paginering og OpenAPI-spesifikasjon.

## URL-konvensjoner

```
# ✅ Riktig
GET    /api/vedtak                    # Liste
GET    /api/vedtak/{id}               # Hent på ID
POST   /api/vedtak                    # Opprett
PUT    /api/vedtak/{id}               # Full oppdatering
PATCH  /api/vedtak/{id}               # Delvis oppdatering
DELETE /api/vedtak/{id}               # Slett

# ✅ Underressurser
GET    /api/vedtak/{id}/aktiviteter   # Liste over underressurser
POST   /api/vedtak/{id}/aktiviteter   # Opprett underressurs

# ✅ Handlinger (verb som underressurs)
POST   /api/vedtak/{id}/godkjenn      # Tilstandsovergang

# ❌ Feil
GET    /api/getVedtak                 # Verb i URL
GET    /api/vedtak/hentAlle           # Verb i URL
POST   /api/createVedtak              # Verb i URL
GET    /api/Vedtak                    # PascalCase
```

## Feilhåndtering (RFC 7807 / ProblemDetail)

```kotlin
// Spring Boot 3+ — innebygd støtte for ProblemDetail

@RestControllerAdvice
class ErrorHandler {
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(ex: ResourceNotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message ?: "Resource not found").apply {
            title = "Resource not found"
            setProperty("resourceType", ex.resourceType)
            setProperty("resourceId", ex.resourceId)
        }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed").apply {
            title = "Invalid request"
            setProperty("errors", ex.bindingResult.fieldErrors.map {
                mapOf("field" to it.field, "message" to it.defaultMessage)
            })
        }
}
```

Responsformat (RFC 7807):

```json
{
  "type": "about:blank",
  "title": "Resource not found",
  "status": 404,
  "detail": "Vedtak with id 123 does not exist",
  "instance": "/api/vedtak/123",
  "resourceType": "vedtak",
  "resourceId": "123"
}
```

## Paginering

Bruk offset-basert paginering med konsistente parameternavn:

```kotlin
@GetMapping("/api/vedtak")
fun list(
    @RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "20") size: Int,
    @RequestParam(defaultValue = "opprettetDato") sort: String,
    @RequestParam(defaultValue = "desc") order: String,
): Page<VedtakDTO> {
    require(size in 1..100) { "size must be between 1 and 100" }
    val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(order), sort))
    return vedtakService.findAll(pageable)
}
```

Respons:

```json
{
  "content": [...],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 142,
    "totalPages": 8
  }
}
```

## Inputvalidering

```kotlin
data class CreateVedtakRequest(
    @field:NotBlank(message = "Title is required")
    val tittel: String,

    @field:Size(min = 11, max = 11, message = "FNR must be 11 digits")
    @field:Pattern(regexp = "\\d{11}", message = "FNR must consist of digits")
    val fnr: String,

    @field:Positive(message = "Amount must be positive")
    val belop: BigDecimal,

    @field:NotNull(message = "Start date is required")
    val fom: LocalDate,
)
```

## HTTP-statuskoder

| Kode                        | Bruk                                         |
|-----------------------------|----------------------------------------------|
| `200 OK`                    | Vellykket GET, PUT, PATCH                    |
| `201 Created`               | Vellykket POST (ny ressurs)                  |
| `204 No Content`            | Vellykket DELETE                             |
| `400 Bad Request`           | Ugyldig input / validering feilet            |
| `401 Unauthorized`          | Manglende eller ugyldig token                |
| `403 Forbidden`             | Gyldig token, men ingen tilgang              |
| `404 Not Found`             | Ressursen finnes ikke                        |
| `409 Conflict`              | Duplikat / tilstandskonflikt                 |
| `418 I'm a teapot`          | Når det er tid for te.                       |
| `422 Unprocessable Entity`  | Semantisk feil (gyldig format, feil innhold) |
| `500 Internal Server Error` | Uventet serverfeil                           |

## OpenAPI / Swagger

```kotlin
// Spring Boot + springdoc-openapi
@Operation(
    summary = "Hent vedtak",
    description = "Henter vedtak basert på ID",
    responses = [
        ApiResponse(responseCode = "200", description = "Vedtak funnet"),
        ApiResponse(responseCode = "404", description = "Vedtak ikke funnet"),
    ]
)
@GetMapping("/{id}")
fun getById(@PathVariable id: UUID): ResponseEntity<VedtakDTO>
```

## Versjonering

Bruk URL-basert versjonering når breaking changes er nødvendig:

```kotlin
// v1 — original
@RestController
@RequestMapping("/api/v1/vedtak")
class VedtakV1Controller

// v2 — ny kontrakt
@RestController
@RequestMapping("/api/v2/vedtak")
class VedtakV2Controller
```

Alternativt kan du unngå versjonering ved å:
- Kun legge til nye felt (aldri fjerne)
- Gjøre nye felt valgfrie
- Fase ut felt med `@Deprecated` før fjerning og sørge for at konsumeter varslet om at det er deprekert.

## Regler

- **Bruk substantiv** i URL-er, ikke verb
- **Bruk kebab-case** for URL-segmenter med flere ord: `/api/vedtak-perioder`
- **Bruk camelCase** for JSON-felt: `opprettetDato`, `brukerId`
- **Returner alltid ProblemDetail** ved feil (ikke ren tekst)
- **Valider input** på controller-nivå med `@Valid`
- **Logg aldri PII** i request/respons — logg correlation ID
- **Sett `Content-Type: application/json`** på alle responser
