package no.nav.familie.ba.mottak.integrasjoner

import com.github.tomakehurst.wiremock.client.WireMock.*
import no.nav.familie.ba.mottak.DevLauncher
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.FilType
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.context.ActiveProfiles


@SpringBootTest(classes = [DevLauncher::class], properties = ["FAMILIE_INTEGRASJONER_API_URL=http://localhost:28085/api"])
@ActiveProfiles("dev", "mock-oauth")
@AutoConfigureWireMock(port = 28085)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DokarkivClientTest {

    @Autowired
    lateinit var dokarkivClient: DokarkivClient


    @Test
    @Tag("integration")
    fun `oppdaterJournalpost skal kjøre OK`() {
        stubFor(put(urlEqualTo("/api/arkiv/v2/${jp.journalpostId}"))
            .withRequestBody(equalToJson(forventetRequestJson()))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(response)))

        dokarkivClient.oppdaterJournalpostSak(jp, "11111111")

    }

    @Test
    @Tag("integration")
    fun `arkiver skal kjøre OK`() {
        val søknadsdokumentJson =
                Dokument("test123".toByteArray(), FilType.JSON, null, "TEST_JSON", "BARNETRYGD_ORDINÆR")
        val søknadsdokumentPdf =
                Dokument("test321".toByteArray(), FilType.PDFA, null, "TEST_PDF", "BARNETRYGD_ORDINÆR")
        val hoveddokumentvarianter = listOf(søknadsdokumentPdf, søknadsdokumentJson)
        stubFor(post(urlEqualTo("/api/arkiv/v3"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(200)
                .withBody(objectMapper.writeValueAsString(Ressurs.success(ArkiverDokumentResponse ("123456", false))))))
        dokarkivClient.arkiver(ArkiverDokumentRequest( jp.bruker!!.id, false, hoveddokumentvarianter))

    }


    @Test
    @Tag("integration")
    fun `ferdigstillJournalpost skal kjøre OK`() {
        val journalpostId = "12345678"
        stubFor(put(urlEqualTo("/api/arkiv/v2/$journalpostId/ferdigstill?journalfoerendeEnhet=9999"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(response)))

        dokarkivClient.ferdigstillJournalpost("12345678")

    }

    @Test
    @Tag("integration")
    fun `DokarkivClient skal kaste feil hvis response er ugyldig`() {
        val journalpostId = "12345678"
        stubFor(put(urlEqualTo("/api/arkiv/v2/$journalpostId/ferdigstill?journalfoerendeEnhet=9999"))
            .willReturn(aResponse()
                .withStatus(500)
                .withBody(objectMapper.writeValueAsString(Ressurs.failure<String>("test")))))

        Assertions.assertThatThrownBy {
            dokarkivClient.ferdigstillJournalpost("12345678")
        }.isInstanceOf(IntegrasjonException::class.java)
            .hasMessageContaining("status=500 body={\"data\":null,\"status\":\"FEILET\",\"melding\":\"test\",\"frontendFeilmelding\":null,\"stacktrace\":")
    }

    fun forventetRequestJson(): String {
        return "{ \n" +
            "  \"bruker\" : {\n" +
            "    \"idType\" : \"FNR\",\n" +
            "    \"id\" : \"12345678910\"\n" +
            "  },\n" +
            "  \"tema\" : \"BAR\",\n" +
            "  \"sak\" : {\n" +
            "    \"fagsakId\" : \"11111111\",\n" +
            "    \"fagsaksystem\" : \"BA\"\n" +
            "  }\n" +
            "}"
    }

    companion object {
        private val response = objectMapper.writeValueAsString(Ressurs.success(mapOf("journalpostId" to "12345678"),"test"))
        private val jp = Journalpost(journalpostId = "12345678",
                                     journalposttype = Journalposttype.I,
                                     journalstatus = Journalstatus.MOTTATT,
                                     bruker = Bruker("12345678910", BrukerIdType.FNR))
    }
}
