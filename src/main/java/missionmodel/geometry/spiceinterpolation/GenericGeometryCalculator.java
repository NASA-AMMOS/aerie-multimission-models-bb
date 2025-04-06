package missionmodel.geometry.spiceinterpolation;

import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.time.Time;
import missionmodel.AbsoluteClock;
import missionmodel.JPLTimeConvertUtility;
import missionmodel.geometry.directspicecalls.SpiceDirectTimeDependentStateCalculator;
import missionmodel.geometry.interfaces.GeometryCalculator;
import missionmodel.geometry.interfaces.GeometryInformationNotAvailableException;
import missionmodel.geometry.interfaces.TimeDependentStateCalculator;
import missionmodel.geometry.resources.GenericGeometryResources;
import missionmodel.geometry.returnedobjects.*;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import spice.basic.SpiceErrorException;
import spice.basic.SpiceException;
import spice.basic.SpiceWindow;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.set;
import static missionmodel.JPLTimeConvertUtility.jplTimeFromUTCInstant;
import static missionmodel.geometry.directspicecalls.SpiceDirectTimeDependentStateCalculator.et2LSTHours;
import static missionmodel.spice.Spice.*;

public class GenericGeometryCalculator implements GeometryCalculator {
  protected Map<String, Body> bodies;
  protected final int sc_id;
  protected final String abcorr;
  protected TimeDependentStateCalculator calc;
  protected  AbsoluteClock absClock;

  private  GenericGeometryResources geomRes;

  public Duration spiceStart;
  public Instant planStart;
  public Time spiceStartTime;
  public boolean useLinearResources;

  protected Optional<Registrar> registrar;

  public GenericGeometryCalculator(AbsoluteClock absoluteClock, int sc_id, String abcorr, Instant planStart, boolean useLinearResources, Optional<Registrar> registrar) {
    this.absClock = absoluteClock;
    this.sc_id = sc_id;
    this.abcorr = abcorr;
    this.planStart = planStart;
    this.useLinearResources = useLinearResources;
    this.registrar = registrar;
  }

  public void setBodies(Map<String, Body> bodies){
    this.bodies = bodies;
    this.calc = new SpiceDirectTimeDependentStateCalculator(bodies, true);
    determineSpiceStart();
    this.geomRes = new GenericGeometryResources(registrar, bodies, this);  // TODO -- these 2 classes depend on each other
  }

  /**
   * determine when data is available from SPICE
   */
  private void determineSpiceStart() {
    boolean debug = false;
    // try getCoverage(), which tries to use the SPICE API to determine it
    try {
      SpiceWindow w = getCoverage(sc_id);
      Instant spiceInstant = toUTC(w.getInterval(0)[0]);
      if (debug) System.out.println("determineSpiceStart(): spiceInstant = toUTC(" + w.getInterval(0)[0] + ") = " + spiceInstant);
      this.spiceStart = minus(spiceInstant, planStart);
      if (debug) System.out.println("determineSpiceStart(): spiceStart = " + spiceStart);
      spiceStartTime = d2t(spiceStart);
    } catch (SpiceException e) {
      throw new RuntimeException(e);
    }
    // For some reason SPICE doesn't like queries at the time from getCoverage(), even 1 minute later.
    // Not sure why -- maybe because of light time or aberrations.
    // So, we hunt for the time when we get good values.
    // Not sure error handling works as documented, so this may not work
    int hours = 0;
    while (true) {
      try {
        var t = spiceStartTime.plus(gov.nasa.jpl.time.Duration.fromHours(hours));
        var state = calc.getState(t, Integer.toString(sc_id), "SUN", abcorr);
        if (debug) System.out.println("determineSpiceStart(): t = " + t + ", state = " + (state == null ? "null" : "" + Arrays.deepToString(state)));
        // currently just giving up after 48 hours from the getCoverage() value;
        // Voyager 1 light time was about 23 hours away in 2024
        if (hours < 48 && (state == null || state.length == 0 || state[0] == null))
          hours += 1;
        else break;
      } catch (GeometryInformationNotAvailableException e) {
        if (debug) System.out.println("Got exception trying to determine when spice starts: " + e.getLocalizedMessage());
        if (debug) e.printStackTrace();
        if (hours >= 48) break;
        hours += 1;
      }
    }
    spiceStart = spiceStart.plus(Duration.of(hours, Duration.HOURS));
    spiceStartTime = d2t(spiceStart);
    if (debug) System.out.println("determineSpiceStart(): spiceStartTime = " + spiceStartTime + "(" + spiceStart + ")");
  }

