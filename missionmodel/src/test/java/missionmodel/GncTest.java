package missionmodel;

import gov.nasa.jpl.aerie.merlin.driver.*;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.types.ActivityDirective;
import gov.nasa.jpl.aerie.types.ActivityDirectiveId;
import missionmodel.generated.GeneratedModelType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import spice.basic.CSPICE;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.HOURS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GncTest {

  @Test
  void testSimulation() {
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

    final var results = simulate(geomConfig, simulationStartTime, simulationDuration, schedule);
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

