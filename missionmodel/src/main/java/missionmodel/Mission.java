package missionmodel;


import gov.nasa.jpl.aerie.contrib.streamline.core.Resources;
import gov.nasa.jpl.aerie.contrib.streamline.debugging.Logging;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.time.Duration;
import missionmodel.geometry.resources.GenericGeometryResources;
import missionmodel.geometry.spiceinterpolation.GenericGeometryCalculator;
import missionmodel.geometry.spiceinterpolation.SpiceResourcePopulater;
import missionmodel.gnc.GncDataModel;
import missionmodel.spice.Spice;
import spice.basic.SpiceErrorException;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

/**
 * Top-level Mission Model Class
 *
 * Declare, define, and register resources within this class or its delegates
 * Spawn daemon tasks using spawn(objectName::nameOfMethod) within the class constructor
 */
public final class Mission {

  public final Configuration configuration;

  // Special registrar class that handles simulation errors via auto-generated resources
  public final Registrar registrar;

  public final AbsoluteClock absoluteClock;

  public final GenericGeometryCalculator geometryCalculator;

  public final SpiceResourcePopulater spiceResPop;

  public final GenericGeometryResources geometryResources;

  public static final Integer SPICE_SCID = -74; // MRO

  public static final Path VERSIONED_KERNELS_ROOT_DIRECTORY = Path.of(System.getenv().getOrDefault("SPICE_DIRECTORY", "spice/kernels"));

  public static final String NAIF_META_KERNEL_PATH = VERSIONED_KERNELS_ROOT_DIRECTORY + "/latest_meta_kernel.tm";


  // --------------------------------
  // GNC Model Variables
  public GncDataModel gncDataModel;

  public Mission(final gov.nasa.jpl.aerie.merlin.framework.Registrar registrar, final Instant planStart, final Configuration config) {
    Logging.LOGGER = null;
    this.configuration = config;
    this.registrar = new Registrar(registrar, Registrar.ErrorBehavior.Log);
    this.absoluteClock = new AbsoluteClock(planStart);

    try {
      Spice.initialize(NAIF_META_KERNEL_PATH);
    }
    catch (SpiceErrorException e) {
      System.out.println(e.getMessage());
    }

    // Initialize Geometry Model
    this.geometryCalculator = new GenericGeometryCalculator(this.absoluteClock, SPICE_SCID, "LT+S", planStart, configuration.useLinearResources(), Optional.of(this.registrar));
    // Assume no gaps in SPICE data for now
    this.spiceResPop = new SpiceResourcePopulater(this.geometryCalculator, this.absoluteClock, new Window[]{}, Duration.ZERO_DURATION, config.geometryPath().toString());
    this.geometryResources = this.geometryCalculator.getResources();
    if (!config.useLinearResources()) {
      this.spiceResPop.calculateTimeDependentInformation();
    }

    // --------------------------------
    // GNC Model Integration
    this.gncDataModel = new GncDataModel(this.registrar);
  }
}