  public Map<String, Body> getBodies(){
    return bodies;
  }

  public GenericGeometryResources getResources() {
    return this.geomRes;
  }

  public void calculateGeometry(Body body) throws GeometryInformationNotAvailableException {
    Vector3D[] bodyPositionAndVelocityWRTSpacecraft = bodyPositionAndVelocityWRTSpacecraft(JPLTimeConvertUtility.nowJplTime(absClock), body.getName());
    Vector3D[] sunPositionAndVelocityWRTBody = null;

    // calculate some quantities for every body
    set(geomRes.BODY_POS_ICRF.get(body.getName()), bodyPositionAndVelocityWRTSpacecraft[0]);
    set(geomRes.BODY_VEL_ICRF.get(body.getName()), bodyPositionAndVelocityWRTSpacecraft[1]);
    set(geomRes.SpacecraftBodyRange.get(body.getName()), bodyPositionAndVelocityWRTSpacecraft[0].getNorm());
    set(geomRes.SpacecraftBodySpeed.get(body.getName()), bodyPositionAndVelocityWRTSpacecraft[1].getNorm());
    set(geomRes.BodyHalfAngleSize.get(body.getName()), Math.asin(body.getAverageEquitorialRadius()/bodyPositionAndVelocityWRTSpacecraft[0].getNorm())*(180.0/Math.PI));

    // this section is also multi-mission; the Sun can't have an angle from itself
    if (!body.getName().equals("SUN")) {
      sunPositionAndVelocityWRTBody = sunPositionAndVelocityWRTBody(JPLTimeConvertUtility.nowJplTime(absClock), body.getName());
      set(geomRes.SunSpacecraftBodyAngle.get(body.getName()), sunSpacecraftBodyAngle(bodyPositionAndVelocityWRTSpacecraft[0], sunPositionAndVelocityWRTBody[0]));
      set(geomRes.SunBodySpacecraftAngle.get(body.getName()), sunBodySpacecraftAngle(bodyPositionAndVelocityWRTSpacecraft[0], sunPositionAndVelocityWRTBody[0]));
    }

    // this section is multi-mission because all missions have to communicate with Earth
    if (body.getName().equals("EARTH")) {
      Double ult = upleg_time(JPLTimeConvertUtility.nowJplTime(absClock));
      set(geomRes.upleg_time, ult);
      set(geomRes.downleg_time, downleg_time(JPLTimeConvertUtility.nowJplTime(absClock)));
      set(geomRes.rtlt, ult + downleg_time(JPLTimeConvertUtility.nowJplTime(absClock).plus(gov.nasa.jpl.time.Duration.fromSeconds(ult))));
      RADec scRADec = scRADec(JPLTimeConvertUtility.nowJplTime(absClock));
      set(geomRes.spacecraftDeclination, scRADec.getDec());
      set(geomRes.spacecraftRightAscension, scRADec.getRA());
      set(geomRes.EarthSunProbeAngle, earthSunProbeAngle(currentValue(geomRes.SunBodySpacecraftAngle.get("EARTH")),
        currentValue(geomRes.SunSpacecraftBodyAngle.get("EARTH"))));
    }

    // then we calculate things depending if the body was initialized to ask for it
    if (body.doCalculateRaDec()) {
      Vector3D[] bodyPositionAndVelocityWRTEarth =
        bodyPositionAndVelocityWRTEarth(JPLTimeConvertUtility.nowJplTime(absClock), body.getName());
      RADec earthRaDec = new RADec(bodyPositionAndVelocityWRTEarth[0], Vector3D.ZERO);
      set(geomRes.EarthRaDecByBody.get(body.getName()).get("Ra"), earthRaDec.getRA());
      set(geomRes.EarthRaDecByBody.get(body.getName()).get("Dec"), earthRaDec.getDec());

      double spacecraftRAFromEarth = currentValue(geomRes.spacecraftRightAscension);
      double bodyRAFromEarth = earthRaDec.getRA();
      set(geomRes.EarthRaDeltaWithSCByBody.get(body.getName()),
        Math.min(Math.min(Math.abs(spacecraftRAFromEarth - bodyRAFromEarth),
            Math.abs(spacecraftRAFromEarth - bodyRAFromEarth + 360)),
          Math.abs(spacecraftRAFromEarth - bodyRAFromEarth - 360)));
    }

    if(body.doCalculateEarthSpacecraftBodyAngle()){
      Vector3D[] earthPositionAndVelocityWRTSC = earthPositionAndVelocityWRTSC(JPLTimeConvertUtility.nowJplTime(absClock));
      // this also comes in as radians and we want degrees
      set(geomRes.EarthSpacecraftBodyAngle.get(body.getName()), Vector3D.angle(earthPositionAndVelocityWRTSC[0],
        currentValue(geomRes.BODY_POS_ICRF.get(body.getName())))*(180.0/Math.PI));
    }

    if(body.doCalculateBetaAngle() && !body.getName().equals("SUN")){
      // beta angle is the angle between the vector normal to the orbital plane (sc position x velocity) and the
      // vector from the body to the sun
      Vector3D orbitPlaneNormal = bodyPositionAndVelocityWRTSpacecraft[0].crossProduct(bodyPositionAndVelocityWRTSpacecraft[1]).normalize();
      set(geomRes.BetaAngleByBody.get(body.getName()), (Vector3D.angle(orbitPlaneNormal, sunPositionAndVelocityWRTBody[0].negate())*(180.0/Math.PI))-90);
    }

    if(body.doCalculateSubSolarInformation() && !body.getName().equals("SUN")){
      SubPointInformation sp_sun = calc.getSubPointInformation(JPLTimeConvertUtility.nowJplTime(absClock),
        "SUN", body.getName(), abcorr, body.useDSK());
      LatLonCoord latLonSolarData = new LatLonCoord(sp_sun.getSpoint());
      // noone talks in radians lat/lon, so we convert to degrees
      set(geomRes.BodySubSolarPoint.get(body.getName()), new Vector3D(
        latLonSolarData.getLatitude()*(180.0/Math.PI),
        latLonSolarData.getLongitude()*(180.0/Math.PI),
           latLonSolarData.getRadius()));
    }

    if(body.doCalculateSubSCPoint() || body.doCalculateIlluminationAngles() || body.doCalculateAltitude()){
      SubPointInformation sp_sc = calc.getSubPointInformation(JPLTimeConvertUtility.nowJplTime(absClock),
        Integer.toString(sc_id), body.getName(), abcorr, body.useDSK());
      if(sp_sc.isFound()) {
        if(body.doCalculateSubSCPoint() || body.doCalculateAltitude()) {
          LatLonCoord latLonSurfaceData = new LatLonCoord(sp_sc.getSpoint());
          set(geomRes.BodySubSCPoint.get(body.getName()).get("dist"), sp_sc.getSrfvec().getNorm());
          // noone talks in radians lat/lon, so we convert to degrees
          set(geomRes.BodySubSCPoint.get(body.getName()).get("latitude"), latLonSurfaceData.getLatitude()*(180.0/Math.PI));
          set(geomRes.BodySubSCPoint.get(body.getName()).get("longitude"), latLonSurfaceData.getLongitude()*(180.0/Math.PI));
          set(geomRes.BodySubSCPoint.get(body.getName()).get("radius"), latLonSurfaceData.getRadius());
          if(body.doCalculateAltitude()){
            set(geomRes.SpacecraftAltitude.get(body.getName()),
              bodyPositionAndVelocityWRTSpacecraft[0].getNorm()-latLonSurfaceData.getRadius());
          }

          if(body.doCalculateLST()){
            try {
              set(geomRes.BodySubSCPoint.get(body.getName()).get("LST"),
                et2LSTHours(JPLTimeConvertUtility.nowJplTime(absClock), body.getNAIFID(), latLonSurfaceData.getLongitude()));
            } catch (SpiceErrorException e) {
              throw new GeometryInformationNotAvailableException(e.getMessage());
            }
          }
        }

        if (body.doCalculateIlluminationAngles()) {
          IlluminationAngles illumAngles = calc.getIlluminationAngles(JPLTimeConvertUtility.nowJplTime(absClock),
            Integer.toString(sc_id), body.getName(), abcorr, body.useDSK());
          set(geomRes.IlluminationAnglesByBody.get(body.getName()).get("phase"), illumAngles.getPhaseAngle());
          set(geomRes.IlluminationAnglesByBody.get(body.getName()).get("incidence"), illumAngles.getIncidenceAngle());
          set(geomRes.IlluminationAnglesByBody.get(body.getName()).get("emission"), illumAngles.getEmissionAngle());
        }
      }
    }

    if(body.doCalculateOrbitParameters()){
      OrbitConicElements SCOrbitOfBody = calc.getOrbitConicElements(JPLTimeConvertUtility.nowJplTime(absClock),
        Integer.toString(sc_id), body.getName(), abcorr);
      // we only want to set inclination and orbit period if eccentricity is less than 1, because otherwise we're not actually in orbit and we get NaN for orbit period
      if(SCOrbitOfBody.getEccentricity() < 1) {
        double semiMajorAxis = SCOrbitOfBody.getPerifocalDistance() / (1 - SCOrbitOfBody.getEccentricity());
        set(geomRes.orbitInclinationByBody.get(body.getName()), SCOrbitOfBody.getInclination() * (180.0 / Math.PI));
        set(geomRes.orbitPeriodByBody.get(body.getName()), 2 * Math.PI * Math.sqrt(Math.pow(semiMajorAxis, 3) / body.getMu()));
      }
    }

  }

