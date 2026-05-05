package com.analyzer.backend.modules.simulation.events;

import com.analyzer.backend.modules.simulation.model.Simulation;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SimulationStartedEvent extends ApplicationEvent {

    private final Simulation simulation;

    public SimulationStartedEvent(Object source, Simulation simulation) {
        super(source);
        this.simulation = simulation;
    }
}
