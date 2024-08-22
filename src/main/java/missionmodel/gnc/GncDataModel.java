package missionmodel.gnc;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.BooleanValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.StringValueMapper;
import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.util.LinkedHashMap;

import static gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource.resource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.map;

public class GncDataModel {
  public MutableResource<Discrete<Vector3D>> PointingAxis;
  public MutableResource<Discrete<Double>> PointingRotation;
  public MutableResource<Discrete<Vector3D>> RotationRate;
  public MutableResource<Discrete<Boolean>> IsSlewing;
  public MutableResource<Discrete<String>> primaryObserverString;
  public MutableResource<Discrete<Vector3D>> primaryObserver;
  public MutableResource<Discrete<String>> secondaryObserverString;
  public MutableResource<Discrete<Vector3D>> secondaryObserver;
  public MutableResource<Discrete<String>> primaryTarget;
  public MutableResource<Discrete<String>> secondaryTarget;

  public GncDataModel(Registrar registrar) {
    PointingAxis = resource(discrete(Z));
    registerVector(registrar, "PointingAxis", PointingAxis);
    PointingRotation = resource(discrete(0.0));
    registrar.discrete("PointingRotation", PointingRotation, new DoubleValueMapper());
    RotationRate = resource(discrete(Vector3D.ZERO));
    registerVector(registrar, "RotationRate", RotationRate);
    IsSlewing = resource(discrete(Boolean.FALSE));
    registrar.discrete("IsSlewing", IsSlewing, new BooleanValueMapper());

    primaryObserverString = resource(discrete("X"));
    registrar.discrete("PrimaryObserver", primaryObserverString, new StringValueMapper());
    primaryObserver = resource(discrete(X));
    registerVector(registrar, "PrimaryObserverVector", primaryObserver);
    secondaryObserverString = resource(discrete("Y"));
    registrar.discrete("SecondaryObserver", secondaryObserverString, new StringValueMapper());
    secondaryObserver = resource(discrete(Y));
    registerVector(registrar, "SecondaryObserverVector", secondaryObserver);
    primaryTarget = resource(discrete("SUN"));
    registrar.discrete("PrimaryTarget", primaryTarget, new StringValueMapper());
    secondaryTarget = resource(discrete("EARTH"));
    registrar.discrete("SecondaryTarget", secondaryTarget, new StringValueMapper());
  }

  private static DoubleValueMapper dvm = new DoubleValueMapper();
  private static void registerVector(Registrar registrar, String name, Resource<Discrete<Vector3D>> r) {
    registrar.discrete(name + "_X", map(r, v -> v == null ? null : v.getX()), dvm);
    registrar.discrete(name + "_Y", map(r, v -> v == null ? null : v.getY()), dvm);
    registrar.discrete(name + "_Z", map(r, v -> v == null ? null : v.getZ()), dvm);
    registrar.discrete(name + "_magnitude", map(r, v -> v == null ? null : Math.sqrt(v.getX() * v.getX() + v.getY() * v.getY() + v.getZ() * v.getZ())), dvm);
  }

  public String currentToString() {
    return "Pointing axis: " + currentValue(PointingAxis) + ", Pointing Rotation: " + currentValue(PointingRotation) +
           ", Rotation Rate: " + currentValue(RotationRate) +
           ", Primary Observer: " + currentValue(primaryObserverString) + " " + currentValue(primaryObserver) +
           ", Primary Target: " + currentValue(primaryTarget) +
           ", Secondary Observer: " + currentValue(secondaryObserverString) + " " + currentValue(secondaryObserver) +
           ", Secondary Target: " + currentValue(secondaryTarget);
  }

  public static Vector3D X = Vector3D.PLUS_I;
  public static Vector3D NEG_X = Vector3D.MINUS_I;
  public static Vector3D Y = Vector3D.PLUS_J;
  public static Vector3D NEG_Y = Vector3D.MINUS_J;
  public static Vector3D Z = Vector3D.PLUS_K;
  public static Vector3D NEG_Z = Vector3D.MINUS_K;

  private static LinkedHashMap<String, Vector3D> observerString;
  public static Vector3D observerForString(String s) {
    if (observerString == null) {
      observerString = new LinkedHashMap<>();
      observerString.put("X", X);
      observerString.put("Y", Y);
      observerString.put("Z", Z);
      observerString.put("PLUS_X", X);
      observerString.put("PLUS_Y", Y);
      observerString.put("PLUS_Z", Z);
      observerString.put("NEG_X", NEG_X);
      observerString.put("NEG_Y", NEG_Y);
      observerString.put("NEG_Z", NEG_Z);
      observerString.put("MINUS_X", NEG_X);
      observerString.put("MINUS_Y", NEG_Y);
      observerString.put("MINUS_Z", NEG_Z);
    }
    return observerString.get(s);
  }
}
