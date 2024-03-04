package missionmodel;

// import gov.nasa.jpl.aerie.contrib.serialization.mappers.DoubleValueMapper;
// import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.time.Duration;
import missionmodel.geometry.interfaces.GeometryInformationNotAvailableException;
import missionmodel.geometry.resources.GenericGeometryResources;
import missionmodel.geometry.spiceinterpolation.Body;
import missionmodel.geometry.spiceinterpolation.BodyGeometryGenerator;
import missionmodel.geometry.spiceinterpolation.GenericGeometryCalculator;
import missionmodel.spice.Spice;
import spice.basic.SpiceErrorException;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.spawn;
// import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;

// import static gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource.resource;
// import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;

/**
 * Top-level Mission Model Class
 *
 * Declare, define, and register resources within this class or its delegates
 * Spawn daemon tasks using spawn(objectName::nameOfMethod) within the class constructor
 */
public final class Mission {

  // Special registrar class that handles simulation errors via auto-generated resources
  public final Registrar errorRegistrar;

  public final AbsoluteClock absoluteClock;

  public final GenericGeometryResources geometryResources;
  public final GenericGeometryCalculator geometryCalculator;

  public static final Path VERSIONED_KERNELS_ROOT_DIRECTORY = Path.of(System.getenv().getOrDefault("SPICE_DIRECTORY", "spice/kernels"));

  public static final String NAIF_META_KERNEL_PATH = VERSIONED_KERNELS_ROOT_DIRECTORY.toString() + "/latest_meta_kernel.tm";

  // Example resource declaration
  // public MutableResource<Discrete<Double>> ExampleResource;

  public Mission(final gov.nasa.jpl.aerie.merlin.framework.Registrar registrar, final Instant planStart, final Configuration config) {
    this.errorRegistrar = new Registrar(registrar, Registrar.ErrorBehavior.Log);
    this.absoluteClock = new AbsoluteClock(planStart);

    //Map<String,Object> bodies = new HashMap<>()
    //Map<String, Object> body = new HashMap<String>();

    try {
      Spice.initialize(NAIF_META_KERNEL_PATH);
    }
    catch (SpiceErrorException e) {
      System.out.println(e.getMessage());
    }

    // Initialize Bodies
    Body sun = new Body("SUN", 10, "IAU_SUN", 1.0);
    Body earth = new Body("EARTH", 399, "IAU_EARTH", 0.3);
    Body jupiter = new Body("JUPITER", 599, "IAU_JUPITER", 0.34,
      true, true, true, true, true,
      false, false, false, false, false);

    HashMap<String, Body> bodies = new HashMap<>();
    bodies.put("SUN", sun);
    bodies.put("EARTH", earth);
    bodies.put("JUPITER", jupiter);

    // Spacecraft ID
    Integer juno_sc_id = -61;

    this.geometryResources = new GenericGeometryResources(errorRegistrar, bodies);
    this.geometryCalculator = new GenericGeometryCalculator(this.absoluteClock, this.geometryResources, juno_sc_id, "LT+S" );
    this.geometryCalculator.setBodies(bodies);

    for(Body body : bodies.values()){
//      List<CalculationPeriod> calculationPeriods = getCalculationPeriods(body.getName(), "Trajectory", Duration.ZERO_DURATION);
//      for(CalculationPeriod calculationPeriod : calculationPeriods){
      BodyGeometryGenerator bodyGeoGenerator = new BodyGeometryGenerator(
        absoluteClock, this.geometryResources, JPLTimeConvertUtility.jplTimeFromUTCInstant(planStart), body.getName(),
        0.1, Duration.HOUR_DURATION, Duration.HOUR_DURATION, "", this.geometryCalculator, bodies);
      spawn(bodyGeoGenerator::model);
    }

    //this.bodyGeometryGenerator

    //spawn()

    // Example resource definition and registration
    // ExampleResource = resource(discrete(0.0));
    // errorRegistrar.discrete("ExampleResource", ExampleResource, new DoubleValueMapper());

  }
}
