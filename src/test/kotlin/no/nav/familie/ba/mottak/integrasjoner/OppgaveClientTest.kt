package no.nav.familie.ba.mottak.integrasjoner

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import no.nav.familie.ba.mottak.config.ApplicationConfig
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.*
import no.nav.familie.log.NavHttpHeaders
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@SpringBootTest(classes = [ApplicationConfig::class], properties = ["FAMILIE_INTEGRASJONER_API_URL=http://localhost:28085/api"])
@ActiveProfiles("dev", "mock-oauth")
@AutoConfigureWireMock(port = 28085)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OppgaveClientTest {

    @Autowired
    @Qualifier("oppgaveClient")
    lateinit var oppgaveClient: OppgaveClient

    @Value("\${FAMILIE_INTEGRASJONER_API_URL}")
    lateinit var integrasjonerUri: String

    @AfterEach
    fun cleanUp() {
        MDC.clear()
        WireMock.resetAllRequests()
    }

    @Test
    @Tag("integration")
    fun `Opprett journalføringsoppgave skal returnere oppgave id`() {
        MDC.put("callId", "opprettJournalføringsoppgave")
        stubFor(post(urlEqualTo("/api/oppgave"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(
                    objectMapper.writeValueAsString(Ressurs.success(OppgaveResponse(oppgaveId = 1234))))))

        val opprettOppgaveResponse = oppgaveClient.opprettJournalføringsoppgave(journalPost)

        assertThat(opprettOppgaveResponse.oppgaveId).isEqualTo(1234)
        verify(anyRequestedFor(anyUrl())
            .withHeader(NavHttpHeaders.NAV_CALL_ID.asString(), equalTo("opprettJournalføringsoppgave"))
            .withHeader(NavHttpHeaders.NAV_CONSUMER_ID.asString(), equalTo("familie-ba-mottak"))
            .withRequestBody(equalToJson(forventetOpprettOppgaveJRequestJson("1234567"))))
    }

    @Test
    @Tag("integration")
    fun `Opprett journalføringsoppgave skal kaste feil hvis response er ugyldig`() {
        stubFor(post(urlEqualTo("/api/oppgave"))
            .willReturn(aResponse()
                .withStatus(500)
                .withBody(objectMapper.writeValueAsString(Ressurs.failure<String>("test")))))

        Assertions.assertThatThrownBy {
            oppgaveClient.opprettJournalføringsoppgave(journalPost)
        }.isInstanceOf(IntegrasjonException::class.java)
            .hasMessageContaining("Kall mot integrasjon feilet ved opprettelse av oppgave")

    }

    private fun forventetOpprettOppgaveJRequestJson(journalpostId: String): String {
        return "{\n" +
            "  \"ident\": {\n" +
            "    \"ident\": \"1234567891011\",\n" +
            "    \"type\": \"Aktør\"\n" +
            "  },\n" +
            "  \"enhetsnummer\": \"9999\",\n" +
            "  \"saksId\": null,\n" +
            "  \"journalpostId\": \"$journalpostId\",\n" +
            "  \"tema\": \"BAR\",\n" +
            "  \"oppgavetype\": \"Journalføring\",\n" +
            "  \"behandlingstema\": \"ab0180\",\n" +
            "  \"fristFerdigstillelse\": \"${LocalDate.now().plusDays(2)}\",\n" +
            "  \"aktivFra\": \"${LocalDate.now()}\",\n" +
            "  \"beskrivelse\": \"\"\n" +
            "}"
    }

    companion object {
        private val journalPost = Journalpost("1234567", Journalposttype.I, Journalstatus.MOTTATT, "tema",
            "behandlingstema", null, Bruker("1234567891011", BrukerIdType.AKTOERID), "9999", "kanal", listOf())
    }
}