  private Time d2t(Duration d) {
    return jplTimeFromUTCInstant(Duration.addToInstant(absClock.startTime, d));
  }

  public double upleg_time(Time t) {
    var dur = Time.upleg(Time.max(t, spiceStartTime), sc_id, bodies.get("EARTH").getNAIFID());
    return dur.totalSeconds();
  }

  public double upleg_duration(Duration t) {
    return upleg_time(d2t(t));
  }

  public double downleg_time(Time t) {
    var dur = Time.downleg(Time.max(t, spiceStartTime), sc_id, bodies.get("EARTH").getNAIFID());
    return dur.totalSeconds();
  }

  public double downleg_duration(Duration t) {
    return downleg_time(d2t(t));
  }

  public Vector3D[] bodyPositionAndVelocityWRTSpacecraft(Time t, String bodyName) {
    try {
      return calc.getState(Time.max(t, spiceStartTime), Integer.toString(sc_id), bodyName, abcorr);
    } catch (GeometryInformationNotAvailableException e) {
      e.printStackTrace();
    }
    return new Vector3D[]{};
  }

  public Vector3D[] bodyPositionAndVelocityWRTSpacecraft(Duration t, String bodyName) {
    return bodyPositionAndVelocityWRTSpacecraft(d2t(t), bodyName);
  }

