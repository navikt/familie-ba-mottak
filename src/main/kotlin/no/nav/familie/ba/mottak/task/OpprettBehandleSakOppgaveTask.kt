package no.nav.familie.ba.mottak.task

import no.nav.familie.ba.mottak.integrasjoner.JournalpostClient
import no.nav.familie.ba.mottak.integrasjoner.Journalstatus
import no.nav.familie.ba.mottak.integrasjoner.OppgaveClient
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = OpprettBehandleSakOppgaveTask.TASK_STEP_TYPE, beskrivelse = "Opprett \"BehandleSak\"-oppgave")
class OpprettBehandleSakOppgaveTask(private val journalpostClient: JournalpostClient,
                                    private val oppgaveClient: OppgaveClient,
                                    private val taskRepository: TaskRepository) : AsyncTaskStep {

    val log: Logger = LoggerFactory.getLogger(OpprettBehandleSakOppgaveTask::class.java)

    override fun doTask(task: Task) {
        val journalpost = journalpostClient.hentJournalpost(task.payload)

        if (journalpost.journalstatus == Journalstatus.JOURNALFOERT) {
            val oppgaver = oppgaveClient.finnOppgaver(journalpost.journalpostId, null)
            if (oppgaver.isNullOrEmpty()) {
                var beskrivelse = journalpost.hentHovedDokumentTittel()
                if (task.metadata["fagsystem"] == "BA") {
                    beskrivelse = "Må behandles i BA-sak.\n $beskrivelse"
                }
                task.metadata["oppgaveId"] =
                        "${oppgaveClient.opprettBehandleSakOppgave(journalpost, beskrivelse).oppgaveId}"
                taskRepository.saveAndFlush(task)
            } else {
                log.error("Det eksister minst 1 åpen oppgave på journalpost ${task.payload}")
                throw error("Det eksister minst 1 åpen oppgave på journalpost ${task.payload}")
            }
        } else {
            log.error("Kan ikke opprette oppgave før tilhørende journalpost ${journalpost.journalpostId} er ferdigstilt")
            throw error("Kan ikke opprette oppgave før tilhørende journalpost ${journalpost.journalpostId} er ferdigstilt")
        }
    }

    companion object {
        const val TASK_STEP_TYPE = "opprettBehandleSakoppgave"
    }
}