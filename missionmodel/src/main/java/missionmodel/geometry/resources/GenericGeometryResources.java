package missionmodel.geometry.resources;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.*;
import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.Approximation;

import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.SecantApproximation;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.Unstructured;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.monads.UnstructuredResourceApplicative;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import missionmodel.geometry.returnedobjects.RADec;
import missionmodel.geometry.spiceinterpolation.Body;
import missionmodel.geometry.spiceinterpolation.GenericGeometryCalculator;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import static gov.nasa.jpl.aerie.contrib.metadata.UnitRegistrar.withUnit;
import static gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource.resource;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.IntervalFunctions.byUniformSampling;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.UnstructuredResources.approximateAsLinear;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.map;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.assumeLinear;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.constant;
import static missionmodel.JPLTimeConvertUtility.getDuration;

/**
 * This class instantiates a variety of geometry resources for a collection of bodies (e.g., Sun, Earth, Mars)
 * that the user describes in default_geometry_config.json and an assumed spacecraft.  There are {@link Discrete},
 * {@link Linear} and {@link Unstructured} versions of the resources.  A flag from a {@link GenericGeometryCalculator}
 * determines whether to register and compute discrete vs linear resources.  The unstructured resources do not add
 * computation.  Resources suffixed with "_u" are unstructured, "_p" are linear (polynomial), "_a" are also linear in
 * an array for vector components.
 */
public class GenericGeometryResources {

  private static final boolean optimizeSampling = false;

  /**
   * Set this true and registerTimeBased true and registerDiscrete false
   */
  private final boolean linearTimeBased;
  private final boolean registerDiscrete;

  public static final double FLOAT_EPSILON = 0.0001;
  private static Map<String, Body> bodyObjects;

  //private static String[] cartesian = new String[]{"x","y","z"};
  private static final String[] illumAngles = new String[]{"phase","incidence","emission"};
  private static final String[] raDecIndices = new String[]{"Ra", "Dec"};
  //private static String[] latLonCoordinates = new String[]{"latitude","longitude", "radius"};
  private static final String[] subSCIndices = new String[]{"dist","latitude","longitude","radius","LST"};

  private static final String EARTH = "EARTH";

  public static Map<String, String> ComplexRepresentativeStation = new HashMap<>();
  static{
    ComplexRepresentativeStation.put("Goldstone", "DSS-24");
    ComplexRepresentativeStation.put("Canberra", "DSS-36");
    ComplexRepresentativeStation.put("Madrid", "DSS-54");
  }

  private final DoubleResource upleg_time;
  private final DoubleResource downleg_time;
  private final DoubleResource rtlt;
  private final Map<String, List<Resource<Linear>>> bodyPositionAndVelocityWRTSpacecraft;
  private final Map<String, List<Resource<Linear>>> sunPositionAndVelocityWRTBody;
  private final Map<String, List<Resource<Linear>>> bodyPositionAndVelocityWRTEarth;

  private final Resource<Unstructured<RADec>> radec_u;
  private final DoubleResource spacecraftDeclination;
  private final DoubleResource spacecraftRightAscension;
  //private final Map<String, DoubleResource> BODY_POS_ICRF;
  private final Map<String, MutableResource<Discrete<Vector3D>>> BODY_POS_ICRF;
  private final Map<String, MutableResource<Discrete<Vector3D>>> BODY_VEL_ICRF;
  private final Map<String, DoubleResource> SpacecraftBodyRange;
  private final Map<String, DoubleResource> SpacecraftBodySpeed;
  private final Map<String, DoubleResource> SunSpacecraftBodyAngle;
  private final Map<String, DoubleResource> SunBodySpacecraftAngle;
  private final Map<String, DoubleResource> BodyHalfAngleSize;
  private final Map<String, DoubleResource> BetaAngleByBody;

  private final Map<String, MutableResource<Discrete<Double>>> EarthSpacecraftBodyAngle;

  private final DoubleResource EarthSunProbeAngle;

  private final Map<String, DoubleResource> SpacecraftAltitude;

  private final Map<String, Map<String, MutableResource<Discrete<Double>>>> IlluminationAnglesByBody;
  private final Map<String, Map<String, MutableResource<Discrete<Double>>>> EarthRaDecByBody;

  private final Map<String, MutableResource<Discrete<Double>>> EarthRaDeltaWithSCByBody;

  private final Map<String, MutableResource<Discrete<Vector3D>>> BodySubSolarPoint;
  private final Map<String, Map<String, MutableResource<Discrete<Double>>>> BodySubSCPoint;

