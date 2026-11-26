package com.clinicbooking.notification.infrastructure.config;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/** Fábrica de RestClient con timeouts de conexión y lectura. */
public final class RestClients {
    private RestClients() {
    }

    public static RestClient create(RestClient.Builder builder, String baseUrl, long connectMs, long readMs) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectMs)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(readMs));
        return builder.baseUrl(baseUrl).requestFactory(factory).build();
    }
}
