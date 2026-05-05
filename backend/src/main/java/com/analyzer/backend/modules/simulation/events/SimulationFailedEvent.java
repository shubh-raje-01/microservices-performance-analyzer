package com.analyzer.backend.modules.simulation.events;

import com.analyzer.backend.modules.simulation.model.Simulation;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SimulationFailedEvent extends ApplicationEvent {

    private final Simulation simulation;
    private final String reason;

    public SimulationFailedEvent(Object source, Simulation simulation, String reason) {
        super(source);
        this.simulation = simulation;
        this.reason = reason;
    }

}