  private final Map<String, MutableResource<Discrete<EclipseTypes>>> SpacecraftEclipseByBody;
  private final MutableResource<Discrete<EclipseTypes>> AnySpacecraftEclipse;

  private final Map<String, Map<String, MutableResource<Discrete<Boolean>>>> SpacecraftOccultationByBodyAndStation;
  private final MutableResource<Discrete<Integer>> Occultation;
  private final MutableResource<Discrete<Double>> FractionOfSunNotInEclipse;
  private final MutableResource<Discrete<Integer>> LitOrDarkSide;

  private final Map<String, MutableResource<Discrete<Double>>> orbitInclinationByBody;
  private final Map<String, MutableResource<Discrete<Double>>> orbitPeriodByBody;



  public Map<String, MutableResource<Discrete<Boolean>>> Periapsis;
  public Map<String, MutableResource<Discrete<Boolean>>> Apoapsis;

  public static DoubleValueMapper dvm = new DoubleValueMapper();
  public static BooleanValueMapper bvm = new BooleanValueMapper();
  public static IntegerValueMapper ivm = new IntegerValueMapper();

  private final GenericGeometryCalculator geometryCalculator;

  public final Duration spiceStart;

  /**
   * Instantiate the collection of geometry resources
   * @param reg an optional registrar; if empty, there will be no registrations; this can be useful if wanting to register
   *            with different names or for a subset of resources for efficiency.
   * @param allBodies the set of bodies for which resources will be generated
   * @param geometryCalculator the class for calculating resources, necessary for defining some resources and sharing
   *                           the time of the start of SPICE data and whether to use linear or discrete resources
   */
  public GenericGeometryResources(Registrar reg, Map<String, Body> allBodies, GenericGeometryCalculator geometryCalculator) {
    this.geometryCalculator = geometryCalculator;
    this.spiceStart = geometryCalculator.spiceStart;
    linearTimeBased = geometryCalculator.useLinearResources;
    registerDiscrete = !geometryCalculator.useLinearResources;
    bodyObjects = allBodies;
    String[] bodies = Body.getNamesOfBodies(allBodies);
    List<String> earthSpacecraftBodies = Body.getEarthSCBodies(allBodies);
    List<String> altitudeBodies = Body.getAltitudeBodies(allBodies);
    List<String> illuminationBodies = Body.getIlluminationAngleBodies(allBodies);
    List<String> raDecBodies = Body.getRaDecBodies(allBodies);
    List<String> subSolarBodies = Body.getSubSolarBodies(allBodies);
    List<String> subSCBodies = Body.getRadiatorAvoidanceBodies(allBodies);
    List<String> betaAngleBodies = Body.getBetaAngleBodies(allBodies);
    List<String> orbitParameterBodies = Body.getOrbitParameterBodies(allBodies);

    // Initialize resources
    bodyPositionAndVelocityWRTSpacecraft = new HashMap<>();
    Map<String, Resource<Linear>[][]> bodyPositionAndVelocityWRTSpacecraft_a = new HashMap<>();
    Map<String, Resource<Unstructured<Vector3D[]>>> bodyPositionAndVelocityWRTSpacecraft_u = new HashMap<>();
    sunPositionAndVelocityWRTBody = new HashMap<>();
    Map<String, Resource<Linear>[][]> sunPositionAndVelocityWRTBody_a = new HashMap<>();
    Map<String, Resource<Unstructured<Vector3D[]>>> sunPositionAndVelocityWRTBody_u = new HashMap<>();
    bodyPositionAndVelocityWRTEarth = new HashMap<>();
    Map<String, Resource<Linear>[][]> bodyPositionAndVelocityWRTEarth_a = new HashMap<>();
    Map<String, Resource<Unstructured<Vector3D[]>>> bodyPositionAndVelocityWRTEarth_u = new HashMap<>();
    BODY_POS_ICRF = new HashMap<>();
    Map<String, Resource<Linear>[]> BODY_POS_ICRF_a = new HashMap<>();
    Map<String, Resource<Unstructured<Vector3D>>> BODY_POS_ICRF_u = new HashMap<>();
    BODY_VEL_ICRF = new HashMap<>();
    Map<String, Resource<Linear>[]> BODY_VEL_ICRF_a = new HashMap<>();
    Map<String, Resource<Unstructured<Vector3D>>> BODY_VEL_ICRF_u = new HashMap<>();
    SpacecraftBodyRange = new HashMap<>();
    SpacecraftBodySpeed = new HashMap<>();
    SunSpacecraftBodyAngle = new HashMap<>();
    SunBodySpacecraftAngle = new HashMap<>();
    BodyHalfAngleSize = new HashMap<>();
    IlluminationAnglesByBody = new HashMap<>();
    EarthRaDecByBody = new HashMap<>();
    BodySubSCPoint = new HashMap<>();
    SpacecraftOccultationByBodyAndStation = new HashMap<>();
    SpacecraftEclipseByBody = new HashMap<>();
    BetaAngleByBody = new HashMap<>();
    EarthSpacecraftBodyAngle = new HashMap<>();
    SpacecraftAltitude = new HashMap<>();
    EarthRaDeltaWithSCByBody = new HashMap<>();
    BodySubSolarPoint = new HashMap<>();
    orbitInclinationByBody = new HashMap<>();
    orbitPeriodByBody = new HashMap<>();

    Apoapsis = new HashMap<>();
    Periapsis = new HashMap<>();

    boolean linear = geometryCalculator.useLinearResources;

    // Non-arrayed resources
    var upleg_time_d = resource(discrete(0.0));
    var upleg_time_u = resource(Unstructured.timeBased(fit(geometryCalculator::upleg_duration)));
    var upleg_time_p = !linear ? null : maybeApproximateAsLinear(upleg_time_u, EARTH);
    register_p(reg, "upleg_time", upleg_time_d, upleg_time_p, dvm);
    upleg_time = new DoubleResource(upleg_time_d, upleg_time_u, upleg_time_p);

    var downleg_time_d = resource(discrete(0.0));
    var downleg_time_u = resource(Unstructured.timeBased(fit(geometryCalculator::downleg_duration)));
    var downleg_time_p = !linear ? null : maybeApproximateAsLinear(downleg_time_u, EARTH);
    register_p(reg, "downleg_time", downleg_time_d, downleg_time_p, dvm);
    downleg_time = new DoubleResource(downleg_time_d, downleg_time_u, downleg_time_p);

    var rtlt_d = resource(discrete(0.0));
    var rtlt_u = resource(Unstructured.timeBased(fit(t -> {
      Double ult = geometryCalculator.upleg_duration(t) * 1e6;
      var dlt = geometryCalculator.downleg_duration(t.plus(ult.longValue(), Duration.MICROSECONDS)) * 1e6;
      return (ult + dlt)/1e6;
    })));
    var rtlt_p = !linear ? null : maybeApproximateAsLinear(rtlt_u, EARTH);
    rtlt = new DoubleResource(rtlt_d, rtlt_u, rtlt_p);

    radec_u = resource(Unstructured.timeBased(fit(geometryCalculator::scRADec)));
    var spacecraftDeclination_d = resource(discrete(0.0));
    var spacecraftDeclination_u = UnstructuredResourceApplicative.map(radec_u, RADec::getDec);
    var spacecraftDeclination_p = maybeApproximateAsLinear(spacecraftDeclination_u, EARTH);
    register_p(reg, "spacecraftDeclination", spacecraftDeclination_d, spacecraftDeclination_p, dvm);
    spacecraftDeclination = new DoubleResource(spacecraftDeclination_d, spacecraftDeclination_u, spacecraftDeclination_p);

    var spacecraftRightAscension_d = resource(discrete(0.0));
    var spacecraftRightAscension_u = UnstructuredResourceApplicative.map(radec_u, RADec::getRA);
    var spacecraftRightAscension_p = maybeApproximateAsLinear(spacecraftRightAscension_u, EARTH);
    register_p(reg, "spacecraftRightAscension", spacecraftRightAscension_d, spacecraftRightAscension_p, dvm);
    spacecraftRightAscension = new DoubleResource(spacecraftRightAscension_d, spacecraftRightAscension_u, spacecraftRightAscension_p);

    var EarthSunProbeAngle_d = resource(discrete(0.0));
    var EarthSunProbeAngle_u = resource(Unstructured.timeBased(fit(geometryCalculator::earthSunProbeAngle)));
    var EarthSunProbeAngle_p = maybeApproximateAsLinear(EarthSunProbeAngle_u, EARTH);
    register_p(reg, "EarthSunProbeAngle", EarthSunProbeAngle_d, EarthSunProbeAngle_p, dvm);
    EarthSunProbeAngle = new DoubleResource(EarthSunProbeAngle_d, EarthSunProbeAngle_u, EarthSunProbeAngle_p);

    AnySpacecraftEclipse = resource(discrete(EclipseTypes.NONE));
    if (reg != null) reg.discrete("AnySpacecraftEclipse", AnySpacecraftEclipse, new EnumValueMapper<>(EclipseTypes.class));

    Occultation = resource(discrete(0));
    if (reg != null) reg.discrete("Occultation", Occultation, ivm);

    FractionOfSunNotInEclipse = resource(discrete(1.0));
    if (reg != null) reg.discrete("FractionOfSunNotInEclipse", FractionOfSunNotInEclipse, dvm);

    LitOrDarkSide = resource(discrete(0));
    if (reg != null) reg.discrete("LitOrDarkSide", LitOrDarkSide, ivm);

    // loop through bodies to build and register arrayed resources
    for (String body : bodies) {
      makePositionAndVelocityResources(bodyPositionAndVelocityWRTSpacecraft_u, bodyPositionAndVelocityWRTSpacecraft,
              bodyPositionAndVelocityWRTSpacecraft_a, body, geometryCalculator::bodyPositionAndVelocityWRTSpacecraft);
      BODY_POS_ICRF_a.put(body, bodyPositionAndVelocityWRTSpacecraft_a.get(body)[0]);
      BODY_VEL_ICRF_a.put(body, bodyPositionAndVelocityWRTSpacecraft_a.get(body)[1]);
      makePositionAndVelocityResources(sunPositionAndVelocityWRTBody_u, sunPositionAndVelocityWRTBody,
              sunPositionAndVelocityWRTBody_a, body, geometryCalculator::sunPositionAndVelocityWRTBody);
      makePositionAndVelocityResources(bodyPositionAndVelocityWRTEarth_u, bodyPositionAndVelocityWRTEarth,
              bodyPositionAndVelocityWRTEarth_a, body, geometryCalculator::sunPositionAndVelocityWRTBody);

      BODY_POS_ICRF.put(body, resource(discrete(Vector3D.MINUS_K)));
      BODY_POS_ICRF_u.put(body, UnstructuredResourceApplicative.map(bodyPositionAndVelocityWRTSpacecraft_u.get(body), u -> u[0]));
      registerUV(reg, "BODY_POS_ICRF_" + body, BODY_POS_ICRF.get(body), BODY_POS_ICRF_u.get(body), body);

      BODY_VEL_ICRF.put(body, resource(discrete(Vector3D.MINUS_K)));
      BODY_VEL_ICRF_u.put(body, UnstructuredResourceApplicative.map(bodyPositionAndVelocityWRTSpacecraft_u.get(body), u -> u[1]));
      registerUV(reg, "BODY_VEL_ICRF_" + body, BODY_VEL_ICRF.get(body), BODY_VEL_ICRF_u.get(body), body);


      var spacecraftBodyRange_d = resource(discrete(0.0));
      Resource<Linear> spacecraftBodyRange_p = BODY_POS_ICRF_a.get(body)[3];
      SpacecraftBodyRange.put(body, new DoubleResource(spacecraftBodyRange_d, null, spacecraftBodyRange_p));
      register_p(reg, "SpacecraftBodyRange_" + body,
        spacecraftBodyRange_d, spacecraftBodyRange_p, withUnit("km", dvm));

      var spacecraftBodySpeed_d = resource(discrete(0.0));
      Resource<Linear> spacecraftBodySpeed_p = BODY_VEL_ICRF_a.get(body)[3];
      SpacecraftBodySpeed.put(body, new DoubleResource(spacecraftBodySpeed_d, null, spacecraftBodySpeed_p));
      register_p(reg, "SpacecraftBodySpeed_" + body,
        spacecraftBodySpeed_d, spacecraftBodySpeed_p, withUnit("km/s", dvm));

      var sunSpacecraftBodyAngle_d = resource(discrete(0.0));
      var sunSpacecraftBodyAngle_u = resource(Unstructured.timeBased(t -> geometryCalculator.sunSpacecraftBodyAngle(t, body)));
      var sunSpacecraftBodyAngle_p = maybeApproximateAsLinear(sunSpacecraftBodyAngle_u, body);
      SunSpacecraftBodyAngle.put(body, new DoubleResource(sunSpacecraftBodyAngle_d, sunSpacecraftBodyAngle_u, sunSpacecraftBodyAngle_p));
      register_p(reg, "SunSpacecraftBodyAngle_" + body, sunSpacecraftBodyAngle_d, sunSpacecraftBodyAngle_p, withUnit("deg", dvm));

      var sunBodySpacecraftAngle_d = resource(discrete(0.0));
      var sunBodySpacecraftAngle_u = resource(Unstructured.timeBased(t -> geometryCalculator.sunBodySpacecraftAngle(t, body)));
      var sunBodySpacecraftAngle_p = maybeApproximateAsLinear(sunBodySpacecraftAngle_u, body);
      SunBodySpacecraftAngle.put(body, new DoubleResource(sunBodySpacecraftAngle_d, sunBodySpacecraftAngle_u, sunBodySpacecraftAngle_p));
      register_p(reg, "SunBodySpacecraftAngle_" + body, sunBodySpacecraftAngle_d, sunBodySpacecraftAngle_p, withUnit("deg", dvm));

      var bodyHalfAngleSize_d = resource(discrete(0.0));
      var bodyHalfAngleSize_u = resource(Unstructured.timeBased(t -> geometryCalculator.bodyHalfAngleSize(t, body)));
      var bodyHalfAngleSize_p = maybeApproximateAsLinear(bodyHalfAngleSize_u, body);
      BodyHalfAngleSize.put(body, new DoubleResource(bodyHalfAngleSize_d, bodyHalfAngleSize_u, bodyHalfAngleSize_p));
      register_p(reg, "BodyHalfAngleSize_" + body, bodyHalfAngleSize_d, bodyHalfAngleSize_p, withUnit("deg", dvm));

      if (betaAngleBodies.contains(body)) {
        var betaAngleByBody_d = resource(discrete(0.0));
        var betaAngleByBody_u = resource(Unstructured.timeBased(t -> geometryCalculator.betaAngleByBody(t, body)));
        var betaAngleByBody_p = maybeApproximateAsLinear(betaAngleByBody_u, body);
        BetaAngleByBody.put(body, new DoubleResource(betaAngleByBody_d, betaAngleByBody_u, betaAngleByBody_p));
        register_p(reg, "BetaAngle_" + body, betaAngleByBody_d, betaAngleByBody_p, withUnit("deg", dvm));
      }

      if (earthSpacecraftBodies.contains(body)) {
        EarthSpacecraftBodyAngle.put(body, resource(discrete(0.0)));
        if (reg != null) reg.discrete("EarthSpacecraftAngle_" + body, EarthSpacecraftBodyAngle.get(body), withUnit("deg", dvm));
      }

      if (altitudeBodies.contains(body)) {
        var spacecraftAltitude_d = resource(discrete(0.0));
        var spacecraftAltitude_u = resource(Unstructured.timeBased(t -> geometryCalculator.spacecraftAltitude(t, body)));
        var spacecraftAltitude_p = maybeApproximateAsLinear(spacecraftAltitude_u, body);
        SpacecraftAltitude.put(body, new DoubleResource(spacecraftAltitude_d, spacecraftAltitude_u, spacecraftAltitude_p));
        register_p(reg, "SpacecraftAltitude_" + body, spacecraftAltitude_d, spacecraftAltitude_p, withUnit("km", dvm));
      }

      if (illuminationBodies.contains(body)) {
        Map<String, MutableResource<Discrete<Double>>> illumAnglesMap = new HashMap<>();
        for (String angle : illumAngles) {
          illumAnglesMap.put(angle, resource(discrete(0.0)));
          if (reg != null) reg.discrete("IlluminationAnglesByBody_" + body + "_" + angle,
            illumAnglesMap.get(angle), withUnit("deg", dvm));
        }
        IlluminationAnglesByBody.put(body, illumAnglesMap);
      }

      if (raDecBodies.contains(body)) {
        Map<String, MutableResource<Discrete<Double>>> EarthRaDecMap = new HashMap<>();
        for (String angle : raDecIndices) {
          EarthRaDecMap.put(angle, resource(discrete(0.0)));
          if (reg != null) reg.discrete("EarthRaDecByBody_" + body + "_" + angle,
            EarthRaDecMap.get(angle), withUnit("deg", dvm));
        }
        EarthRaDecByBody.put(body, EarthRaDecMap);
        EarthRaDeltaWithSCByBody.put(body, resource(discrete(0.0)));
        if (reg != null) reg.discrete("EarthRaDeltaWithSCByBody_" + body, EarthRaDeltaWithSCByBody.get(body), withUnit("deg", dvm));
      }

      if (subSolarBodies.contains(body)) {
        BodySubSolarPoint.put(body, resource(discrete( new Vector3D(0.0,0.0,0.0))));
        registerVector(reg, "BodySubSolarPoint_" + body, BodySubSolarPoint.get(body));
      }

      if (subSCBodies.contains(body)) {
        Map<String, MutableResource<Discrete<Double>>> subSCMap = new HashMap<>();
        for (String index : subSCIndices) {
          subSCMap.put(index, resource(discrete(0.0)));
          if (reg != null) reg.discrete("subSCBodies_" + body + "_" + index,
            subSCMap.get(index), dvm);
        }
        BodySubSCPoint.put(body, subSCMap);
      }

      SpacecraftEclipseByBody.put(body, resource(discrete(EclipseTypes.NONE)));
      if (reg != null) reg.discrete("SpacecraftEclipseByBody_" + body,
        SpacecraftEclipseByBody.get(body), new EnumValueMapper<>(EclipseTypes.class));

      Map<String, MutableResource<Discrete<Boolean>>> occultationStationMap = new HashMap<>();
      for (Map.Entry<String,String> entry : ComplexRepresentativeStation.entrySet()) {
        occultationStationMap.put(entry.getValue(), resource(discrete(false)));
        if (reg != null) reg.discrete("IlluminationAnglesByBody_" + body + "_" + entry.getKey(),
          occultationStationMap.get(entry.getValue()), bvm);
      }
      SpacecraftOccultationByBodyAndStation.put(body, occultationStationMap);

      if (orbitParameterBodies.contains(body)) {
        orbitInclinationByBody.put(body, resource(discrete(0.0)));
        if (reg != null) reg.discrete("orbitInclinationByBody_" + body,
          orbitInclinationByBody.get(body), withUnit("deg", dvm));

        orbitPeriodByBody.put(body, resource(discrete(0.0)));
        if (reg != null) reg.discrete("orbitPeriodByBody_" + body,
          orbitPeriodByBody.get(body), withUnit("s", dvm));
      }

      Periapsis.put(body, resource(discrete(false)));
      if (reg != null) reg.discrete("Periapsis_" + body, Periapsis.get(body), bvm);

      Apoapsis.put(body, resource(discrete(false)));
      if (reg != null) reg.discrete("Apoapsis_" + body, Apoapsis.get(body), bvm);
    }

  }

