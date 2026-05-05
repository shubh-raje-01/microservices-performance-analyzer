package com.analyzer.backend.modules.simulation.service;

import com.analyzer.backend.common.constants.CacheConstants;
import com.analyzer.backend.common.dto.request.AnalysisRequestDto;
import com.analyzer.backend.common.exceptions.AnalyzerException;
import com.analyzer.backend.modules.simulation.events.SimulationCompletedEvent;
import com.analyzer.backend.modules.simulation.events.SimulationFailedEvent;
import com.analyzer.backend.modules.simulation.events.SimulationStartedEvent;
import com.analyzer.backend.modules.simulation.model.Simulation;
import com.analyzer.backend.modules.simulation.model.SimulationResult;
import com.analyzer.backend.modules.simulation.model.SimulationStatus;
import com.analyzer.backend.modules.simulation.repository.SimulationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationService {

    private final SimulationRepository repository;
    private final SimulationRunner runner;
    private final ApplicationEventPublisher eventPublisher;


    @Transactional
    @CacheEvict(value = CacheConstants.SIMULATIONS_LIST, allEntries = true)
    public Simulation createAndRun(AnalysisRequestDto dto) {
        guardNoDuplicateRunning(dto.getScenarioName());

        Map<String, String> params = dto.getCustomParams() == null ? Map.of()
                : dto.getCustomParams().entrySet().stream()
                  .collect(Collectors.toMap(
                          Map.Entry::getKey,
                          e -> String.valueOf(e.getValue())
                  ));

        Simulation simulation = Simulation.builder()
                .scenarioName(dto.getScenarioName())
                .targetService(dto.getTargetService())
                .durationSeconds(dto.getDurationSeconds())
                .concurrentUsers(dto.getConcurrentUsers())
                .errorRateThreshold(dto.getErrorRateThreshold())
                .customParams(params)
                .status(SimulationStatus.PENDING)
                .build();

        Simulation saved = repository.save(simulation);
        log.info("Simulation created — id={} scenario={}",
                saved.getId(), saved.getScenarioName());

        runAsync(saved.getId());
        return saved;
    }


    @Async
    public void runAsync(Long simulationId) {
        Simulation sim = repository.findById(simulationId)
                .orElseThrow(() -> AnalyzerException.notFound("Simulation", simulationId));

        try {
            transitionToRunning(sim);
            SimulationResult result = runner.run(sim);
            transitionToCompleted(sim, result);
        } catch (Exception ex) {
            log.error("Simulation {} failed — {}", simulationId, ex.getMessage(), ex);
            transitionToFailed(sim, ex.getMessage());
        }
    }

    @Transactional
    public void transitionToRunning(Simulation sim) {
        sim.markRunning();
        repository.save(sim);
        eventPublisher.publishEvent(new SimulationStartedEvent(this, sim));
        log.info("Simulation {} → RUNNING", sim.getId());
    }

    @Transactional
    @CacheEvict(value = CacheConstants.SIMULATION_BY_ID, key = "#sim.id")
    public void transitionToCompleted(Simulation sim, SimulationResult result) {
        sim.markCompleted(result);
        repository.save(sim);
        eventPublisher.publishEvent(new SimulationCompletedEvent(this, sim, result));
        log.info("Simulation {} → COMPLETED (p95={}ms errorRate={})",
                sim.getId(), sim.getP95LatencyMs(), sim.getActualErrorRate());
    }

    @Transactional
    @CacheEvict(value = CacheConstants.SIMULATION_BY_ID, key = "#sim.id")
    public void transitionToFailed(Simulation sim, String reason) {
        sim.markFailed(reason);
        repository.save(sim);
        eventPublisher.publishEvent(new SimulationFailedEvent(this, sim, reason));
        log.warn("Simulation {} → FAILED ({})", sim.getId(), reason);
    }

    // Reads

    @Cacheable(value = CacheConstants.SIMULATION_BY_ID, key = "#id")
    public Simulation getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> AnalyzerException.notFound("Simulation", id));
    }

    public Page<Simulation> listAll(int page, int size) {
        return repository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, Math.min(size, 100))
        );
    }

    public List<Simulation> getByService(String serviceName) {
        return repository.findByTargetService(serviceName);
    }

    public List<Simulation> getRunning() {
        return repository.findByStatus(SimulationStatus.RUNNING);
    }

    //  Delete

    @Transactional
    @CacheEvict(value = {
            CacheConstants.SIMULATION_BY_ID,
            CacheConstants.SIMULATIONS_LIST
    }, allEntries = true)
    public void delete(Long id) {
        Simulation sim = getById(id);
        if (sim.getStatus() == SimulationStatus.RUNNING) {
            throw AnalyzerException.badRequest(
                    "Cannot delete a running simulation — cancel it first");
        }
        repository.delete(sim);
        log.info("Simulation {} deleted", id);
    }

    // Guards

    private void guardNoDuplicateRunning(String scenarioName) {
        if (repository.existsByScenarioNameAndStatus(
                scenarioName, SimulationStatus.RUNNING)) {
            throw AnalyzerException.badRequest(
                    "Scenario '" + scenarioName + "' is already running");
        }
    }

}
