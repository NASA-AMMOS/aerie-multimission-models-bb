package missionmodel.gnc.activities;

import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;
import gov.nasa.jpl.time.Duration;
import gov.nasa.jpl.time.Time;
import missionmodel.JPLTimeConvertUtility;
import missionmodel.Mission;
import missionmodel.gnc.GncDataModel;
import missionmodel.gnc.blackbird.functions.AttitudeNotAvailableException;
import missionmodel.gnc.blackbird.interfaces.Orientation;
import missionmodel.gnc.blackbird.mmgenerator.GenerateAttitudeModel;
import missionmodel.gnc.blackbird.mmgenerator.GenerateNoRateMatchAttitudeModel;
import missionmodel.gnc.blackbird.mmgenerator.GenerateRateMatchAttitudeModel;
import missionmodel.gnc.blackbird.observers.CustomObserver;
import missionmodel.gnc.blackbird.targets.primary.BodyCenterPrimaryTarget;
import missionmodel.gnc.blackbird.targets.secondary.BodyCenterSecondaryTarget;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.util.List;
import java.util.SortedMap;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.set;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

@ActivityType("PointToTargetBody")
public class PointingActivity {
  public static boolean debug = false;
  @Export.Parameter
  public String primaryObserverString = "X";
  public Vector3D primaryObserver = GncDataModel.X; // looked up from primaryObserverString
  @Export.Parameter
  public String secondaryObserverString = "Y";
  public Vector3D secondaryObserver = GncDataModel.Y; // looked up from secondaryObserverString
  @Export.Parameter
  public String primaryTargetBodyName = "SUN";
  @Export.Parameter
  public String secondaryTargetBodyName = "EARTH";