  /**
   * Only calculate at times after which spice data is available.
   * @param f input function of time
   * @return f(t) but if t is before data is available use the time when it is first available
   * @param <T>
   */
  <T> Function<Duration, T> fit(Function<Duration, T> f) {
    return t -> f.apply(Duration.max(spiceStart, t));
  }

private Resource<Linear> maybeApproximateAsLinear(Resource<Unstructured<Double>> resource, String body) {
    if (!geometryCalculator.useLinearResources) {
      return assumeLinear(constant(0.0)); // dummy resource
    }
    if (optimizeSampling) {
      return approximateAsLinear(resource);
    }
    var periods = bodyObjects.get(body).calculationPeriods();
    if (periods.isEmpty() && !body.equalsIgnoreCase(EARTH)) {
      periods = bodyObjects.get(EARTH).calculationPeriods();
    }
    Duration samplePeriod = periods.isEmpty() ? Duration.of(24, Duration.HOURS) : getDuration(periods.get(0).getMaxTimeStep());
    return approximateUniformalyAsLinear(resource, samplePeriod);
  }

  public static Resource<Linear> approximateUniformalyAsLinear(Resource<Unstructured<Double>> resource, Duration samplePeriod) {
    if ( samplePeriod == null ) samplePeriod = Duration.HOUR;
    return Approximation.approximate(resource, SecantApproximation.<Unstructured<Double>>secantApproximation(byUniformSampling(samplePeriod)));
  }


