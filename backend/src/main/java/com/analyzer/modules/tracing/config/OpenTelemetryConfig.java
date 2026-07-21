package com.analyzer.modules.tracing.config;

import com.analyzer.modules.tracing.exporter.PostgresSpanExporter;
import com.analyzer.modules.tracing.repository.TraceSpanRepository;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;
import java.util.Collection;

@Slf4j
@Configuration
@EnableScheduling
public class OpenTelemetryConfig {

    @Value("${otel.service.name:microservices-analyzer}")
    private String serviceName;

    @Value("${otel.exporter.otlp.endpoint:http://localhost:4317}")
    private String otlpEndpoint;

    @Value("${otel.environment:local}")
    private String environment;

    private OpenTelemetrySdk openTelemetrySdk;

    @Bean
    public PostgresSpanExporter postgresSpanExporter(TraceSpanRepository repository) {
        return new PostgresSpanExporter(repository);
    }

    @Bean
    public OtlpGrpcSpanExporter otlpSpanExporter() {
        return OtlpGrpcSpanExporter.builder()
                .setEndpoint(otlpEndpoint)
                .setTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Bean
    public SdkTracerProvider sdkTracerProvider(
            PostgresSpanExporter postgresExporter,
            OtlpGrpcSpanExporter otlpExporter) {

        Resource resource = Resource.getDefault().merge(
                Resource.create(Attributes.of(
                        AttributeKey.stringKey("service.name"), serviceName,
                        AttributeKey.stringKey("deployment.environment"), environment
                )));

        SpanExporter composite = new CompositeSpanExporter(postgresExporter, otlpExporter);

        SdkTracerProvider provider = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(SimpleSpanProcessor.create(composite))
                .build();

        log.info("OpenTelemetry TracerProvider initialized — service: {}, otlp: {}",
                serviceName, otlpEndpoint);

        return provider;
    }

    @Bean
    public OpenTelemetrySdk openTelemetrySdk(SdkTracerProvider tracerProvider) {
        openTelemetrySdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();
        return openTelemetrySdk;
    }

    @Bean
    public Tracer otelTracer(OpenTelemetrySdk sdk) {
        return sdk.getTracer(serviceName);
    }

    @PreDestroy
    public void shutdown() {
        if (openTelemetrySdk != null) {
            log.info("Shutting down OpenTelemetry SDK");
            openTelemetrySdk.getSdkTracerProvider().close();
        }
    }

    /**
     * Simple composite exporter that forwards finished spans to multiple exporters.
     */
    private static class CompositeSpanExporter implements SpanExporter {
        private final SpanExporter[] delegates;

        CompositeSpanExporter(SpanExporter... delegates) {
            this.delegates = delegates;
        }

        @Override
        public CompletableResultCode export(Collection<io.opentelemetry.sdk.trace.data.SpanData> spans) {
            for (SpanExporter delegate : delegates) {
                delegate.export(spans);
            }
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode flush() {
            for (SpanExporter delegate : delegates) {
                delegate.flush();
            }
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode shutdown() {
            for (SpanExporter delegate : delegates) {
                delegate.shutdown();
            }
            return CompletableResultCode.ofSuccess();
        }
    }
}
