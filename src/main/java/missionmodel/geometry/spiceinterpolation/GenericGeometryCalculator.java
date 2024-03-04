package missionmodel.geometry.spiceinterpolation;

import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects;
import missionmodel.AbsoluteClock;
import missionmodel.JPLTimeConvertUtility;
import missionmodel.geometry.directspicecalls.SpiceDirectTimeDependentStateCalculator;
import missionmodel.geometry.interfaces.GeometryCalculator;
import missionmodel.geometry.interfaces.GeometryInformationNotAvailableException;
import missionmodel.geometry.interfaces.TimeDependentStateCalculator;
import missionmodel.geometry.resources.GenericGeometryResources;
import missionmodel.geometry.returnedobjects.*;
import gov.nasa.jpl.time.Time;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import spice.basic.SpiceErrorException;

import java.util.Map;

// import static missionmodel.geometry.directspicecalls.SpiceDirectTimeDependentStateCalculator.et2LSTHours;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.set;
import static missionmodel.geometry.resources.GenericGeometryResources.*;

public class GenericGeometryCalculator implements GeometryCalculator {
  protected Map<String, Body> bodies;
  protected final int sc_id;
  protected final String abcorr;
  protected TimeDependentStateCalculator calc;
  protected  AbsoluteClock absClock;

  protected  GenericGeometryResources geomRes;

  public GenericGeometryCalculator(AbsoluteClock absoluteClock, GenericGeometryResources genericGeometryResources, int sc_id, String abcorr){
    this.absClock = absoluteClock;
    this.geomRes = genericGeometryResources;
    this.sc_id = sc_id;
    this.abcorr = abcorr;
  }

  public void setBodies(Map<String, Body> bodies){
    this.bodies = bodies;
    this.calc = new SpiceDirectTimeDependentStateCalculator(bodies, true);
  }

