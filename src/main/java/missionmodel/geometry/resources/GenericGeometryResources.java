package missionmodel.geometry.resources;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.BooleanValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.Vector3DValueMapper;
import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import missionmodel.geometry.spiceinterpolation.Body;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.util.HashMap;
import java.util.Map;

import static gov.nasa.jpl.aerie.contrib.metadata.UnitRegistrar.withUnit;
import static gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource.resource;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;

public class GenericGeometryResources {

  public static final double FLOAT_EPSILON = 0.0001;
  private static Map<String, Body> bodyObjects;
  private static String[] bodies;

  public Map<String, MutableResource<Discrete<Vector3D>>> BODY_POS_ICRF;
  public Map<String, MutableResource<Discrete<Vector3D>>> BODY_VEL_ICRF;
  public Map<String, MutableResource<Discrete<Double>>> SpacecraftBodyRange;
  public Map<String, MutableResource<Discrete<Double>>> SpacecraftBodySpeed;
  public Map<String, MutableResource<Discrete<Double>>> SunSpacecraftBodyAngle;
  public Map<String, MutableResource<Discrete<Double>>> SunBodySpacecraftAngle;
  public Map<String, MutableResource<Discrete<Double>>> BodyHalfAngleSize;
  public Map<String, MutableResource<Discrete<Boolean>>> Periapsis;
  public Map<String, MutableResource<Discrete<Boolean>>> Apoapsis;

  public GenericGeometryResources(Registrar registrar, HashMap<String, Body> allBodies) {
    bodyObjects = allBodies;
    bodies = Body.getNamesOfBodies(allBodies);

    // Initialize resources
    BODY_POS_ICRF = new HashMap<>();
    BODY_VEL_ICRF = new HashMap<>();
    SpacecraftBodyRange = new HashMap<>();
    SpacecraftBodySpeed = new HashMap<>();
    SunSpacecraftBodyAngle = new HashMap<>();
    SunBodySpacecraftAngle = new HashMap<>();
    BodyHalfAngleSize = new HashMap<>();
    Apoapsis = new HashMap<>();
    Periapsis = new HashMap<>();

    // loop through bodies to build and register resources
    for (String body : bodies) {
      BODY_POS_ICRF.put(body, resource(discrete( new Vector3D(0.0,0.0,0.0))));
      registrar.discrete("BODY_POS_ICRF_" + body, BODY_POS_ICRF.get(body), new Vector3DValueMapper(new DoubleValueMapper()));

      BODY_VEL_ICRF.put(body, resource(discrete( new Vector3D(0.0,0.0,0.0))));
      registrar.discrete("BODY_VEL_ICRF_" + body, BODY_VEL_ICRF.get(body), new Vector3DValueMapper(new DoubleValueMapper()));

      SpacecraftBodyRange.put(body, resource(discrete(0.0)));
      registrar.discrete("SpacecraftBodyRange_" + body, SpacecraftBodyRange.get(body), withUnit("km", new DoubleValueMapper()));

      SpacecraftBodySpeed.put(body, resource(discrete(0.0)));
      registrar.discrete("SpacecraftBodySpeed_" + body, SpacecraftBodySpeed.get(body), withUnit("km/s", new DoubleValueMapper()));

      SunSpacecraftBodyAngle.put(body, resource(discrete(0.0)));
      registrar.discrete("SunSpacecraftBodyAngle_" + body, SunSpacecraftBodyAngle.get(body), withUnit("deg", new DoubleValueMapper()));

      SunBodySpacecraftAngle.put(body, resource(discrete(0.0)));
      registrar.discrete("SunBodySpacecraftAngle_" + body, SunBodySpacecraftAngle.get(body), withUnit("deg", new DoubleValueMapper()));

      BodyHalfAngleSize.put(body, resource(discrete(0.0)));
      registrar.discrete("BodyHalfAngleSize_" + body, BodyHalfAngleSize.get(body), withUnit("deg", new DoubleValueMapper()));

      Periapsis.put(body, resource(discrete(false)));
      registrar.discrete("Periapsis_" + body, Periapsis.get(body), new BooleanValueMapper());

      Apoapsis.put(body, resource(discrete(false)));
      registrar.discrete("Apoapsis_" + body, Apoapsis.get(body), new BooleanValueMapper());
    }

  }
  public static Map<String, Body> getBodies(){
    return bodyObjects;
  }


}
