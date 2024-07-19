package missionmodel.gnc.activities;

import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;
import gov.nasa.jpl.time.Duration;
import gov.nasa.jpl.time.Time;
import missionmodel.Configuration;
import missionmodel.JPLTimeConvertUtility;
import missionmodel.Mission;
import missionmodel.geometry.directspicecalls.SpiceDirectEventGenerator;
import missionmodel.geometry.interfaces.GeometryInformationNotAvailableException;
import missionmodel.geometry.resources.GenericGeometryResources;
import missionmodel.geometry.spiceinterpolation.Body;
import missionmodel.geometry.spiceinterpolation.GenericGeometryCalculator;
import missionmodel.gnc.blackbird.functions.AttitudeNotAvailableException;
import missionmodel.gnc.blackbird.interfaces.Observer;
import missionmodel.gnc.blackbird.interfaces.Orientation;
import missionmodel.gnc.blackbird.interfaces.Target;
import missionmodel.gnc.blackbird.mmgenerator.GenerateNoRateMatchAttitudeModel;
import missionmodel.gnc.blackbird.observers.CustomObserver;
import missionmodel.gnc.blackbird.observers.SpacecraftInstrumentObserver;
import missionmodel.gnc.blackbird.targets.primary.BodyCenterPrimaryTarget;
import missionmodel.gnc.blackbird.targets.secondary.BodyCenterSecondaryTarget;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.set;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;

@ActivityType("PointToTargetBody")
public class PointingActivity {
  @Export.Parameter
  public String primaryTargetBodyName = "";
  @Export.Parameter
  public String secondaryTargetBodyName = "";

  @ActivityType.EffectModel
  public void run(Mission model) {
    // TODO: Verify time scale is the same as Blackbird (99% yes)
    Time activityStartTime = JPLTimeConvertUtility.nowJplTime(model.absoluteClock);

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
    Double rotation = currentValue(model.gncDataModel.PointingRotation);
    Vector3D axis = currentValue(model.gncDataModel.PointingAxis);
    Orientation startingOrientation = new Orientation(new Rotation(axis, rotation, RotationConvention.VECTOR_OPERATOR));

    // For now, we'll pretend we are looking straight through the spacecraft's Y axis, with a secondary axis straight off Z
    // TODO: Get pointing axis for specific instrument, or from spacecraft geometry
    CustomObserver bbSpacecraftObserver = new CustomObserver(new Vector3D(0, 1, 0));
    CustomObserver bbSpacecraftSecondaryObserver = new CustomObserver(new Vector3D(0, 0, 1));

    // Target body as a BodyCenterPrimaryTarget
    // (breadcrumbs: this is the same calculation as model.geometryResources.BODY_POS_ICRF and .BODY_VEL_ICRF
    BodyCenterPrimaryTarget bbPrimaryTarget = new BodyCenterPrimaryTarget(primaryTargetBodyName,
      Configuration.DEFAULT_SPICE_SCID.toString(), // TODO: In our Mission this is an int ... Blackbird is expecting a String?
      "J2000" // TODO: Are we using J2000?
    );

    // Same for secondary target
    BodyCenterSecondaryTarget bbSecondaryTarget = new BodyCenterSecondaryTarget(secondaryTargetBodyName,
      Configuration.DEFAULT_SPICE_SCID.toString(), // TODO: In our Mission this is an int ... Blackbird is expecting a String?
      "J2000", // TODO: Are we using J2000?
      bbPrimaryTarget
    );

    // This uses the simpler BB model that assumes the slew starts/stops at zero velocity and does not match the current rates
    // TODO: No idea what the appropriate sampling rates and velocities are here - or what the units are
    GenerateNoRateMatchAttitudeModel attitudeModel = new GenerateNoRateMatchAttitudeModel(
      new Vector3D(100,100,100),  // Angular velocity limit - should be set elsewhere in the mission model
      new Vector3D(100, 100, 100), // Angular acceleration limit - should be set elsewhere in the mission model
      gov.nasa.jpl.time.Duration.fromSeconds(1),  // Step size for the forward differencing calculation - 1 second ???
      gov.nasa.jpl.time.Duration.fromSeconds(10) // Sample rate for the returned values - 10 seconds ???
    );

    try {
      System.out.println("Generating Slew");

      // Generate the orientations for this pointing activity
      SortedMap<Time, Orientation> bbSlewData = attitudeModel.getOrientations(
        activityStartTime,
        startingOrientation,
        bbSpacecraftObserver,
        bbPrimaryTarget,
        bbSpacecraftSecondaryObserver,
        bbSecondaryTarget,
        Duration.fromHours(1)   // TODO: Arbitrary max on slew time of 1 hr
      );

      // Spew them out as a series of Aerie DiscreteEffects
      Time endOfActivity = bbSlewData.lastKey();
      System.out.println("End of Activity: " + endOfActivity.toString());

      Time previousTime = bbSlewData.firstKey();
      for (Time t : bbSlewData.keySet()) {
        System.out.println(t);
        System.out.println(bbSlewData.get(t));

        DiscreteEffects.set(model.gncDataModel.IsSlewing, Boolean.TRUE);

        Duration deltaT = t.subtract(previousTime);
        delay(JPLTimeConvertUtility.getDuration(deltaT));
        previousTime = t;

        Orientation newOrientation = bbSlewData.get(t);
        Rotation newRotation = newOrientation.getRotation();
        DiscreteEffects.set(model.gncDataModel.PointingAxis, newRotation.getAxis(RotationConvention.VECTOR_OPERATOR));
        DiscreteEffects.set(model.gncDataModel.PointingRotation, newRotation.getAngle());
      }

      DiscreteEffects.set(model.gncDataModel.IsSlewing, Boolean.FALSE);
    } catch (AttitudeNotAvailableException e) {
      // TODO: Bail?
      e.printStackTrace();
    }
  }
}