  public void calculateGeometry(Body body) throws GeometryInformationNotAvailableException {
    Vector3D[] bodyPositionAndVelocityWRTSpacecraft = calc.getState(JPLTimeConvertUtility.nowJplTime(absClock), Integer.toString(sc_id), body.getName(), abcorr);
    Vector3D[] sunPositionAndVelocityWRTBody = null;

    // calculate some quantities for every body
    set(geomRes.BODY_POS_ICRF.get(body.getName()), bodyPositionAndVelocityWRTSpacecraft[0]);
    set(geomRes.BODY_VEL_ICRF.get(body.getName()), bodyPositionAndVelocityWRTSpacecraft[1]);
//    BODY_POS_ICRF.get(body.getName()).get("x").set(bodyPositionAndVelocityWRTSpacecraft[0].getX());
//    BODY_POS_ICRF.get(body.getName()).get("y").set(bodyPositionAndVelocityWRTSpacecraft[0].getY());
//    BODY_POS_ICRF.get(body.getName()).get("z").set(bodyPositionAndVelocityWRTSpacecraft[0].getZ());
//    BODY_VEL_ICRF.get(body.getName()).get("x").set(bodyPositionAndVelocityWRTSpacecraft[1].getX());
//    BODY_VEL_ICRF.get(body.getName()).get("y").set(bodyPositionAndVelocityWRTSpacecraft[1].getY());
//    BODY_VEL_ICRF.get(body.getName()).get("z").set(bodyPositionAndVelocityWRTSpacecraft[1].getZ());
    set(geomRes.SpacecraftBodyRange.get(body.getName()), bodyPositionAndVelocityWRTSpacecraft[0].getNorm());
    set(geomRes.SpacecraftBodySpeed.get(body.getName()), bodyPositionAndVelocityWRTSpacecraft[1].getNorm());
    set(geomRes.BodyHalfAngleSize.get(body.getName()), Math.asin(body.getAverageEquitorialRadius()/bodyPositionAndVelocityWRTSpacecraft[0].getNorm())*(180.0/Math.PI));
//    SpacecraftBodyRange.get(body.getName()).set(bodyPositionAndVelocityWRTSpacecraft[0].getNorm());
//    SpacecraftBodySpeed.get(body.getName()).set(bodyPositionAndVelocityWRTSpacecraft[1].getNorm());
//    BodyHalfAngleSize.get(body.getName()).set(Math.asin(body.getAverageEquitorialRadius()/bodyPositionAndVelocityWRTSpacecraft[0].getNorm())*(180.0/Math.PI));

    // this section is also multi-mission; the Sun can't have an angle from itself
    if(!body.getName().equals("SUN")){
      sunPositionAndVelocityWRTBody = calc.getState(JPLTimeConvertUtility.nowJplTime(absClock), body.getName(), "SUN", abcorr);
      set(geomRes.SunSpacecraftBodyAngle.get(body.getName()), Vector3D.angle(bodyPositionAndVelocityWRTSpacecraft[0].add(sunPositionAndVelocityWRTBody[0]),bodyPositionAndVelocityWRTSpacecraft[0])*(180.0/Math.PI));
      set(geomRes.SunBodySpacecraftAngle.get(body.getName()), Vector3D.angle(bodyPositionAndVelocityWRTSpacecraft[0].scalarMultiply(-1.0), sunPositionAndVelocityWRTBody[0])*(180.0/Math.PI));
//      SunSpacecraftBodyAngle.get(body.getName()).set(Vector3D.angle(bodyPositionAndVelocityWRTSpacecraft[0].add(sunPositionAndVelocityWRTBody[0]),bodyPositionAndVelocityWRTSpacecraft[0])*(180.0/Math.PI));
//      SunBodySpacecraftAngle.get(body.getName()).set(Vector3D.angle(bodyPositionAndVelocityWRTSpacecraft[0].scalarMultiply(-1.0), sunPositionAndVelocityWRTBody[0])*(180.0/Math.PI));
    }

    // this section is multi-mission because all missions have to communicate with Earth
//    if(body.getName().equals("EARTH")) {
//      upleg_time.set(Time.upleg(now(), sc_id, bodies.get("EARTH").getNAIFID()).totalSeconds());
//      downleg_time.set(Time.downleg(now(), sc_id, bodies.get("EARTH").getNAIFID()).totalSeconds());
//      RADec scRADec = new RADec(positionResourceToVector3D("EARTH").negate(), new Vector3D(0.0, 0.0, 0.0));
//      spacecraftDeclination.set(scRADec.getDec());
//      spacecraftRightAscension.set(scRADec.getRA());
//      EarthSunProbeAngle.set(180.0 - ((SunBodySpacecraftAngle.get("EARTH").currentval() + SunSpacecraftBodyAngle.get("EARTH").currentval())));
//    }
//
//    // then we calculate things depending if the body was initialized to ask for it
//    if(body.doCalculateRaDec()){
//      Vector3D[] bodyPositionAndVelocityWRTEarth = calc.getState(now(), "EARTH", body.getName(), abcorr);
//      RADec earthRaDec = new RADec(bodyPositionAndVelocityWRTEarth[0], new Vector3D(0.0,0.0,0.0));
//      EarthRaDecByBody.get(body.getName()).get("Ra").set(earthRaDec.getRA());
//      EarthRaDecByBody.get(body.getName()).get("Dec").set(earthRaDec.getDec());
//
//      double spacecraftRAFromEarth = spacecraftRightAscension.currentval();
//      double bodyRAFromEarth = earthRaDec.getRA();
//      EarthRaDeltaWithSCByBody.get(body.getName()).set(Math.min(Math.min(Math.abs(spacecraftRAFromEarth - bodyRAFromEarth), Math.abs(spacecraftRAFromEarth - bodyRAFromEarth + 360)), Math.abs(spacecraftRAFromEarth - bodyRAFromEarth - 360)));
//    }
//
//    if(body.doCalculateEarthSpacecraftBodyAngle()){
//      Vector3D[] earthPositionAndVelocityWRTSC = calc.getState(now(), Integer.toString(sc_id), "EARTH", abcorr);
//      // this also comes in as radians and we want degrees
//      EarthSpacecraftBodyAngle.get(body.getName()).set(Vector3D.angle(earthPositionAndVelocityWRTSC[0], positionResourceToVector3D(body.getName()))*(180.0/Math.PI));
//    }
//
//    if(body.doCalculateBetaAngle() && !body.getName().equals("SUN")){
//      // beta angle is the angle between the vector normal to the orbital plane (sc position x velocity) and the vector from the body to the sun
//      Vector3D orbitPlaneNormal = bodyPositionAndVelocityWRTSpacecraft[0].crossProduct(bodyPositionAndVelocityWRTSpacecraft[1]).normalize();
//      BetaAngleByBody.get(body.getName()).set((Vector3D.angle(orbitPlaneNormal, sunPositionAndVelocityWRTBody[0].negate())*(180.0/Math.PI))-90);
//    }
//
//    if(body.doCalculateSubSolarInformation() && !body.getName().equals("SUN")){
//      SubPointInformation sp_sun = calc.getSubPointInformation(now(), "SUN", body.getName(), abcorr, body.useDSK());
//      LatLonCoord latLonSolarData = new LatLonCoord(sp_sun.getSpoint());
//      // noone talks in radians lat/lon, so we convert to degrees
//      BodySubSolarPoint.get(body.getName()).get("latitude").set(latLonSolarData.getLatitude()*(180.0/Math.PI));
//      BodySubSolarPoint.get(body.getName()).get("longitude").set(latLonSolarData.getLongitude()*(180.0/Math.PI));
//      BodySubSolarPoint.get(body.getName()).get("radius").set(latLonSolarData.getRadius());
//    }
//
//    if(body.doCalculateSubSCPoint() || body.doCalculateIlluminationAngles() || body.doCalculateAltitude()){
//      SubPointInformation sp_sc = calc.getSubPointInformation(now(), Integer.toString(sc_id), body.getName(), abcorr, body.useDSK());
//      if(sp_sc.isFound()) {
//        if(body.doCalculateSubSCPoint() || body.doCalculateAltitude()) {
//          LatLonCoord latLonSurfaceData = new LatLonCoord(sp_sc.getSpoint());
//          BodySubSCPoint.get(body.getName()).get("dist").set(sp_sc.getSrfvec().getNorm());
//          // noone talks in radians lat/lon, so we convert to degrees
//          BodySubSCPoint.get(body.getName()).get("latitude").set(latLonSurfaceData.getLatitude()*(180.0/Math.PI));
//          BodySubSCPoint.get(body.getName()).get("longitude").set(latLonSurfaceData.getLongitude()*(180.0/Math.PI));
//          BodySubSCPoint.get(body.getName()).get("radius").set(latLonSurfaceData.getRadius());
//          if(body.doCalculateAltitude()){
//            SpacecraftAltitude.get(body.getName()).set(bodyPositionAndVelocityWRTSpacecraft[0].getNorm()-latLonSurfaceData.getRadius());
//          }
//
//          if(body.doCalculateLST()){
//            try {
//              BodySubSCPoint.get(body.getName()).get("LST").set(et2LSTHours(now(), body.getNAIFID(), latLonSurfaceData.getLongitude()));
//            } catch (SpiceErrorException e) {
//              throw new GeometryInformationNotAvailableException(e.getMessage());
//            }
//          }
//        }
//
//        if (body.doCalculateIlluminationAngles()) {
//          IlluminationAngles illumAngles = calc.getIlluminationAngles(now(), Integer.toString(sc_id), body.getName(), abcorr, body.useDSK());
//          IlluminationAnglesByBody.get(body.getName()).get("phase").set(illumAngles.getPhaseAngle());
//          IlluminationAnglesByBody.get(body.getName()).get("incidence").set(illumAngles.getIncidenceAngle());
//          IlluminationAnglesByBody.get(body.getName()).get("emission").set(illumAngles.getEmissionAngle());
//        }
//      }
//    }
//
//    if(body.doCalculateOrbitParameters()){
//      OrbitConicElements SCOrbitOfBody = calc.getOrbitConicElements(now(), Integer.toString(sc_id), body.getName(), abcorr);
//      // we only want to set inclination and orbit period if eccentricity is less than 1, because otherwise we're not actually in orbit and we get NaN for orbit period
//      if(SCOrbitOfBody.getEccentricity() < 1) {
//        double semiMajorAxis = SCOrbitOfBody.getPerifocalDistance() / (1 - SCOrbitOfBody.getEccentricity());
//        orbitInclinationByBody.get(body.getName()).set(SCOrbitOfBody.getInclination() * (180.0 / Math.PI));
//        orbitPeriodByBody.get(body.getName()).set(2 * Math.PI * Math.sqrt(Math.pow(semiMajorAxis, 3) / body.getMu()));
//      }
//    }

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