  public RADec scRADec(Vector3D bodyPosEarth) {
    RADec scRADec = new RADec(bodyPosEarth.negate(), Vector3D.ZERO);
    return scRADec;
  }

  public RADec scRADec(Time t) {
    return scRADec(bodyPositionAndVelocityWRTSpacecraft(Time.max(t, spiceStartTime), "EARTH")[0]);
  }

  public RADec scRADec(Duration t) {
    return scRADec(d2t(t));
  }

  public double spacecraftDeclination(Time t) {
    return scRADec(Time.max(t, spiceStartTime)).getDec();
  }

  public double spacecraftDeclination(Duration t) {
    return spacecraftDeclination(d2t(t));
  }

  public double spacecraftRightAscension(Time t) {
    return scRADec(t).getRA();
  }

  public double spacecraftRightAscension(Duration t) {
    return spacecraftRightAscension(d2t(t));
  }

  Vector3D[] earthPositionAndVelocityWRTSC(Time t) {
    try {
      return calc.getState(Time.max(t, spiceStartTime), Integer.toString(sc_id), "EARTH", abcorr);
    } catch (GeometryInformationNotAvailableException e) {
      e.printStackTrace();
    }
    return new Vector3D[]{};
  }
  public Vector3D[] earthPositionAndVelocityWRTSC(Duration t) {
    return earthPositionAndVelocityWRTSC(d2t(t));
  }