  /**
   * Populate different types of resources for position and velocity with respect to a body using a specified function
   * of time.
   */
  private void makePositionAndVelocityResources(
      Map<String, Resource<Unstructured<Vector3D[]>>> bodyPositionAndVelocity_u,
      Map<String, List<Resource<Linear>>> bodyPositionAndVelocity_p,
      Map<String, Resource<Linear>[][]> bodyPositionAndVelocity_a,
      String body,
      BiFunction<Duration, String, Vector3D[]> f
    ) {

    var bpvr = resource(Unstructured.timeBased(fit(t -> f.apply(t, body))));
    bodyPositionAndVelocity_u.put(body, bpvr);
    List<Resource<Linear>> xyzn = new ArrayList<>();
    Resource[] xyzna0 = new Resource[] {
      maybeApproximateAsLinear(UnstructuredResourceApplicative.map(bpvr, v -> v[0] == null ? null : v[0].getX()), body),
      maybeApproximateAsLinear(UnstructuredResourceApplicative.map(bpvr, v -> v[0] == null ? null : v[0].getY()), body),
      maybeApproximateAsLinear(UnstructuredResourceApplicative.map(bpvr, v -> v[0] == null ? null : v[0].getZ()), body),
      maybeApproximateAsLinear(UnstructuredResourceApplicative.map(bpvr, v -> v[0] == null ? null : v[0].getNorm()), body)
    };
    Resource[] xyzna1 = new Resource[] {
      maybeApproximateAsLinear(UnstructuredResourceApplicative.map(bpvr, v -> v[1] == null ? null : v[1].getX()), body),
      maybeApproximateAsLinear(UnstructuredResourceApplicative.map(bpvr, v -> v[1] == null ? null : v[1].getY()), body),
      maybeApproximateAsLinear(UnstructuredResourceApplicative.map(bpvr, v -> v[1] == null ? null : v[1].getZ()), body),
      maybeApproximateAsLinear(UnstructuredResourceApplicative.map(bpvr, v -> v[1] == null ? null : v[1].getNorm()), body)
    };
    Resource<Linear>[][] xyzna = new Resource[][] { xyzna0, xyzna1 };
    bodyPositionAndVelocity_a.put(body, xyzna);
    for (var a : xyzna) {
        Collections.addAll(xyzn, a);
    }
    bodyPositionAndVelocity_p.put(body, xyzn);
  }

