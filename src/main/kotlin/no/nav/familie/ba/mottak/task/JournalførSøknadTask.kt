package no.nav.familie.ba.mottak.task

import no.nav.familie.ba.mottak.søknad.JournalføringService
import no.nav.familie.ba.mottak.søknad.PdfService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = JournalførSøknadTask.JOURNALFØR_SØKNAD, beskrivelse = "Journalfør søknad")
class JournalførSøknadTask(private val pdfService: PdfService,
                           private val journalføringService: JournalføringService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        try {
            log.info("Generer pdf og journalfør søknad")
            val pdf = pdfService.lagPdf(task.payload)
            journalføringService.journalførSøknad(task.payload, pdf)
        } catch (e: Exception) {
            log.error("Uventet feil ved journalføring av søknad. taskId=${task.id}. Se task eller securelog")
            SECURE_LOGGER.error("Uventet feil ved journalføring søknad $task", e)
            throw e
        }
    }

    companion object {
        const val JOURNALFØR_SØKNAD = "journalførSøknad"
        val log: Logger = LoggerFactory.getLogger(this::class.java)
        val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLogger")
    }

}