  @ActivityType.EffectModel
  public void run(Mission model) {
    // TODO: Verify time scale is the same as Blackbird (99% yes)
    Time activityStartTime = JPLTimeConvertUtility.nowJplTime(model.absoluteClock);

    primaryObserver = GncDataModel.observerForString(primaryObserverString);
    secondaryObserver = GncDataModel.observerForString(secondaryObserverString);

//    // Get body geometry at the start of this activity
//    Body targetBody = model.spiceResPop.getBodies().get(primaryTargetBodyName);
//    Vector3D bodyPositionWRTSpacecraft = new Vector3D(0, 0, 0);
//    Vector3D bodyVelocityWRTSpacecraft = new Vector3D(0, 0, 0);
//    try {
//      model.geometryCalculator.calculateGeometry(targetBody);
//      bodyPositionWRTSpacecraft = currentValue(model.geometryResources.BODY_POS_ICRF.get(targetBody.getName()));
//      bodyVelocityWRTSpacecraft = currentValue(model.geometryResources.BODY_VEL_ICRF.get(targetBody.getName()));
//    } catch (GeometryInformationNotAvailableException e) {
//      // TODO: Bail?
//      e.printStackTrace();
//    }
//    DiscreteEffects.set(model.gncDataModel.PointingTarget, bodyPositionWRTSpacecraft);
//


    // ---Massage data for the Blackbird GNC model
    // Starting spacecraft orientation as an Orientation
    Vector3D previousPrimaryObserver = currentValue(model.gncDataModel.primaryObserver);
    Vector3D previousSecondaryObserver = currentValue(model.gncDataModel.secondaryObserver);
    String previousPrimaryTarget = currentValue(model.gncDataModel.primaryTarget);
    String previousSecondaryTarget = currentValue(model.gncDataModel.secondaryTarget);
    Double rotation = currentValue(model.gncDataModel.PointingRotationAngle);
    Vector3D axis = currentValue(model.gncDataModel.PointingAxis);
    Vector3D rotationRate = currentValue(model.gncDataModel.RotationRate);
    if (rotationRate == null || rotationRate == Vector3D.NaN) {
      rotationRate = Vector3D.ZERO;
    }
    Orientation startingOrientation = new Orientation(new Rotation(axis, rotation, RotationConvention.VECTOR_OPERATOR), rotationRate);
    if (debug) System.out.println("Slewing from " + model.gncDataModel.currentToString() + ", " + toString(startingOrientation));

    // For now, we'll pretend we are looking straight through the spacecraft's Y axis, with a secondary axis straight off Z
    // TODO: Get pointing axis for specific instrument, or from spacecraft geometry
    CustomObserver bbSpacecraftObserver = new CustomObserver(primaryObserver);
    CustomObserver bbSpacecraftSecondaryObserver = new CustomObserver(secondaryObserver);

    // Target body as a BodyCenterPrimaryTarget
    // (breadcrumbs: this is the same calculation as model.geometryResources.BODY_POS_ICRF and .BODY_VEL_ICRF
    BodyCenterPrimaryTarget bbPrimaryTarget = new BodyCenterPrimaryTarget(primaryTargetBodyName,
      model.configuration.spacecraftIdString(),
      "J2000" // TODO: Are we using J2000?
    );

    // Same for secondary target
    BodyCenterSecondaryTarget bbSecondaryTarget = new BodyCenterSecondaryTarget(secondaryTargetBodyName,
      model.configuration.spacecraftIdString(),
      "J2000", // TODO: Are we using J2000?
      bbPrimaryTarget
    );

    // This uses the simpler BB model that assumes the slew starts/stops at zero velocity and does not match the current rates
    // TODO: No idea what the appropriate sampling rates and velocities are here - or what the units are
    GenerateAttitudeModel attitudeModel;
    if (model.configuration.gncRateMatching()) {
      attitudeModel = new GenerateRateMatchAttitudeModel(
        listToVector(model.configuration.gncAngularVelocityLimit()),
        listToVector(model.configuration.gncAngularAccelerationLimit()),
        gov.nasa.jpl.time.Duration.fromSeconds(1),  // Step size for the forward differencing calculation - 1 second ???
        gov.nasa.jpl.time.Duration.fromSeconds(10)//, // Sample rate for the returned values - 10 seconds ???
//        true,  // whether to throw exception if not enough time for slew
//        true   // whether to truncate
      );
    } else {
      attitudeModel = new GenerateNoRateMatchAttitudeModel(
        listToVector(model.configuration.gncAngularVelocityLimit()),
        listToVector(model.configuration.gncAngularAccelerationLimit()),
        gov.nasa.jpl.time.Duration.fromSeconds(1),  // Step size for the forward differencing calculation - 1 second ???
        gov.nasa.jpl.time.Duration.fromSeconds(10)//, // Sample rate for the returned values - 10 seconds ???
//      true,  // whether to throw exception if not enough time for slew
//      true   // whether to truncate
      );
    }

    try {
      if (debug) System.out.println("Generating Slew");

      // Generate the orientations for this pointing activity
      SortedMap<Time, Orientation> bbSlewData = attitudeModel.getOrientations(
        activityStartTime,
        startingOrientation,
        bbSpacecraftObserver,
        bbPrimaryTarget,
        bbSpacecraftSecondaryObserver,
        bbSecondaryTarget,
        Duration.fromHours(2)   // TODO: Arbitrary max on slew time of 1 hr
      );

      // Spew them out as a series of Aerie DiscreteEffects
      Time endOfActivity = bbSlewData.lastKey();
      if (debug) System.out.println("End of Activity: " + endOfActivity.toString());

      set(model.gncDataModel.primaryObserverString, primaryObserverString);
      set(model.gncDataModel.primaryObserver, primaryObserver);
      set(model.gncDataModel.primaryTarget, primaryTargetBodyName);
      set(model.gncDataModel.secondaryObserver, secondaryObserver);
      set(model.gncDataModel.secondaryObserverString, secondaryObserverString);
      set(model.gncDataModel.secondaryTarget, secondaryTargetBodyName);

      Time previousTime = bbSlewData.firstKey();
      for (Time t : bbSlewData.keySet()) {
        if (debug) System.out.println(t);
        if (debug) System.out.println(toString(bbSlewData.get(t)));

        DiscreteEffects.set(model.gncDataModel.IsSlewing, Boolean.TRUE);

        Duration deltaT = t.subtract(previousTime);
        delay(JPLTimeConvertUtility.getDuration(deltaT));
        previousTime = t;

        Orientation newOrientation = bbSlewData.get(t);
        Rotation newRotation = newOrientation.getRotation();
        DiscreteEffects.set(model.gncDataModel.PointingAxis, newRotation.getAxis(RotationConvention.VECTOR_OPERATOR));
        DiscreteEffects.set(model.gncDataModel.PointingRotationAngle, newRotation.getAngle());
        DiscreteEffects.set(model.gncDataModel.RotationRate, newOrientation.getRotationRate());
      }

      DiscreteEffects.set(model.gncDataModel.IsSlewing, Boolean.FALSE);
    } catch (AttitudeNotAvailableException e) {
      // TODO: Bail?
      e.printStackTrace();
    }
  }

  private static Vector3D listToVector(List<Double> list) {
    return new Vector3D(list.get(0), list.get(1), list.get(2));
  }

  public static String toString(Rotation r) {
    return "Rotation(" + String.join(", ", List.of(""+r.getQ0(), ""+r.getQ1(), ""+r.getQ2(), ""+r.getQ3())) + ")";
  }
  public static String toString(Orientation o) {
    return "Orientation(" + toString(o.getRotation()) + ", " + "RotationRate " + o.getRotationRate() + ")";
  }
}
