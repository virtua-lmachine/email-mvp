package com.example.emailmvp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP (RestClient) usado para llamar al Worker, con tiempos de espera
 * para no dejar peticiones colgadas.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient workerRestClient(WorkerProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeoutMs());
        factory.setReadTimeout(properties.readTimeoutMs());

        return RestClient.builder()
                .baseUrl(properties.url())
                .requestFactory(factory)
                .build();
    }
}
