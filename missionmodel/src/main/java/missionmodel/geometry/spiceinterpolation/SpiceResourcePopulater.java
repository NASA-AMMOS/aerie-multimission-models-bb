package missionmodel.geometry.spiceinterpolation;

import com.google.gson.*;

//import gov.nasa.jpl.geometrymodel.activities.spawner.AddApoapsis;
//import gov.nasa.jpl.geometrymodel.activities.spawner.AddOccultations;
//import gov.nasa.jpl.geometrymodel.activities.spawner.AddPeriapsis;
//import gov.nasa.jpl.geometrymodel.activities.spawner.AddSpacecraftEclipses;
import gov.nasa.jpl.time.Duration;
import missionmodel.AbsoluteClock;
import missionmodel.JPLTimeConvertUtility;
import missionmodel.Window;
//import gov.nasa.jpl.scheduler.Window;
//import gov.nasa.jpl.time.Duration;
//import gov.nasa.jpl.time.EpochRelativeTime;
//import gov.nasa.jpl.time.Time;

import java.util.*;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.spawn;

import java.util.List;

//import static gov.nasa.jpl.blackbirdconfig.RecursiveConfigAccess.getArbitraryJSON;
//import static gov.nasa.jpl.geometrymodel.resources.GenericGeometryResources.ComplexRepresentativeStation;

public class SpiceResourcePopulater {
  public static boolean debug = false;
  private final Bodies bodiesObj;
  //private int sc_id;
  private JsonObject bodiesJsonObject;
  private HashMap<String, Body> bodies;
  private GenericGeometryCalculator geoCalc;

  private AbsoluteClock absClock;

  //public SpiceResourcePopulater(String filename, int sc_id, GeometryCalculator geoCalc, Window[] dataGaps, Duration paddingAroundDataGaps) {
  public SpiceResourcePopulater(GenericGeometryCalculator geoCalc, AbsoluteClock absoluteClock, Window[] dataGaps, Duration paddingAroundDataGaps, String geomPath) {
    bodiesObj = new Bodies(geomPath, dataGaps, paddingAroundDataGaps);
    this.bodiesJsonObject = bodiesObj.getBodiesJson();
    this.bodies = bodiesObj.getBodiesMap();
    //this.sc_id = sc_id;
    this.geoCalc = geoCalc;
    this.absClock = absoluteClock;
    this.geoCalc.setBodies(this.bodies);
  }

  public void calculateTimeDependentInformation(){
    for(Body body : bodies.values()){
      List<CalculationPeriod> calculationPeriods = getBodies().getCalculationPeriods(body.getName(), "Trajectory");
      for(CalculationPeriod calculationPeriod : calculationPeriods) {
        BodyGeometryGenerator bodyGeoGenerator = new BodyGeometryGenerator(
          absClock, geoCalc.getResources(), JPLTimeConvertUtility.jplTimeFromUTCInstant(absClock.now()), body.getName(),
          calculationPeriod.getThreshold(), calculationPeriod.getMinTimeStep(), calculationPeriod.getMaxTimeStep(), "", geoCalc, bodies);
        spawn(bodyGeoGenerator::model);
      }
    }
  }

//  public void calculateEvents(){
//    addApoapsisActivities();
//    addPeriapsisActivities();
//    addOccultationActivities();
//    addEclipseActivities();
//  }
//
//  public void addPeriapsisActivities(){
//    for(Body body : bodies.values()) {
//      List<CalculationPeriod> calculationPeriods = getCalculationPeriods(body.getName(), "Periapsis", this.paddingAroundDataGaps);
//      for(CalculationPeriod calculationPeriod : calculationPeriods) {
//        new AddPeriapsis(calculationPeriod.getStart(), calculationPeriod.getDuration(), Integer.toString(sc_id),
//          body.getName(), calculationPeriod.getMaxTimeStep(), calculationPeriod.getThreshold())
//          .decompose();
//      }
//    }
//  }
//
//  public void addApoapsisActivities(){
//    for(Body body : bodies.values()) {
//      List<CalculationPeriod> calculationPeriods = getCalculationPeriods(body.getName(), "Apoapsis", this.paddingAroundDataGaps);
//      for(CalculationPeriod calculationPeriod : calculationPeriods) {
//        new AddApoapsis(calculationPeriod.getStart(), calculationPeriod.getDuration(), Integer.toString(sc_id),
//          body.getName(), calculationPeriod.getMaxTimeStep(), calculationPeriod.getThreshold())
//          .decompose();
//      }
//    }
//  }
//
//  public void addOccultationActivities(){
//    for(Body body : bodies.values()) {
//      List<CalculationPeriod> calculationPeriods = getCalculationPeriods(body.getName(), "Occultations", this.paddingAroundDataGaps);
//      for(CalculationPeriod calculationPeriod : calculationPeriods) {
//        for(String station : ComplexRepresentativeStation.values()) {
//          new AddOccultations(calculationPeriod.getStart(), calculationPeriod.getDuration(), station, Integer.toString(sc_id), body.getName(),
//            calculationPeriod.getMaxTimeStep(), body.useDSK())
//            .decompose();
//        }
//      }
//    }
//  }
//
//  public void addEclipseActivities(){
//    for(Body body : bodies.values()) {
//      List<CalculationPeriod> calculationPeriods = getCalculationPeriods(body.getName(), "SolarEclipses", this.paddingAroundDataGaps);
//      for(CalculationPeriod calculationPeriod : calculationPeriods) {
//        new AddSpacecraftEclipses(calculationPeriod.getStart(), calculationPeriod.getDuration(), Integer.toString(sc_id), "SUN", body.getName(),
//          calculationPeriod.getMaxTimeStep(), body.useDSK())
//          .decompose();
//      }
//    }
//  }

  public HashMap<String, Body> getBodiesMap(){
    return bodies;
  }
  public Bodies getBodies(){
    return bodiesObj;
  }


}
