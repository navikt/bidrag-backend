package no.nav.bidrag.person.hendelse.konfigurasjon

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/**
 * Verifiserer at Kafka faktisk klarer å deserialisere en spesifikk Avro-record
 * når pakken er tillatt av AvroSecurityConfig.
 */
@DisplayName("Kafka Avro-deserialisering av Personhendelse")
internal class AvroSecurityConfigTest {

    @Test
    @DisplayName("skal serialisere og deserialisere Personhendelse korrekt via ekte Kafka Avro (de)serializer når pakken er tillatt")
    fun `skal serialisere og deserialisere personhendelse korrekt naar pakken er tillatt av AvroSecurityConfig`() {
        AvroSecurityConfig("no.nav.person.pdl.leesah").konfigurerAvroSerializablePackages()
        val original = opprettPersonhendelse()

        val deserialisert = deserialiser(serialiser(original))

        deserialisert.shouldBeInstanceOf<Personhendelse>()
        // Avro sine CharSequence-felt deserialiseres som org.apache.avro.util.Utf8 og må
        // konverteres til String før sammenligning for å unngå falske negativer.
        deserialisert.hendelseId.toString() shouldBe original.hendelseId.toString()
        deserialisert.personidenter.map { it.toString() } shouldBe original.personidenter.map { it.toString() }
        deserialisert.opplysningstype.toString() shouldBe original.opplysningstype.toString()
        deserialisert.endringstype shouldBe original.endringstype
    }

    private fun opprettPersonhendelse(): Personhendelse = Personhendelse
        .newBuilder()
        .setHendelseId("567f35f1-b5c0-4457-8848-01d897d78bba")
        .setPersonidenter(listOf(genererFødselsnummer()))
        .setMaster("FREG")
        .setOpprettet(Instant.now())
        .setOpplysningstype("DOEDSFALL_V1")
        .setEndringstype(Endringstype.OPPRETTET)
        .build()

    private fun serialiser(record: Personhendelse): ByteArray = KafkaAvroSerializer().use {
        it.configure(mapOf(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryUrl), false)
        it.serialize(TOPIC, record)
    }

    private fun deserialiser(bytes: ByteArray): Personhendelse = KafkaAvroDeserializer().use {
        it.configure(
            mapOf(
                KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryUrl,
                KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to "true",
            ),
            false,
        )
        it.deserialize(TOPIC, bytes) as Personhendelse
    }

    companion object {
        private const val TOPIC = "avro-security-config-test-topic"

        // Unik pr. testklasse-instansiering slik at ikke testen deler skjemaregister-tilstand med andre tester.
        private val schemaRegistryUrl = "mock://${UUID.randomUUID()}"
    }
}
