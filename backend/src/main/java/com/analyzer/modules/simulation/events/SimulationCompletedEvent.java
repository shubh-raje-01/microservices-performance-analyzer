package com.analyzer.modules.simulation.events;

import com.analyzer.modules.simulation.model.Simulation;
import com.analyzer.modules.simulation.model.SimulationResult;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SimulationCompletedEvent extends ApplicationEvent {

    private final Simulation simulation;
    private final SimulationResult result;

    public SimulationCompletedEvent(Object source, Simulation simulation, SimulationResult result) {
        super(source);
        this.simulation = simulation;
        this.result = result;
    }

    private Long getSimulationId() {
        return simulation.getId();
    }

    private String getTargetService() {
        return simulation.getTargetService();
    }

}
