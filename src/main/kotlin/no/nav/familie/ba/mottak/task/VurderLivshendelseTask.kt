package no.nav.familie.ba.mottak.task

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.mottak.config.FeatureToggleService
import no.nav.familie.ba.mottak.integrasjoner.*
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstema
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
@TaskStepBeskrivelse(taskStepType = VurderLivshendelseTask.TASK_STEP_TYPE, beskrivelse = "Vurder livshendelse")
class VurderLivshendelseTask(
    private val oppgaveClient: OppgaveClient,
    private val taskRepository: TaskRepository,
    private val pdlClient: PdlClient,
    private val sakClient: SakClient,
    private val featureToggleService: FeatureToggleService
) : AsyncTaskStep {

    val log: Logger = LoggerFactory.getLogger(OpprettBehandleSakOppgaveTask::class.java)
    val oppgaveOpprettetDødsfallCounter: Counter = Metrics.counter("barnetrygd.dodsfall.oppgave.opprettet")
    val oppgaveIgnorerteDødsfallCounter: Counter = Metrics.counter("barnetrygd.dodsfall.oppgave.ignorert")

    override fun doTask(task: Task) {
        val payload = objectMapper.readValue(task.payload, VurderLivshendelseTaskDTO::class.java)
        //hent familierelasjoner
        val pdlPersonData = pdlClient.hentPerson(payload.personIdent, "hentperson-relasjon-dødsfall")
        val familierelasjon = pdlPersonData.familierelasjoner
        when(payload.type) {
            VurderLivshendelseType.DØDSFALL -> {
                if (pdlPersonData.dødsfall?.firstOrNull()?.dødsdato != null) {
                    //Skal man gjøre spesifikk filtrering med OR for å sikre at det ikke kommer en ny relasjonstype
                    val listeMedBarn =
                        familierelasjon.filter { it.minRolleForPerson != Familierelasjonsrolle.BARN }.map { it.relatertPersonsIdent }
                    if (listeMedBarn.isNotEmpty()) {
                        //Her er vi kun interessert i om den som dør er SØKER. Er Sakspart ANNEN, så er det annen part som har
                        //søkt og er mest sannsynlig levende
                        val sak = sakClient.hentPågåendeSakStatus(payload.personIdent, listeMedBarn)
                        if (sak.baSak == Sakspart.SØKER) {
                            val fagsak = sakClient.hentRestFagsak(payload.personIdent)
                            val restUtvidetBehandling = fagsak.behandlinger.first { it.aktiv }
                            if (featureToggleService.isEnabled("familie-ba-mottak.opprettLivshendelseOppgave", false)) {
                                //TODO beskrivelse????
                                val oppgave = oppgaveClient.opprettVurderLivshendelseOppgave(OppgaveVurderLivshendelseDto(payload.personIdent, "Søker har aktiv sak", fagsak.id.toString(), tilBehandlingstema(restUtvidetBehandling)))
                                task.metadata["oppgaveId"] = oppgave.oppgaveId
                                taskRepository.saveAndFlush(task)
                                oppgaveOpprettetDødsfallCounter.increment()
                            } else {
                                oppgaveIgnorerteDødsfallCounter.increment()
                            }
                        }
                    }


                    if (pdlPersonData.fødsel.isEmpty() || pdlPersonData.fødsel.first().fødselsdato.isAfter(LocalDate.now().minusYears(19))) {
                        val listeMedForeldreForBarn =
                                familierelasjon.filter { it.minRolleForPerson == Familierelasjonsrolle.BARN }.map { it.relatertPersonsIdent }

                        listeMedForeldreForBarn.forEach {
                            val sak = sakClient.hentPågåendeSakStatus(it, listOf(payload.personIdent))
                            if (sak.baSak == Sakspart.SØKER) {
                                val fagsak = sakClient.hentRestFagsak(it)
                                val restUtvidetBehandling = fagsak.behandlinger.first { it.aktiv }
                                if (featureToggleService.isEnabled("familie-ba-mottak.opprettLivshendelseOppgave", false)) {
                                    val oppgave = oppgaveClient.opprettVurderLivshendelseOppgave(OppgaveVurderLivshendelseDto(it, "Barn har aktiv sak", fagsak.id.toString(), tilBehandlingstema(restUtvidetBehandling)))
                                    task.metadata["oppgaveId"] = oppgave.oppgaveId
                                    taskRepository.saveAndFlush(task)
                                    oppgaveOpprettetDødsfallCounter.increment()
                                } else {
                                    oppgaveIgnorerteDødsfallCounter.increment()
                                }
                            }
                        }
                    }

                }
            }
            else -> log.debug("Behandlinger enda ikke livshendelse av type ${payload.type}")
        }
    }

    private fun tilBehandlingstema(restUtvidetBehandling: RestUtvidetBehandling ): String {
        return when {
            restUtvidetBehandling.kategori == BehandlingKategori.EØS -> Behandlingstema.BarnetrygdEØS.value
            restUtvidetBehandling.kategori == BehandlingKategori.NASJONAL && restUtvidetBehandling.underkategori == BehandlingUnderkategori.ORDINÆR -> Behandlingstema.OrdinærBarnetrygd.value
            restUtvidetBehandling.kategori == BehandlingKategori.NASJONAL && restUtvidetBehandling.underkategori == BehandlingUnderkategori.UTVIDET -> Behandlingstema.UtvidetBarnetrygd.value
            else -> Behandlingstema.Barnetrygd.value
        }
    }

    companion object {

        const val TASK_STEP_TYPE = "vurderLivshendelseTask"
    }
}

data class VurderLivshendelseTaskDTO(val personIdent: String, val type: VurderLivshendelseType)

enum class VurderLivshendelseType {
    DØDSFALL,
    SIVILSTAND,
    ADDRESSE
}