  public static void registerVector(Registrar reg, String name, Resource<Discrete<Vector3D>> r) {
    if (reg == null) return;
    reg.discrete(name + "_X", map(r, v -> v == null ? null : v.getX()), dvm);
    reg.discrete(name + "_Y", map(r, v -> v == null ? null : v.getY()), dvm);
    reg.discrete(name + "_Z", map(r, v -> v == null ? null : v.getZ()), dvm);
    reg.discrete(name + "_magnitude", map(r, v -> v == null ? null : v.getNorm()), dvm);
  }

  public static void registerRotation(Registrar reg, String name, Resource<Discrete<Rotation>> rotationResource) {
    if (reg == null) return;
    reg.discrete(name + ".Q0",  DiscreteResourceMonad.map(rotationResource, Rotation::getQ0), dvm);
    reg.discrete(name + ".Q1",  DiscreteResourceMonad.map(rotationResource, Rotation::getQ1), dvm);
    reg.discrete(name + ".Q2",  DiscreteResourceMonad.map(rotationResource, Rotation::getQ2), dvm);
    reg.discrete(name + ".Q3",  DiscreteResourceMonad.map(rotationResource, Rotation::getQ3), dvm);
  }

  private void register_p(Registrar r, String name, Resource<Discrete<Double>> rd, Resource<Linear> rl,
                          ValueMapper<Double> vm) {
    if (r == null) return;
    if (linearTimeBased) r.real(name, rl);
    if (registerDiscrete) r.discrete(name, rd, vm);
  }
  private void register_u(Registrar r, String name, Resource<Discrete<Double>> rd, Resource<Unstructured<Double>> ru, String body) {
    if (r == null) return;
    if (linearTimeBased) r.real(name, maybeApproximateAsLinear(ru, body));
    if (registerDiscrete) r.discrete(name, rd, dvm);
  }

