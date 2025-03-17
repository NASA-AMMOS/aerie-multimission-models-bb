package missionmodel;

import gov.nasa.jpl.aerie.merlin.driver.*;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import missionmodel.generated.GeneratedModelType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GeometrySpawnersTest {

  private final String sc_id = "-74"; // MRO
  private final String target = "MARS";

  @Test
  void testGeometrySpawners() {
    System.out.println("testGeometrySpawners() start");
    // Create a simple plan to test spawners
    final var simulationStartTime = Instant.parse("2024-01-02T00:00:00Z");;
    final var simulationDuration = Duration.of(4, HOURS);
    final var stepSize = Duration.of(5, MINUTES);

    // Input configuration
    final Configuration geomConfig = Configuration.defaultConfiguration();

    // Add Activities to Plan
    final Map<ActivityDirectiveId, ActivityDirective> schedule = new HashMap<>();

    // AddSpacecraftEclipses spawner
    schedule.put(new ActivityDirectiveId(1L), new ActivityDirective(
      Duration.ZERO,
      "AddSpacecraftEclipses",
      Map.of("searchDuration", SerializedValue.of(simulationDuration.in(MICROSECONDS)),
        "observer", SerializedValue.of(sc_id),
        "target", SerializedValue.of("SUN"),
        "occultingBody", SerializedValue.of(target),
        "stepSize", SerializedValue.of(stepSize.in(MICROSECONDS)),
        "useDSK", SerializedValue.of(false)),
      null,
      true
    ));

    // AddOccultations spawner
    schedule.put(new ActivityDirectiveId(2L), new ActivityDirective(
      Duration.ZERO,
      "AddOccultations",
      Map.of("searchDuration", SerializedValue.of(simulationDuration.in(MICROSECONDS)),
        "observer", SerializedValue.of("DSS-24"),
        "target", SerializedValue.of(sc_id),
        "occultingBody", SerializedValue.of(target),
        "stepSize", SerializedValue.of(stepSize.in(MICROSECONDS)),
        "useDSK", SerializedValue.of(false)),
      null,
      true
    ));

    // AddPeriapsis spawner
    schedule.put(new ActivityDirectiveId(3L), new ActivityDirective(
      Duration.ZERO,
      "AddPeriapsis",
      Map.of("searchDuration", SerializedValue.of(simulationDuration.in(MICROSECONDS)),
        "body", SerializedValue.of(sc_id),
        "target", SerializedValue.of(target),
        "stepSize",SerializedValue.of(stepSize.in(MICROSECONDS)),
        "maxDistanceFilter", SerializedValue.of(10000.0)),
      null,
      true
    ));

    // AddApoapsis spawner
    schedule.put(new ActivityDirectiveId(4L), new ActivityDirective(
      Duration.ZERO,
      "AddApoapsis",
      Map.of("searchDuration", SerializedValue.of(simulationDuration.in(MICROSECONDS)),
        "body", SerializedValue.of(sc_id),
        "target", SerializedValue.of(target),
        "stepSize",SerializedValue.of(stepSize.in(MICROSECONDS)),
        "minDistanceFilter", SerializedValue.of(0.0)),
      null,
      true
    ));

    schedule.put(new ActivityDirectiveId(6L), new ActivityDirective(
      Duration.of(2, HOURS),
      "PointToTargetBody",
      Map.of("primaryTargetBodyName", SerializedValue.of("SUN"),
        "secondaryTargetBodyName", SerializedValue.of("EARTH")),
      null,
      true
    ));


    final var results = simulate(geomConfig, simulationStartTime, simulationDuration, schedule);

    // Store off activity types of all simulated activities
    ArrayList<String> act_types = new ArrayList<String>();
    for (SimulatedActivity act : results.simulatedActivities.values()) {
      act_types.add(act.type());
    }

    // Based on results from MATLAB test script (test_mro_geom.m), we expect the following number of events between
    // 2024-01-02T00:00:00Z and 2024-01-02T04:00:00Z
    // 3 Periapses
    // 2 Apoapses
    // 2 Eclipses
    // 3 Occultations
    // Note, we are using the "Exit" eclipse and occultation activities here since there may be multiple "Enter"
    // activities per event to specify transitions between partial and full events.
    assertEquals(3, Collections.frequency(act_types,"Periapsis"));
    assertEquals(2, Collections.frequency(act_types,"Apoapsis"));
    assertEquals(2, Collections.frequency(act_types,"SpacecraftExitEclipse"));
    assertEquals(3, Collections.frequency(act_types,"ExitOccultation"));

    System.out.println("testGeometrySpawners() passes");
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
