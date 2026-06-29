package com.analyzer.backend.modules.ai.events;

import com.analyzer.backend.modules.ai.adapter.FastAPIResponse;
import com.analyzer.backend.modules.ai.model.AIInsight;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AIAnalysisCompletedEvent extends ApplicationEvent {

    private final AIInsight      insight;
    private final FastAPIResponse response;

    public AIAnalysisCompletedEvent (
            Object source,
            AIInsight insight,
            FastAPIResponse response
    ) {
        super(source);
        this.insight = insight;
        this.response = response;
    }

    public Long getSimulationId() {
        return insight.getSimulationId();
    }

    public boolean hasAnomaly() {
        return insight.hasAnomaly();
    }

    public boolean isDegraded() {
        return insight.isDegraded();
    }
}