  private void registerUV(Registrar r, String name, Resource<Discrete<Vector3D>> rd, Resource<Unstructured<Vector3D>> ru, String body) {
    register_u(r, name + "_X", map(rd, v -> v == null ? null : v.getX()),
      UnstructuredResourceApplicative.map(ru, v -> v == null ? null : v.getX()), body);
    register_u(r, name + "_Y", map(rd, v -> v == null ? null : v.getY()),
      UnstructuredResourceApplicative.map(ru, v -> v == null ? null : v.getY()), body);
    register_u(r, name + "_Z", map(rd, v -> v == null ? null : v.getZ()),
      UnstructuredResourceApplicative.map(ru, v -> v == null ? null : v.getZ()), body);
    register_u(r, name + "_magnitude", map(rd, v -> v == null ? null : v.getNorm()),
      UnstructuredResourceApplicative.map(ru, v -> v == null ? null : v.getNorm()), body);
  }

  public static Map<String, Body> getBodies(){
    return bodyObjects;
  }

  public MutableResource<Discrete<Double>> upleg_time() {
    return upleg_time.discrete();
  }

  public MutableResource<Discrete<Double>> downleg_time() {
    return downleg_time.discrete();
  }

  public MutableResource<Discrete<Double>> rtlt() {
    return rtlt.discrete();
  }

