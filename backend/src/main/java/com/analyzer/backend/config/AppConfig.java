package com.analyzer.backend.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class AppConfig {

    @Value("${ai-service.base-url}")
    private String aiServiceBaseUrl;

    @Value("${ai-service.timeout-seconds:30}")
    private int timeoutSeconds;

    @Value("${ai-service.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Bean
    public WebClient aiServiceWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofSeconds(timeoutSeconds))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(timeoutSeconds,  TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)));

        return WebClient.builder()
                .baseUrl(aiServiceBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept",       "application/json")
                .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                .build();
    }
}