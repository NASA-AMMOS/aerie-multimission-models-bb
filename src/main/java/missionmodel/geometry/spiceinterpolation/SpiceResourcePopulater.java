package missionmodel.geometry.spiceinterpolation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gov.nasa.jpl.time.Duration;
import gov.nasa.jpl.time.EpochRelativeTime;
import gov.nasa.jpl.time.Time;
import missionmodel.AbsoluteClock;
import missionmodel.JPLTimeConvertUtility;
import missionmodel.Mission;
import missionmodel.Window;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.spawn;
import static missionmodel.config.ConfigObject.jsonObjHasKey;
import static missionmodel.config.RecursiveConfigAccess.getArbitraryJSON;

//import static gov.nasa.jpl.blackbirdconfig.RecursiveConfigAccess.getArbitraryJSON;
//import static gov.nasa.jpl.geometrymodel.resources.GenericGeometryResources.ComplexRepresentativeStation;

public class SpiceResourcePopulater {
  //private int sc_id;
  private Window[] dataGaps;
  private Duration paddingAroundDataGaps;
  private JsonObject bodiesJsonObject;
  private HashMap<String, Body> bodies;
  private GenericGeometryCalculator geoCalc;

  private AbsoluteClock absClock;

  public SpiceResourcePopulater(GenericGeometryCalculator geoCalc, AbsoluteClock absoluteClock, Window[] dataGaps, Duration paddingAroundDataGaps) {
    try (
      var in = Objects.requireNonNull(Mission.class.getResourceAsStream("default_geometry_config.json"), "default_geometry_config.json not found");
      var reader = new InputStreamReader(in)
    ) {
      bodiesJsonObject = JsonParser.parseReader(reader).getAsJsonObject();
    } catch (IOException e) {
        throw new RuntimeException(e);
    }

    this.bodies = initializeAllBodiesFromJson();
    //this.sc_id = sc_id;
    this.geoCalc = geoCalc;
    this.absClock = absoluteClock;
    this.geoCalc.setBodies(this.bodies);
    this.dataGaps = dataGaps;
    this.paddingAroundDataGaps = paddingAroundDataGaps;
  }

  public SpiceResourcePopulater(GenericGeometryCalculator geoCalc, AbsoluteClock absoluteClock) {
    this(geoCalc, absoluteClock, new Window[0], Duration.ZERO_DURATION);
  }

  public void setDataGaps(Window[] newGaps, Duration newPadding) {
    dataGaps = newGaps;
    paddingAroundDataGaps = newPadding;
  }

  public void calculateTimeDependentInformation(){
    for(Body body : bodies.values()){
      List<CalculationPeriod> calculationPeriods = getCalculationPeriods(body.getName(), "Trajectory");
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

  public HashMap<String, Body> initializeAllBodiesFromJson(){
    HashMap<String, Body> toReturn = new HashMap<>();
    JsonObject jsonObject = bodiesJsonObject.get("bodies").getAsJsonObject();

    Set<Map.Entry<String, JsonElement>> entrySet = jsonObject.entrySet();
    for(Map.Entry<String, JsonElement> entry : entrySet){
      JsonObject body = entry.getValue().getAsJsonObject();
      if(jsonObjHasKey(body, "Trajectory")) {
        JsonObject trajectory = body.get("Trajectory").getAsJsonObject();
        toReturn.put(entry.getKey(), new Body(entry.getKey(),
          body.get("NaifID").getAsInt(),
          body.get("NaifFrame").getAsString(),
          body.get("Albedo").getAsDouble(),
          getIfNonNull(trajectory, "calculateAltitude"),
          getIfNonNull(trajectory, "calculateEarthSpacecraftBodyAngle"),
          getIfNonNull(trajectory, "calculateSubSCInformation"),
          getIfNonNull(trajectory, "calculateRaDec"),
          getIfNonNull(trajectory, "calculateIlluminationAngles"),
          getIfNonNull(trajectory, "calculateSubSolarInformation"),
          getIfNonNull(trajectory, "calculateLST"),
          getIfNonNull(trajectory, "calculateBetaAngle"),
          getIfNonNull(trajectory, "calculateOrbitParameters"),
          getIfNonNull(trajectory, "useDSK")));
      }
      else{
        toReturn.put(entry.getKey(), new Body(entry.getKey(),
          body.get("NaifID").getAsInt(),
          body.get("NaifFrame").getAsString(),
          body.get("Albedo").getAsDouble()
        ));
      }
    }

   return toReturn;
  }

  private boolean getIfNonNull(JsonObject obj, String key){
    return obj.get(key) != null && !obj.get(key).isJsonNull() ? obj.get(key).getAsBoolean() : false;
  }

  public HashMap<String, Body> getBodies(){
    return bodies;
  }

  private static Window[] getWindowsWithData(Time begin, Time end, Window[] gaps, Duration paddingAroundDataGaps){
    // potentially need to splice up calculation window into multiple smaller windows to cut out around periods where SPKs or other input files don't have relevant information
    List<Window> paddedWindowsWithData = new ArrayList<>();
    Window[] unpaddedWindowsWithData = Window.and(new Window[]{new Window(begin, end)}, Window.not(gaps, begin, end));
    for(Window w : unpaddedWindowsWithData){
      paddedWindowsWithData.add(new Window(
        w.getStart().equals(begin) ? w.getStart() : w.getStart().add(paddingAroundDataGaps),
        w.getEnd().equals(end)     ? w.getEnd()   : w.getEnd().subtract(paddingAroundDataGaps)
      ));
    }
    return paddedWindowsWithData.toArray(new Window[paddedWindowsWithData.size()]);
  }

  public List<CalculationPeriod> getCalculationPeriods(String bodyname, String geometryType){
    List<CalculationPeriod> toReturn = new ArrayList<>();
    List<String> indices = Arrays.asList("bodies", bodyname, geometryType, "calculationPeriods");
    JsonElement calculationPeriods = getArbitraryJSON(bodiesJsonObject, indices);
    if(calculationPeriods != null) {
      for (JsonElement period : calculationPeriods.getAsJsonArray()) {
        JsonObject periodStruct = period.getAsJsonObject();
        for (Window dataWindow : getWindowsWithData(EpochRelativeTime.getAbsoluteOrRelativeTime(periodStruct.get("begin").getAsString()), EpochRelativeTime.getAbsoluteOrRelativeTime(periodStruct.get("end").getAsString()), dataGaps, paddingAroundDataGaps)) {
          Duration minTimeStep = jsonObjHasKey(periodStruct, "minTimeStep") ? new Duration(periodStruct.get("minTimeStep").getAsString()) : Duration.SECOND_DURATION;
          Duration maxTimeStep = jsonObjHasKey(periodStruct, "maxTimeStep") ? new Duration(periodStruct.get("maxTimeStep").getAsString()) : Duration.DAY_DURATION;
          double threshold =     jsonObjHasKey(periodStruct, "threshold")   ? periodStruct.get("threshold").getAsDouble() : 0.0;

          toReturn.add(new CalculationPeriod(new Time(dataWindow.getStart()), new Time(dataWindow.getEnd()),
            minTimeStep, maxTimeStep, threshold));
        }
      }
    }
    return toReturn;
  }

}
