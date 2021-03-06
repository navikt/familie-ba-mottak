package no.nav.familie.ba.mottak.config

import no.nav.familie.http.config.RestTemplateSts
import no.nav.familie.http.interceptor.BearerTokenClientInterceptor
import no.nav.familie.http.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.http.interceptor.MdcValuesPropagatingClientInterceptor
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.*
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestOperations

@Configuration
@Import(BearerTokenClientInterceptor::class,
        MdcValuesPropagatingClientInterceptor::class,
        RestTemplateSts::class)
class RestTemplateConfig {

    @Profile("!dev || !e2e || !postgres")
    @Bean("clientCredentials")
    fun restTemplateClientCredentials(consumerIdClientInterceptor: ConsumerIdClientInterceptor,
                                      bearerTokenClientInterceptor: BearerTokenClientInterceptor): RestOperations {
        return RestTemplateBuilder()
            .additionalCustomizers(NaisProxyCustomizer())
            .interceptors(consumerIdClientInterceptor,
                bearerTokenClientInterceptor,
                MdcValuesPropagatingClientInterceptor())
            .additionalMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
            .build()
    }

    @Bean("restTemplateUnsecured")
    fun restTemplate(restTemplateBuilder: RestTemplateBuilder,
                     mdcInterceptor: MdcValuesPropagatingClientInterceptor,
                     consumerIdClientInterceptor: ConsumerIdClientInterceptor): RestOperations {
        return restTemplateBuilder.interceptors(mdcInterceptor, consumerIdClientInterceptor).build()
    }

}
