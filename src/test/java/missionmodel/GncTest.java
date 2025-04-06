package missionmodel;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import missionmodel.generated.GeneratedModelType;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.DirectiveTypeRegistry;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelBuilder;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.junit.jupiter.api.TestInstance;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.HOURS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static missionmodel.Debug.debug;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GncTest {

  @Test
  void testSimulation() {
    if (debug) System.out.println("testSimulation() start");
    final var simulationStartTime = Instant.parse("2024-01-02T00:00:00Z");
    final var simulationDuration = Duration.of(96, HOURS);

    // Input configuration
    final Configuration geomConfig = Configuration.defaultConfiguration();

    // Add Activities to Plan
    final Map<ActivityDirectiveId, ActivityDirective> schedule = new HashMap<>();

        schedule.put(new ActivityDirectiveId(1L), new ActivityDirective(
                Duration.of(10, SECONDS),
                "PointToTargetBody",
                Map.of("primaryTargetBodyName", SerializedValue.of("MARS"),
                       "secondaryTargetBodyName", SerializedValue.of("SUN"),
                       "primaryObserverString", SerializedValue.of("X"),
                       "secondaryObserverString", SerializedValue.of("Z")),
                null,
                true
        ));

    Timer.reset();
    var testTimer = new Timer("testSimulation", false);
    final var results = simulate(geomConfig, simulationStartTime, simulationDuration, schedule);
    testTimer.stop(false);
    if (debug) System.out.println("testSimulation()        CPU time: " + (testTimer.accumulatedCpuTime/1e9) + " seconds");
    if (debug) System.out.println("testSimulation() wall clock time: " + (testTimer.accumulatedWallClockTime/1e9) + " seconds");
    if (debug) System.out.println("testSimulation() passes");
  }

  @Test
  void testSimulationLinear() {
    if (debug) System.out.println("testSimulationLinear() start");
    final var simulationStartTime = Instant.parse("2024-01-02T00:00:00Z");
    final var simulationDuration = Duration.of(96, HOURS);

    // Input configuration
    final Configuration geomConfig = Configuration.linearConfiguration();

    // Add Activities to Plan
    final Map<ActivityDirectiveId, ActivityDirective> schedule = new HashMap<>();

    schedule.put(new ActivityDirectiveId(1L), new ActivityDirective(
      Duration.of(10, SECONDS),
      "PointToTargetBody",
      Map.of("primaryTargetBodyName", SerializedValue.of("MARS"),
        "secondaryTargetBodyName", SerializedValue.of("SUN"),
        "primaryObserverString", SerializedValue.of("X"),
        "secondaryObserverString", SerializedValue.of("Z")),
      null,
      true
    ));

    Timer.reset();
    var testTimer = new Timer("testSimulationLinear", false);
    final var results = simulate(geomConfig, simulationStartTime, simulationDuration, schedule);
    testTimer.stop(false);
    if (debug) System.out.println("testSimulationLinear()        CPU time: " + (testTimer.accumulatedCpuTime/1e9) + " seconds");
    if (debug) System.out.println("testSimulationLinear() wall clock time: " + (testTimer.accumulatedWallClockTime/1e9) + " seconds");
    if (debug) System.out.println("testSimulationLinear() passes");
  }


  public SimulationResults simulate(
    Configuration configuration,
    Instant simulationStartTime,
    Duration simulationDuration,
    Map<ActivityDirectiveId, ActivityDirective> schedule
  ) {
    return SimulationDriver.simulate(
      makeMissionModel(new MissionModelBuilder(), simulationStartTime, configuration),
      schedule,
      simulationStartTime,
      simulationDuration,
      simulationStartTime,
      simulationDuration,
      () -> { return false; }
    );
  }

  private static MissionModel<?> makeMissionModel(final MissionModelBuilder builder, final Instant planStart, final Configuration config) {
    final var factory = new GeneratedModelType();
    final var registry = DirectiveTypeRegistry.extract(factory);
    final var model = factory.instantiate(planStart, config, builder);
    return builder.build(model, registry);
  }
}

