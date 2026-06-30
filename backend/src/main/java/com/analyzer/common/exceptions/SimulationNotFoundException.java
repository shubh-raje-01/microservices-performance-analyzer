package com.analyzer.common.exceptions;

import org.springframework.http.HttpStatus;

public class SimulationNotFoundException extends AnalyzerException {

    public SimulationNotFoundException(Long id) {
        super("Simulation not found with id: " + id,
                HttpStatus.NOT_FOUND,
                "SIMULATION_NOT_FOUND");
    }

    public SimulationNotFoundException(String scenarioName) {
        super("Simulation not found for scenario: " + scenarioName,
                HttpStatus.NOT_FOUND,
                "SIMULATION_NOT_FOUND");
    }

}
