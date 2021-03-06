package no.nav.familie.ba.mottak.util

import org.springframework.core.env.Environment
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month


fun fristFerdigstillelse(daysToAdd: Long = 0): LocalDate {
    var date = LocalDateTime.now().plusDays(daysToAdd)

    if (date.hour >= 14) {
        date = date.plusDays(1)
    }

    when (date.dayOfWeek) {
        DayOfWeek.SATURDAY -> date = date.plusDays(2)
        DayOfWeek.SUNDAY -> date = date.plusDays(1)
    }

    when {
        date.dayOfMonth == 1 && date.month == Month.JANUARY -> date = date.plusDays(1)
        date.dayOfMonth == 1 && date.month == Month.MAY -> date = date.plusDays(1)
        date.dayOfMonth == 17 && date.month == Month.MAY -> date = date.plusDays(1)
        date.dayOfMonth == 25 && date.month == Month.DECEMBER -> date = date.plusDays(2)
        date.dayOfMonth == 26 && date.month == Month.DECEMBER -> date = date.plusDays(1)
    }

    when (date.dayOfWeek) {
        DayOfWeek.SATURDAY -> date = date.plusDays(2)
        DayOfWeek.SUNDAY -> date = date.plusDays(1)
    }

    return date.toLocalDate()
}

/**
 * Skipper helger hvis ikke e2e.
 */
fun nesteGyldigeTriggertidFødselshendelser(minutesToAdd: Long = 0, environment: Environment): LocalDateTime {
    var date = LocalDateTime.now().plusMinutes(minutesToAdd)

    if (environment.activeProfiles.contains("e2e")) {
        return date
    }

    when {
        date.dayOfWeek == DayOfWeek.SATURDAY -> date = date.plusDays(2)
        date.dayOfWeek == DayOfWeek.SUNDAY -> date = date.plusDays(1)
        date.dayOfWeek == DayOfWeek.FRIDAY && date.hour >= 16 -> date = date.plusHours(56)
    }

    when {
        date.dayOfMonth == 1 && date.month == Month.JANUARY -> date = date.plusDays(1)
        date.dayOfMonth == 1 && date.month == Month.MAY -> date = date.plusDays(1)
        date.dayOfMonth == 17 && date.month == Month.MAY -> date = date.plusDays(1).withHour(10)
        date.dayOfMonth == 25 && date.month == Month.DECEMBER -> date = date.plusDays(2).withHour(10)
        date.dayOfMonth == 26 && date.month == Month.DECEMBER -> date = date.plusDays(1).withHour(10)
    }

    when {
        date.dayOfWeek == DayOfWeek.SATURDAY -> date = date.plusDays(2)
        date.dayOfWeek == DayOfWeek.SUNDAY -> date = date.plusDays(1)
        date.dayOfWeek == DayOfWeek.FRIDAY && date.hour >= 16 -> date = date.plusHours(56)
        date.dayOfWeek == DayOfWeek.MONDAY && date.hour <= 8 -> date = date.withHour(10)
    }

    return date
}
