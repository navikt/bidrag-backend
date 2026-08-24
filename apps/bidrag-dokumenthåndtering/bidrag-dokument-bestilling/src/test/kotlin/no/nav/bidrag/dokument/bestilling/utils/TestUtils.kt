package no.nav.bidrag.dokument.bestilling.utils

import org.springframework.core.io.ClassPathResource

fun readFile(filePath: String): String = String(ClassPathResource("__files/$filePath").inputStream.readAllBytes())
