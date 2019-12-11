package no.nav.familie.ba.mottak.controller

import no.nav.familie.ba.mottak.task.SendTilSakTask
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/test")
@Profile(value = ["dev", "postgres"])
class TestTaskController(private val taskRepository: TaskRepository) {


    @GetMapping(path = ["/lagtask"])
    @Unprotected
    fun task() {
        val task = Task.nyTask(SendTilSakTask.SEND_TIL_SAK, "test")
        taskRepository.save(task)
    }
}