package no.nav.familie.ba.mottak.integrasjoner

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.client.RestClientResponseException
import java.net.URI

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
class IntegrasjonException(msg: String,
                           throwable: Throwable? = null,
                           uri: URI? = null,
                           ident: String? = null) : RuntimeException(msg, throwable) {

    init {
        val message = if (throwable is RestClientResponseException) throwable.responseBodyAsString else ""

        secureLogger.info("Ukjent feil ved integrasjon mot {}. ident={} {} {}",
                          uri,
                          ident,
                          message,
                          throwable)
        logger.warn("Ukjent feil ved integrasjon mot '{}'.", uri)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IntegrasjonException::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}