  public Map<String, List<Resource<Linear>>> bodyPositionAndVelocityWRTSpacecraft() {
    return bodyPositionAndVelocityWRTSpacecraft;
  }

  public Map<String, List<Resource<Linear>>> sunPositionAndVelocityWRTBody() {
    return sunPositionAndVelocityWRTBody;
  }

  public Map<String, List<Resource<Linear>>> bodyPositionAndVelocityWRTEarth() {
    return bodyPositionAndVelocityWRTEarth;
  }

  public MutableResource<Discrete<Double>> spacecraftDeclination() {
    return spacecraftDeclination.discrete();
  }

  public MutableResource<Discrete<Double>> spacecraftRightAscension() {
    return spacecraftRightAscension.discrete();
  }

  public Map<String, MutableResource<Discrete<Vector3D>>> BODY_POS_ICRF() {
    return BODY_POS_ICRF;
  }

  public Map<String, MutableResource<Discrete<Vector3D>>> BODY_VEL_ICRF() {
    return BODY_VEL_ICRF;
  }

  public Map<String, DoubleResource> SpacecraftBodyRange() {
    return SpacecraftBodyRange;
  }

  public Map<String, DoubleResource> SpacecraftBodySpeed() {
    return SpacecraftBodySpeed;
  }

  public Map<String, MutableResource<Discrete<Double>>> SunSpacecraftBodyAngle() {
    Map<String, MutableResource<Discrete<Double>>> result = new HashMap<>();
    for (Map.Entry<String, DoubleResource> entry : SunSpacecraftBodyAngle.entrySet()) {
      result.put(entry.getKey(), entry.getValue().discrete());
    }
    return result;
  }

