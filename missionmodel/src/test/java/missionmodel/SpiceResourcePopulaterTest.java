package missionmodel;

import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinExtension;
import gov.nasa.jpl.time.Duration;
import gov.nasa.jpl.time.Time;
import missionmodel.geometry.spiceinterpolation.Body;
import missionmodel.geometry.spiceinterpolation.CalculationPeriod;
import missionmodel.geometry.spiceinterpolation.SpiceResourcePopulater;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

import spice.basic.CSPICE;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static missionmodel.Debug.debug;

// The `@ExtendWith` annotation injects the given extension into JUnit's testing apparatus.
// Our `MerlinExtension` hooks test class construction and test method execution,
// executing each with the appropriate simulation context.
@ExtendWith(MerlinExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class SpiceResourcePopulaterTest {

  private final Mission model;

  // Initializers and the test class constructor are executed in an "initialization" Merlin context.
  // This means that models can be created (and cell storage allocated, and daemons spawned),
  // but simulation control actions like `waitFor`, `delay`, and `emit` cannot be performed.
  // The `Registrar` does not need to be declared as a parameter, but will be injected if declared.
  public SpiceResourcePopulaterTest(final Registrar registrar) {
    // The test kernel set has the following valid data interval for MRO, which is the limiting case
    // Body: MARS RECON ORBITER (-74)
    // Start of Interval (ET)              End of Interval (ET)
    // -----------------------------       -----------------------------
    // 2024 JAN 01 00:01:10.000            2024 MAY 06 10:40:00.000
    Instant planStart = Instant.parse("2024-01-02T00:00:00Z");
    // Model configuration can be provided directly, just as for a normal Java class constructor.
    this.model = new Mission(registrar, planStart, Configuration.defaultConfiguration());
  }

  @Test
  public void testInitializeAllBodiesFromJson() {
    if (debug) System.out.println("testInitializeAllBodiesFromJson() start");
    // The bodies in the test data set (default_geometry_config.json) against which MRO geometry will be calculated are
    // the Sun (10), Earth (399), and Mars (499). Each body has an associated body-fixed frame, iau_<body>. The test
    // data set only requests that most geometric quantities (e.g. Altitude, LST) be computed against Mars.
    HashMap<String, Body> bodies = this.model.spiceResPop.getBodies();
    assertEquals("IAU_SUN", bodies.get("SUN").getNAIFBodyFrame());
    assertEquals("IAU_EARTH", bodies.get("EARTH").getNAIFBodyFrame());
    assertEquals("IAU_MARS", bodies.get("MARS").getNAIFBodyFrame());
    assertEquals(false, bodies.get("SUN").doCalculateAltitude());
    assertEquals(false, bodies.get("EARTH").doCalculateAltitude());
    assertEquals(true, bodies.get("MARS").doCalculateAltitude());
    assertEquals(true, bodies.get("MARS").doCalculateSubSCPoint());
    assertEquals(true, bodies.get("MARS").doCalculateBetaAngle());
    assertEquals(true, bodies.get("MARS").doCalculateLST());
    assertEquals(true, bodies.get("MARS").doCalculateEarthSpacecraftBodyAngle());
    assertEquals(true, bodies.get("MARS").doCalculateIlluminationAngles());
    assertEquals(true, bodies.get("MARS").doCalculateOrbitParameters());
    assertEquals(true, bodies.get("MARS").doCalculateRaDec());
    assertEquals(true, bodies.get("MARS").doCalculateSubSolarInformation());
    assertEquals(false, bodies.get("MARS").useDSK());
    if (debug) System.out.println("testInitializeAllBodiesFromJson() passes");
  }

  @Test
  public void testGetCalculationPeriods() {
    if (debug) System.out.println("testGetCalculationPeriods() start");
    // Make it easier to work with the populater
    SpiceResourcePopulater pop = this.model.spiceResPop;

    // Create a data gap sometime during valid SPICE interval of the default data
    Window[] dataGaps = new Window[]{
      new Window(new Time("2024-01-14T00:00:00"), new Time("2024-01-15T00:00:00")),
    };
    Window dataGap = dataGaps[0];
    Duration dataPad = new Duration("00:15:00");
    // Add data gaps into resource populater
    pop.setDataGaps(dataGaps, dataPad);

    List<CalculationPeriod> calPeriods;

    // There is only one calculation period specified for each body in test data set (default_geometry_config.json):
    // "calculationPeriods": [
    //          {
    //            "begin": "2024-01-02T00:00:00.000",
    //            "end": "2024-05-06T00:00:00.000",
    //            "minTimeStep": "00:00:30",
    //            "maxTimeStep": "00:00:30",
    //            "threshold": 0.1
    //          }
    //        ]
    // But, the addition of a data gap will create a second calculation period. The end time of the first period will be
    // at the gap time minus the padding and the start time of the second period will be at the end of the gap plus the
    // padding
    calPeriods = pop.getCalculationPeriods("MARS", "Trajectory");
    assertEquals(2, calPeriods.size());
    assertEquals(new Time("2024-01-02T00:00:00.000"), calPeriods.get(0).getStart());
    assertEquals(dataGap.getStart().subtract(dataPad), calPeriods.get(0).getEnd());
    assertEquals(dataGap.getEnd().add(dataPad), calPeriods.get(1).getStart());
    assertEquals(new Time("2024-05-06T00:00:00.000"),    calPeriods.get(1).getEnd());
    assertEquals(new Duration("00:00:30"), calPeriods.get(0).getMinTimeStep());
    assertEquals(new Duration("00:00:30"), calPeriods.get(0).getMaxTimeStep());
    assertEquals(0.1, calPeriods.get(0).getThreshold());

    calPeriods = pop.getCalculationPeriods("MARS", "Apoapsis");
    assertEquals(0, calPeriods.size());

    calPeriods = pop.getCalculationPeriods("MARS", "Periapsis");
    assertEquals(0, calPeriods.size());

    calPeriods = pop.getCalculationPeriods("MARS", "SolarEclipses");
    assertEquals(0, calPeriods.size());

    calPeriods = pop.getCalculationPeriods("MARS", "Occultations");
    assertEquals(0, calPeriods.size());
    if (debug) System.out.println("testGetCalculationPeriods() passes");
  }
}
