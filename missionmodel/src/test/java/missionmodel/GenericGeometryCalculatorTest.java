package missionmodel;

import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinExtension;
import gov.nasa.jpl.time.Time;
import missionmodel.geometry.directspicecalls.SpiceDirectTimeDependentStateCalculator;
import missionmodel.geometry.interfaces.GeometryInformationNotAvailableException;
import missionmodel.geometry.resources.GenericGeometryResources;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import spice.basic.CSPICE;

import java.time.Instant;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

// The `@ExtendWith` annotation injects the given extension into JUnit's testing apparatus.
// Our `MerlinExtension` hooks test class construction and test method execution,
// executing each with the appropriate simulation context.
@ExtendWith(MerlinExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class GenericGeometryCalculatorTest {

  private final Mission model;

  private final Instant planStart;

  private SpiceDirectTimeDependentStateCalculator stateCalculator;

  // Initializers and the test class constructor are executed in an "initialization" Merlin context.
  // This means that models can be created (and cell storage allocated, and daemons spawned),
  // but simulation control actions like `waitFor`, `delay`, and `emit` cannot be performed.
  // The `Registrar` does not need to be declared as a parameter, but will be injected if declared.
  public GenericGeometryCalculatorTest(final Registrar registrar) {
    // The test kernel set has the following valid data interval for MRO, which is the limiting case
    // Body: MARS RECON ORBITER (-74)
    // Start of Interval (ET)              End of Interval (ET)
    // -----------------------------       -----------------------------
    // 2024 JAN 01 00:01:10.000            2024 MAY 06 10:40:00.000
    this.planStart = Instant.parse("2024-01-02T00:00:00Z");
    // Model configuration can be provided directly, just as for a normal Java class constructor.
    this.model = new Mission(registrar, planStart, Configuration.defaultConfiguration());
    // For performing SPICE calls directly in tests
    this.stateCalculator = new SpiceDirectTimeDependentStateCalculator(true);
  }

  @Test
  public void testCalculateGeometry() {
    // By this point the model has been initialized and we are in the simulation context at the start of the plan
    Time t = JPLTimeConvertUtility.jplTimeFromUTCInstant(planStart);
    int sc_id = -74; // MRO
    String sc_str = Integer.toString(sc_id);

    // Check all resources that the GenericGeometryCalculator calculates that aren't just a direct pass-through
    // to SPICE
    GenericGeometryResources geoRes = this.model.geometryResources;
    try {
      assertEquals(stateCalculator.getBodyHalfAngleSize(t, sc_str, "MARS", "LT+S"), currentValue(geoRes.BodyHalfAngleSize.get("MARS")), 0.01);
      assertEquals(stateCalculator.getEarthSpacecraftBodyAngle(t, sc_str, "MARS", "LT+S"), currentValue(geoRes.EarthSpacecraftBodyAngle.get("MARS")), 0.01);
      assertEquals(stateCalculator.getSunBodySpacecraftAngle(t, sc_str, "MARS", "LT+S"), currentValue(geoRes.SunBodySpacecraftAngle.get("MARS")), 0.01);
      assertEquals(stateCalculator.getSunSpacecraftBodyAngle(t, sc_str, "MARS", "LT+S"), currentValue(geoRes.SunSpacecraftBodyAngle.get("MARS")), 0.01);
      assertEquals(stateCalculator.getEarthSunProbeAngle(t, sc_str, "LT+S"), currentValue(geoRes.EarthSunProbeAngle), 0.01);
    } catch (GeometryInformationNotAvailableException e) {
      e.printStackTrace();
    }
  }

}