  public Map<String, MutableResource<Discrete<Double>>> SunBodySpacecraftAngle() {
    Map<String, MutableResource<Discrete<Double>>> result = new HashMap<>();
    for (Map.Entry<String, DoubleResource> entry : SunBodySpacecraftAngle.entrySet()) {
      result.put(entry.getKey(), entry.getValue().discrete());
    }
    return result;
  }

  public Map<String, DoubleResource> BodyHalfAngleSize() {
    return BodyHalfAngleSize;
  }

  public Map<String, DoubleResource> BetaAngleByBody() {
    return BetaAngleByBody;
  }

  public Map<String, MutableResource<Discrete<Double>>> EarthSpacecraftBodyAngle() {
    return EarthSpacecraftBodyAngle;
  }

  public MutableResource<Discrete<Double>> EarthSunProbeAngle() {
    return EarthSunProbeAngle.discrete();
  }

  public Map<String, DoubleResource> SpacecraftAltitude() {
    return SpacecraftAltitude;
  }

  public Map<String, Map<String, MutableResource<Discrete<Double>>>> IlluminationAnglesByBody() {
    return IlluminationAnglesByBody;
  }

  public Map<String, Map<String, MutableResource<Discrete<Double>>>> EarthRaDecByBody() {
    return EarthRaDecByBody;
  }

  public Map<String, MutableResource<Discrete<Double>>> EarthRaDeltaWithSCByBody() {
    return EarthRaDeltaWithSCByBody;
  }

  public Map<String, MutableResource<Discrete<Vector3D>>> BodySubSolarPoint() {
    return BodySubSolarPoint;
  }

  public Map<String, Map<String, MutableResource<Discrete<Double>>>> BodySubSCPoint() {
    return BodySubSCPoint;
  }

  public Map<String, MutableResource<Discrete<EclipseTypes>>> SpacecraftEclipseByBody() {
    return SpacecraftEclipseByBody;
  }

  public MutableResource<Discrete<EclipseTypes>> AnySpacecraftEclipse() {
    return AnySpacecraftEclipse;
  }

  public Map<String, Map<String, MutableResource<Discrete<Boolean>>>> SpacecraftOccultationByBodyAndStation() {
    return SpacecraftOccultationByBodyAndStation;
  }

  public MutableResource<Discrete<Integer>> Occultation() {
    return Occultation;
  }

  public MutableResource<Discrete<Double>> FractionOfSunNotInEclipse() {
    return FractionOfSunNotInEclipse;
  }

  public MutableResource<Discrete<Integer>> LitOrDarkSide() {
    return LitOrDarkSide;
  }

  public Map<String, MutableResource<Discrete<Double>>> orbitInclinationByBody() {
    return orbitInclinationByBody;
  }

  public Map<String, MutableResource<Discrete<Double>>> orbitPeriodByBody() {
    return orbitPeriodByBody;
  }

}
