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

  public static boolean optimizeSampling = false;

  /**
   * Set this true and registerTimeBased true and registerDiscrete false
   */
  public boolean linearTimeBased;
  public boolean registerDiscrete;

  public static final double FLOAT_EPSILON = 0.0001;
  private static Map<String, Body> bodyObjects;
  private static String[] bodies;
  private static List<String> earthSpacecraftBodies;
  private static List<String> altitudeBodies;
  private static List<String> illuminationBodies;
  private static List<String> raDecBodies;
  private static List<String> subSolarBodies;
  private static List<String> subSCBodies;
  private static List<String> betaAngleBodies;
  private static List<String> orbitParameterBodies;

  //private static String[] cartesian = new String[]{"x","y","z"};
  private static String[] illumAngles = new String[]{"phase","incidence","emission"};
  private static String[] raDecIndices = new String[]{"Ra", "Dec"};
  //private static String[] latLonCoordinates = new String[]{"latitude","longitude", "radius"};
  private static String[] subSCIndices = new String[]{"dist","latitude","longitude","radius","LST"};

  public static Map<String, String> ComplexRepresentativeStation = new HashMap<>();
  static{
    ComplexRepresentativeStation.put("Goldstone", "DSS-24");
    ComplexRepresentativeStation.put("Canberra", "DSS-36");
    ComplexRepresentativeStation.put("Madrid", "DSS-54");
  }

  public MutableResource<Discrete<Double>> upleg_time;
  public Resource<Unstructured<Double>> upleg_time_u;
  public Resource<Linear> upleg_time_p;

  public MutableResource<Discrete<Double>> downleg_time;
  public Resource<Unstructured<Double>> downleg_time_u;
  public Resource<Linear> downleg_time_p;
  public MutableResource<Discrete<Double>> rtlt;
  public Resource<Unstructured<Double>> rtlt_u;
  public Resource<Linear> rtlt_p;
  public Map<String, Resource<Unstructured<Vector3D[]>>> bodyPositionAndVelocityWRTSpacecraft_u;
  public Map<String, Resource<Linear>[][]> bodyPositionAndVelocityWRTSpacecraft_a;
  public Map<String, List<Resource<Linear>>> bodyPositionAndVelocityWRTSpacecraft;
  public Map<String, Resource<Unstructured<Vector3D[]>>> sunPositionAndVelocityWRTBody_u;
  public Map<String, Resource<Linear>[][]> sunPositionAndVelocityWRTBody_a;
  public Map<String, List<Resource<Linear>>> sunPositionAndVelocityWRTBody;
  public Map<String, Resource<Unstructured<Vector3D[]>>> bodyPositionAndVelocityWRTEarth_u;
  public Map<String, Resource<Linear>[][]> bodyPositionAndVelocityWRTEarth_a;
  public Map<String, List<Resource<Linear>>> bodyPositionAndVelocityWRTEarth;

  public Resource<Unstructured<RADec>> radec_u;
  public MutableResource<Discrete<Double>> spacecraftDeclination;
  public Resource<Unstructured<Double>> spacecraftDeclination_u;
  public Resource<Linear> spacecraftDeclination_p;
  public MutableResource<Discrete<Double>> spacecraftRightAscension;
  public Resource<Unstructured<Double>> spacecraftRightAscension_u;
  public Resource<Linear> spacecraftRightAscension_p;
  public Map<String, MutableResource<Discrete<Vector3D>>> BODY_POS_ICRF;
  public Map<String, Resource<Unstructured<Vector3D>>> BODY_POS_ICRF_u;
  public Map<String, Resource<Linear>[]> BODY_POS_ICRF_a;
  public Map<String, MutableResource<Discrete<Vector3D>>> BODY_VEL_ICRF;
  public Map<String, Resource<Unstructured<Vector3D>>> BODY_VEL_ICRF_u;
  public Map<String, Resource<Linear>[]> BODY_VEL_ICRF_a;
  public Map<String, MutableResource<Discrete<Double>>> SpacecraftBodyRange;
  public Map<String, Resource<Linear>> SpacecraftBodyRange_p;
  public Map<String, MutableResource<Discrete<Double>>> SpacecraftBodySpeed;
  public Map<String, Resource<Linear>> SpacecraftBodySpeed_p;
  public Map<String, MutableResource<Discrete<Double>>> SunSpacecraftBodyAngle;
  public Map<String, Resource<Linear>> SunSpacecraftBodyAngle_p;
  public Map<String, Resource<Unstructured<Double>>> SunSpacecraftBodyAngle_u;
  public Map<String, MutableResource<Discrete<Double>>> SunBodySpacecraftAngle;
  public Map<String, Resource<Linear>> SunBodySpacecraftAngle_p;
  public Map<String, Resource<Unstructured<Double>>> SunBodySpacecraftAngle_u;
  public Map<String, MutableResource<Discrete<Double>>> BodyHalfAngleSize;
  public Map<String, Resource<Linear>> BodyHalfAngleSize_p;

  public Map<String, MutableResource<Discrete<Double>>> BetaAngleByBody;
  public Map<String, Resource<Linear>> BetaAngleByBody_p;

  public Map<String, MutableResource<Discrete<Double>>> EarthSpacecraftBodyAngle;
  public Map<String, Resource<Linear>> EarthSpacecraftBodyAngle_p;

  public MutableResource<Discrete<Double>> EarthSunProbeAngle;
  public Resource<Unstructured<Double>> EarthSunProbeAngle_u;
  public Resource<Linear> EarthSunProbeAngle_p;

  public Map<String, MutableResource<Discrete<Double>>> SpacecraftAltitude;
  public Map<String, Resource<Linear>> SpacecraftAltitude_p;

  public Map<String, Map<String, MutableResource<Discrete<Double>>>> IlluminationAnglesByBody;
  public Map<String, Map<String, Resource<Linear>>> IlluminationAnglesByBody_p;

  public Map<String, Map<String, MutableResource<Discrete<Double>>>> EarthRaDecByBody;
  public Map<String, Map<String, Resource<Linear>>> EarthRaDecByBody_p;

  public Map<String, MutableResource<Discrete<Double>>> EarthRaDeltaWithSCByBody;
  public Map<String, Resource<Linear>> EarthRaDeltaWithSCByBody_p;

  public Map<String, MutableResource<Discrete<Vector3D>>> BodySubSolarPoint;
  public Map<String, Resource<Linear>[]> BodySubSolarPoint_p;
  public Map<String, Map<String, MutableResource<Discrete<Double>>>> BodySubSCPoint;
  public Map<String, Map<String, Resource<Linear>>> BodySubSCPoint_p;

  public Map<String, MutableResource<Discrete<EclipseTypes>>> SpacecraftEclipseByBody;
  public MutableResource<Discrete<EclipseTypes>> AnySpacecraftEclipse;

  public Map<String, Map<String, MutableResource<Discrete<Boolean>>>> SpacecraftOccultationByBodyAndStation;
  public MutableResource<Discrete<Integer>> Occultation;
  public MutableResource<Discrete<Double>> FractionOfSunNotInEclipse;
  public MutableResource<Discrete<Integer>> LitOrDarkSide;

  public Map<String, MutableResource<Discrete<Double>>> orbitInclinationByBody;
  public Map<String, Resource<Linear>> orbitInclinationByBody_p;
  public Map<String, MutableResource<Discrete<Double>>> orbitPeriodByBody;
  public Map<String, Resource<Linear>> orbitPeriodByBody_p;


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
  public GenericGeometryResources(Optional<Registrar> reg, Map<String, Body> allBodies, GenericGeometryCalculator geometryCalculator) {
    this.geometryCalculator = geometryCalculator;
    this.spiceStart = geometryCalculator.spiceStart;
    linearTimeBased = geometryCalculator.useLinearResources;
    registerDiscrete = !geometryCalculator.useLinearResources;
    bodyObjects = allBodies;
    bodies = Body.getNamesOfBodies(allBodies);
    earthSpacecraftBodies =  Body.getEarthSCBodies(allBodies);
    altitudeBodies = Body.getAltitudeBodies(allBodies);
    illuminationBodies = Body.getIlluminationAngleBodies(allBodies);
    raDecBodies = Body.getRaDecBodies(allBodies);
    subSolarBodies = Body.getSubSolarBodies(allBodies);
    subSCBodies = Body.getRadiatorAvoidanceBodies(allBodies);
    betaAngleBodies = Body.getBetaAngleBodies(allBodies);
    orbitParameterBodies = Body.getOrbitParameterBodies(allBodies);

    // Initialize resources
    bodyPositionAndVelocityWRTSpacecraft = new HashMap<>();
    bodyPositionAndVelocityWRTSpacecraft_a = new HashMap<>();
    bodyPositionAndVelocityWRTSpacecraft_u = new HashMap<>();
    sunPositionAndVelocityWRTBody = new HashMap<>();
    sunPositionAndVelocityWRTBody_a = new HashMap<>();
    sunPositionAndVelocityWRTBody_u = new HashMap<>();
    bodyPositionAndVelocityWRTEarth = new HashMap<>();
    bodyPositionAndVelocityWRTEarth_a = new HashMap<>();
    bodyPositionAndVelocityWRTEarth_u = new HashMap<>();
    BODY_POS_ICRF = new HashMap<>();
    BODY_POS_ICRF_a = new HashMap<>();
    BODY_POS_ICRF_u = new HashMap<>();
    BODY_VEL_ICRF = new HashMap<>();
    BODY_VEL_ICRF_a = new HashMap<>();
    BODY_VEL_ICRF_u = new HashMap<>();
    SpacecraftBodyRange = new HashMap<>();
    SpacecraftBodyRange_p = new HashMap<>();
    SpacecraftBodySpeed = new HashMap<>();
    SpacecraftBodySpeed_p = new HashMap<>();
    SunSpacecraftBodyAngle = new HashMap<>();
    SunSpacecraftBodyAngle_p = new HashMap<>();
    SunSpacecraftBodyAngle_u = new HashMap<>();
    SunBodySpacecraftAngle = new HashMap<>();
    SunBodySpacecraftAngle_p = new HashMap<>();
    SunBodySpacecraftAngle_u = new HashMap<>();
    BodyHalfAngleSize = new HashMap<>();
    BodyHalfAngleSize_p = new HashMap<>();
    IlluminationAnglesByBody = new HashMap<>();
    IlluminationAnglesByBody_p = new HashMap<>();
    EarthRaDecByBody = new HashMap<>();
    EarthRaDecByBody_p = new HashMap<>();
    BodySubSCPoint = new HashMap<>();
    BodySubSCPoint_p = new HashMap<>();
    SpacecraftOccultationByBodyAndStation = new HashMap<>();
    SpacecraftEclipseByBody = new HashMap<>();
    BetaAngleByBody = new HashMap<>();
    BetaAngleByBody_p = new HashMap<>();
    EarthSpacecraftBodyAngle = new HashMap<>();
    EarthSpacecraftBodyAngle_p = new HashMap<>();
    SpacecraftAltitude = new HashMap<>();
    SpacecraftAltitude_p = new HashMap<>();
    EarthRaDeltaWithSCByBody = new HashMap<>();
    EarthRaDeltaWithSCByBody_p = new HashMap<>();
    BodySubSolarPoint = new HashMap<>();
    BodySubSolarPoint_p = new HashMap<>();
    orbitInclinationByBody = new HashMap<>();
    orbitInclinationByBody_p = new HashMap<>();
    orbitPeriodByBody = new HashMap<>();
    orbitPeriodByBody_p = new HashMap<>();

    Apoapsis = new HashMap<>();
    //Apoapsis_p = new HashMap<>();
    Periapsis = new HashMap<>();
    //Periapsis_p = new HashMap<>();

    boolean linear = geometryCalculator.useLinearResources;

    // Non-arrayed resources
    upleg_time = resource(discrete(0.0));
    upleg_time_u = resource(Unstructured.timeBased(fit(geometryCalculator::upleg_duration)));
    upleg_time_p = !linear ? null : maybeApproximateAsLinear(upleg_time_u, "EARTH");
    register_p(reg, "upleg_time", upleg_time, upleg_time_p, dvm);

    downleg_time = resource(discrete(0.0));
    downleg_time_u = resource(Unstructured.timeBased(fit(geometryCalculator::downleg_duration)));
    downleg_time_p = !linear ? null : maybeApproximateAsLinear(downleg_time_u, "EARTH");
    register_p(reg, "downleg_time", downleg_time, downleg_time_p, dvm);

    rtlt = resource(discrete(0.0));
    rtlt_u = resource(Unstructured.timeBased(fit(t -> {
      Double ult = geometryCalculator.upleg_duration(t) * 1e6;
      var dlt = geometryCalculator.downleg_duration(t.plus(ult.longValue(), Duration.MICROSECONDS)) * 1e6;
      return (ult + dlt)/1e6;
    })));
    rtlt_p = !linear ? null : maybeApproximateAsLinear(rtlt_u, "EARTH");

    radec_u = resource(Unstructured.timeBased(fit(geometryCalculator::scRADec)));
    spacecraftDeclination = resource(discrete(0.0));
    spacecraftDeclination_u = UnstructuredResourceApplicative.map(radec_u, radec -> radec.getDec());
    spacecraftDeclination_p = maybeApproximateAsLinear(spacecraftDeclination_u, "EARTH");
    register_p(reg, "spacecraftDeclination", spacecraftDeclination, spacecraftDeclination_p, dvm);

    spacecraftRightAscension = resource(discrete(0.0));
    spacecraftRightAscension_u = UnstructuredResourceApplicative.map(radec_u, radec -> radec.getRA());
    spacecraftRightAscension_p = maybeApproximateAsLinear(spacecraftRightAscension_u, "EARTH");
    register_p(reg, "spacecraftRightAscension", spacecraftRightAscension, spacecraftRightAscension_p, dvm);

    EarthSunProbeAngle = resource(discrete(0.0));
    EarthSunProbeAngle_u = resource(Unstructured.timeBased(fit(t -> geometryCalculator.earthSunProbeAngle(t))));
    EarthSunProbeAngle_p = maybeApproximateAsLinear(EarthSunProbeAngle_u, "EARTH");
    register_p(reg, "EarthSunProbeAngle", EarthSunProbeAngle, EarthSunProbeAngle_p, dvm);

    AnySpacecraftEclipse = resource(discrete(EclipseTypes.NONE));
    if (!reg.isEmpty()) reg.get().discrete("AnySpacecraftEclipse", AnySpacecraftEclipse, new EnumValueMapper(EclipseTypes.class));

    Occultation = resource(discrete(0));
    if (!reg.isEmpty()) reg.get().discrete("Occultation", Occultation, ivm);

    FractionOfSunNotInEclipse = resource(discrete(1.0));
    if (!reg.isEmpty()) reg.get().discrete("FractionOfSunNotInEclipse", FractionOfSunNotInEclipse, dvm);

    LitOrDarkSide = resource(discrete(0));
    if (!reg.isEmpty()) reg.get().discrete("LitOrDarkSide", LitOrDarkSide, ivm);

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

      SpacecraftBodyRange.put(body, resource(discrete(0.0)));
      SpacecraftBodyRange_p.put(body, BODY_POS_ICRF_a.get(body)[3]);
      register_p(reg, "SpacecraftBodyRange_" + body,
        SpacecraftBodyRange.get(body), SpacecraftBodyRange_p.get(body), withUnit("km", dvm));

      SpacecraftBodySpeed.put(body, resource(discrete(0.0)));
      SpacecraftBodySpeed_p.put(body, BODY_VEL_ICRF_a.get(body)[3]);
      register_p(reg, "SpacecraftBodySpeed_" + body,
        SpacecraftBodySpeed.get(body), SpacecraftBodySpeed_p.get(body), withUnit("km/s", dvm));

      SunSpacecraftBodyAngle.put(body, resource(discrete(0.0)));
      //var pres = ResourceMonad.map(BODY_POS_ICRF_a.get(body), sunPositionAndVelocityWRTBody_a.get(body)[0], (bp, sp) -> geometryCalculator.sunSpacecraftBodyAngle(new Vector3D(bp), new Vector3D(sp)));
      var ures = resource(Unstructured.timeBased(t -> geometryCalculator.sunSpacecraftBodyAngle(t, body)));
      SunSpacecraftBodyAngle_u.put(body, ures);
      SunSpacecraftBodyAngle_p.put(body, maybeApproximateAsLinear(ures, body));
      //if (!reg.isEmpty()) reg.get().discrete("SunSpacecraftBodyAngle_" + body, SunSpacecraftBodyAngle.get(body), withUnit("deg", dvm));
      register_p(reg, "SunSpacecraftBodyAngle_" + body, SunSpacecraftBodyAngle.get(body), SunSpacecraftBodyAngle_p.get(body), withUnit("deg", dvm));

      SunBodySpacecraftAngle.put(body, resource(discrete(0.0)));
      ures = resource(Unstructured.timeBased(t -> geometryCalculator.sunBodySpacecraftAngle(t, body)));
      SunBodySpacecraftAngle_u.put(body, ures);
      SunBodySpacecraftAngle_p.put(body, maybeApproximateAsLinear(ures, body));
      register_p(reg, "SunBodySpacecraftAngle_" + body, SunBodySpacecraftAngle.get(body), SunBodySpacecraftAngle_p.get(body), withUnit("deg", dvm));
      //if (!reg.isEmpty()) reg.get().discrete("SunBodySpacecraftAngle_" + body, SunBodySpacecraftAngle.get(body), withUnit("deg", dvm));

      BodyHalfAngleSize.put(body, resource(discrete(0.0)));
      if (!reg.isEmpty()) reg.get().discrete("BodyHalfAngleSize_" + body, BodyHalfAngleSize.get(body), withUnit("deg", dvm));

      if (betaAngleBodies.contains(body)) {
        BetaAngleByBody.put(body, resource(discrete(0.0)));
        if (!reg.isEmpty()) reg.get().discrete("BetaAngle_" + body, BetaAngleByBody.get(body), withUnit("deg", dvm));
      }

      if (earthSpacecraftBodies.contains(body)) {
        EarthSpacecraftBodyAngle.put(body, resource(discrete(0.0)));
        if (!reg.isEmpty()) reg.get().discrete("EarthSpacecraftAngle_" + body, EarthSpacecraftBodyAngle.get(body), withUnit("deg", dvm));
      }

      if (altitudeBodies.contains(body)) {
        SpacecraftAltitude.put(body, resource(discrete(0.0)));
        if (!reg.isEmpty()) reg.get().discrete("SpacecraftAltitude_" + body, SpacecraftAltitude.get(body), withUnit("km", dvm));
      }

      if (illuminationBodies.contains(body)) {
        Map<String, MutableResource<Discrete<Double>>> illumAnglesMap = new HashMap<>();
        for (String angle : illumAngles) {
          illumAnglesMap.put(angle, resource(discrete(0.0)));
          if (!reg.isEmpty()) reg.get().discrete("IlluminationAnglesByBody_" + body + "_" + angle,
            illumAnglesMap.get(angle), withUnit("deg", dvm));
        }
        IlluminationAnglesByBody.put(body, illumAnglesMap);
      }

      if (raDecBodies.contains(body)) {
        Map<String, MutableResource<Discrete<Double>>> EarthRaDecMap = new HashMap<>();
        for (String angle : raDecIndices) {
          EarthRaDecMap.put(angle, resource(discrete(0.0)));
          if (!reg.isEmpty()) reg.get().discrete("EarthRaDecByBody_" + body + "_" + angle,
            EarthRaDecMap.get(angle), withUnit("deg", dvm));
        }
        EarthRaDecByBody.put(body, EarthRaDecMap);
        EarthRaDeltaWithSCByBody.put(body, resource(discrete(0.0)));
        if (!reg.isEmpty()) reg.get().discrete("EarthRaDeltaWithSCByBody_" + body, EarthRaDeltaWithSCByBody.get(body), withUnit("deg", dvm));
      }

      if (subSolarBodies.contains(body)) {
        BodySubSolarPoint.put(body, resource(discrete( new Vector3D(0.0,0.0,0.0))));
        if (reg != null) registerVector(reg, "BodySubSolarPoint_" + body, BodySubSolarPoint.get(body));
      }

      if (subSCBodies.contains(body)) {
        Map<String, MutableResource<Discrete<Double>>> subSCMap = new HashMap<>();
        for (String index : subSCIndices) {
          subSCMap.put(index, resource(discrete(0.0)));
          if (!reg.isEmpty()) reg.get().discrete("subSCBodies_" + body + "_" + index,
            subSCMap.get(index), dvm);
        }
        BodySubSCPoint.put(body, subSCMap);
      }

      SpacecraftEclipseByBody.put(body, resource(discrete(EclipseTypes.NONE)));
      if (!reg.isEmpty()) reg.get().discrete("SpacecraftEclipseByBody_" + body,
        SpacecraftEclipseByBody.get(body), new EnumValueMapper<>(EclipseTypes.class));

      Map<String, MutableResource<Discrete<Boolean>>> occultationStationMap = new HashMap<>();
      for (Map.Entry<String,String> entry : ComplexRepresentativeStation.entrySet()) {
        occultationStationMap.put(entry.getValue(), resource(discrete(false)));
        if (!reg.isEmpty()) reg.get().discrete("IlluminationAnglesByBody_" + body + "_" + entry.getKey(),
          occultationStationMap.get(entry.getValue()), bvm);
      }
      SpacecraftOccultationByBodyAndStation.put(body, occultationStationMap);

      if (orbitParameterBodies.contains(body)) {
        orbitInclinationByBody.put(body, resource(discrete(0.0)));
        if (!reg.isEmpty()) reg.get().discrete("orbitInclinationByBody_" + body,
          orbitInclinationByBody.get(body), withUnit("deg", dvm));

        orbitPeriodByBody.put(body, resource(discrete(0.0)));
        if (!reg.isEmpty()) reg.get().discrete("orbitPeriodByBody_" + body,
          orbitPeriodByBody.get(body), withUnit("s", dvm));
      }

      Periapsis.put(body, resource(discrete(false)));
      if (!reg.isEmpty()) reg.get().discrete("Periapsis_" + body, Periapsis.get(body), bvm);

      Apoapsis.put(body, resource(discrete(false)));
      if (!reg.isEmpty()) reg.get().discrete("Apoapsis_" + body, Apoapsis.get(body), bvm);
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
    if (periods.isEmpty() && !body.equalsIgnoreCase("EARTH")) {
      periods = bodyObjects.get("EARTH").calculationPeriods();
    }
    Duration samplePeriod = periods.isEmpty() ? Duration.of(24, Duration.HOURS) : getDuration(periods.get(0).getMaxTimeStep());
    return approximateUniformalyAsLinear(resource, samplePeriod);
  }

  public static Resource<Linear> approximateUniformalyAsLinear(Resource<Unstructured<Double>> resource, Duration samplePeriod) {
    return Approximation.approximate(resource, SecantApproximation.<Unstructured<Double>>secantApproximation(byUniformSampling(Duration.HOUR)));
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
    Resource<Linear>[] xyzna0 = new Resource[] {
      maybeApproximateAsLinear(UnstructuredResourceApplicative.map(bpvr, v -> v[0] == null ? null : v[0].getX()), body),
      maybeApproximateAsLinear(UnstructuredResourceApplicative.map(bpvr, v -> v[0] == null ? null : v[0].getY()), body),
      maybeApproximateAsLinear(UnstructuredResourceApplicative.map(bpvr, v -> v[0] == null ? null : v[0].getZ()), body),
      maybeApproximateAsLinear(UnstructuredResourceApplicative.map(bpvr, v -> v[0] == null ? null : v[0].getNorm()), body)
    };
    Resource<Linear>[] xyzna1 = new Resource[] {
      maybeApproximateAsLinear(UnstructuredResourceApplicative.map(bpvr, v -> v[1] == null ? null : v[1].getX()), body),
      maybeApproximateAsLinear(UnstructuredResourceApplicative.map(bpvr, v -> v[1] == null ? null : v[1].getY()), body),
      maybeApproximateAsLinear(UnstructuredResourceApplicative.map(bpvr, v -> v[1] == null ? null : v[1].getZ()), body),
      maybeApproximateAsLinear(UnstructuredResourceApplicative.map(bpvr, v -> v[1] == null ? null : v[1].getNorm()), body)
    };
    Resource<Linear>[][] xyzna = new Resource[][] { xyzna0, xyzna1 };
    bodyPositionAndVelocity_a.put(body, xyzna);
    for (var a : xyzna) {
      for (var aa : a) {
        xyzn.add(aa);
      }
    }
    bodyPositionAndVelocity_p.put(body, xyzn);
  }

  public static void registerVector(Optional<Registrar> reg, String name, Resource<Discrete<Vector3D>> r) {
    if (reg.isEmpty()) return;
    reg.get().discrete(name + "_X", map(r, v -> v == null ? null : v.getX()), dvm);
    reg.get().discrete(name + "_Y", map(r, v -> v == null ? null : v.getY()), dvm);
    reg.get().discrete(name + "_Z", map(r, v -> v == null ? null : v.getZ()), dvm);
    reg.get().discrete(name + "_magnitude", map(r, v -> v == null ? null : v.getNorm()), dvm);
  }

  public static void registerRotation(Optional<Registrar> reg, String name, Resource<Discrete<Rotation>> rotationResource) {
    if (reg.isEmpty()) return;
    reg.get().discrete(name + ".Q0",  DiscreteResourceMonad.map(rotationResource, Rotation::getQ0), dvm);
    reg.get().discrete(name + ".Q1",  DiscreteResourceMonad.map(rotationResource, Rotation::getQ1), dvm);
    reg.get().discrete(name + ".Q2",  DiscreteResourceMonad.map(rotationResource, Rotation::getQ2), dvm);
    reg.get().discrete(name + ".Q3",  DiscreteResourceMonad.map(rotationResource, Rotation::getQ3), dvm);
  }

  private void register_p(Optional<Registrar> r, String name, Resource<Discrete<Double>> rd, Resource<Linear> rl,
                          ValueMapper<Double> vm) {
    if (r.isEmpty()) return;
    if (linearTimeBased) r.get().real(name, rl);
    if (registerDiscrete) r.get().discrete(name, rd, vm);
  }
  private void register_u(Optional<Registrar> r, String name, Resource<Discrete<Double>> rd, Resource<Unstructured<Double>> ru, String body) {
    if (r.isEmpty()) return;
    if (linearTimeBased) r.get().real(name, maybeApproximateAsLinear(ru, body));
    if (registerDiscrete) r.get().discrete(name, rd, dvm);
  }

  private void registerUV(Optional<Registrar> r, String name, Resource<Discrete<Vector3D>> rd, Resource<Unstructured<Vector3D>> ru, String body) {
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


  public static double auToKm(double au) {
      return au * 149597870.700;
//        try {
//            double km = CSPICE.convrt(au, "AU", "KILOMETERS");
//            return km;
//        } catch (SpiceErrorException e) {
//            throw new RuntimeException(e);
//        }
  }

  public static double kmToAu(double km) {
      return km /149597870.700;
//        try {
//            double au = CSPICE.convrt(au, "KILOMETERS", "AU");
//            return au;
//        } catch (SpiceErrorException e) {
//            throw new RuntimeException(e);
//        }
  }
}