  public Vector3D[] bodyPositionAndVelocityWRTEarth(Time t, String bodyName) {
    try {
      return calc.getState(Time.max(t, spiceStartTime), "EARTH", bodyName, abcorr);
    } catch (GeometryInformationNotAvailableException e) {
      e.printStackTrace();
    }
    return new Vector3D[]{};
  }
  public Vector3D[] bodyPositionAndVelocityWRTEarth(Duration t, String bodyName) {
    return bodyPositionAndVelocityWRTEarth(d2t(t), bodyName);
  }

  public Vector3D[] sunPositionAndVelocityWRTBody(Time t, String bodyName) {
    try {
      return calc.getState(Time.max(t, spiceStartTime), bodyName, "SUN", abcorr);
    } catch (GeometryInformationNotAvailableException e) {
      e.printStackTrace();
    }
    return new Vector3D[]{};
  }
  public Vector3D[] sunPositionAndVelocityWRTBody(Duration t, String bodyName) {
    return sunPositionAndVelocityWRTBody(d2t(t), bodyName);
  }

  public double sunSpacecraftBodyAngle(Vector3D bodyPosWRTSpacecraft, Vector3D sunPosWRTBody) {
    return Vector3D.angle(bodyPosWRTSpacecraft.add(sunPosWRTBody),bodyPosWRTSpacecraft) * (180.0 / Math.PI);
  }
  public double sunSpacecraftBodyAngle(Time t, String bodyName) {
    return sunSpacecraftBodyAngle(
      bodyPositionAndVelocityWRTSpacecraft(t, bodyName)[0],
      sunPositionAndVelocityWRTBody(t, bodyName)[0]);
  }
  public double sunSpacecraftBodyAngle(Duration t, String bodyName) {
    return sunSpacecraftBodyAngle(d2t(t), bodyName);
  }

  public double sunBodySpacecraftAngle(Vector3D bodyPosWRTSpacecraft, Vector3D sunPosWRTBody) {
    return Vector3D.angle(bodyPosWRTSpacecraft.scalarMultiply(-1.0), sunPosWRTBody) * (180.0 / Math.PI);
  }
  public Resource<Double> sunBodySpacecraftAngle(Resource<Linear>[] bodyPosWRTSpacecraft_a, Resource<Linear>[] sunPosWRTBody_a) {
    return ResourceMonad.map(
      bodyPosWRTSpacecraft_a[0], bodyPosWRTSpacecraft_a[1], bodyPosWRTSpacecraft_a[2],
      sunPosWRTBody_a[0], sunPosWRTBody_a[1], sunPosWRTBody_a[2],
      (bpx, bpy, bpz, spx, spy, spz) ->
        Vector3D.angle(new Vector3D(-bpx.extract(), -bpy.extract(), -bpz.extract()),
                       new Vector3D(spx.extract(), spy.extract(), spz.extract())) * (180.0 / Math.PI));
  }
  public double sunBodySpacecraftAngle(Time t, String bodyName) {
    return sunBodySpacecraftAngle(
      bodyPositionAndVelocityWRTSpacecraft(t, bodyName)[0],
      sunPositionAndVelocityWRTBody(t, bodyName)[0]);
  }
  public double sunBodySpacecraftAngle(Duration t, String bodyName) {
    return sunSpacecraftBodyAngle(d2t(t), bodyName);
  }
  public double earthSunProbeAngle(double sunBodySpacecraftAngleDeg, double sunSpacecraftBodyAngleDeg) {
    return 180.0 - (sunBodySpacecraftAngleDeg + sunSpacecraftBodyAngleDeg);
  }
  public double earthSunProbeAngle(Time t) {
    return earthSunProbeAngle(sunBodySpacecraftAngle(t, "EARTH"), sunSpacecraftBodyAngle(t, "EARTH"));
  }
  public double earthSunProbeAngle(Duration t) {
    return earthSunProbeAngle(d2t(t));
  }

//  public static Vector3D positionResourceToVector3D(String body) {
//    return new Vector3D(
//      currentValue(geomRes.BODY_POS_ICRF.get(body);
//      BODY_POS_ICRF.get(body).get("x").currentval(),
//      BODY_POS_ICRF.get(body).get("y").currentval(),
//      BODY_POS_ICRF.get(body).get("z").currentval()
//    );
//  }
}
