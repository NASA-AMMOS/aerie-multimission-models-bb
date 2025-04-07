package missionmodel.geometry.spiceinterpolation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gov.nasa.jpl.time.Duration;
import gov.nasa.jpl.time.EpochRelativeTime;
import gov.nasa.jpl.time.Time;
import missionmodel.Window;

import java.io.*;
import java.util.*;
import java.nio.file.Path;

import static missionmodel.config.ConfigObject.jsonObjHasKey;
import static missionmodel.config.RecursiveConfigAccess.getArbitraryJSON;

public class Bodies {

  private JsonObject bodiesJsonObject;
  private HashMap<String, Body> bodies;
  private Window[] dataGaps;
  private Duration paddingAroundDataGaps;

  public Bodies() {
    this(null, new Window[0], Duration.ZERO_DURATION);
  }

  public Bodies(String geomPath, Window[] dataGaps, Duration paddingAroundDataGaps) {
    this.dataGaps = dataGaps;
    this.paddingAroundDataGaps = paddingAroundDataGaps;
    this.bodiesJsonObject = parseBodiesFromJson(geomPath);
    this.bodies = initializeAllBodiesFromJson(this.bodiesJsonObject);
  }

  public HashMap<String, Body> getBodiesMap(){
    return bodies;
  }

  public JsonObject getBodiesJson(){
    return bodiesJsonObject;
  }

  private JsonObject parseBodiesFromJson(String geomPath) {
    InputStreamReader reader = null;
    if (geomPath == null) {
      geomPath = "default_geometry_config.json";
    }
    InputStream in = null;
    InputStream r = null;
    try {
      reader = new FileReader(geomPath);
    } catch(FileNotFoundException fnfe) {
      String fileName = Path.of(geomPath).getFileName().toString();
      try {
        r = SpiceResourcePopulater.class.getClassLoader().getResourceAsStream(fileName);
        in = Objects.requireNonNull(r, "file " + geomPath + " and resource " + fileName + " not found");//" in " + GenericGeometryCalculator.class.getClassLoader().);
      } catch (NullPointerException e) {// | IOException e) {
        throw new RuntimeException(e);
      }
      reader = new InputStreamReader(in);
    }
    JsonObject bodiesJson = null;
    try {
      bodiesJson = JsonParser.parseReader(reader).getAsJsonObject();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return bodiesJson;
  }

  public HashMap<String, Body> initializeAllBodiesFromJson(JsonObject bodiesJson){
    HashMap<String, Body> toReturn = new HashMap<>();
    JsonObject jsonObject = bodiesJson.get("bodies").getAsJsonObject();

    Set<Map.Entry<String, JsonElement>> entrySet = jsonObject.entrySet();
    for(Map.Entry<String, JsonElement> entry : entrySet){
      JsonObject body = entry.getValue().getAsJsonObject();
      if(jsonObjHasKey(body, "Trajectory")) {
        JsonObject trajectory = body.get("Trajectory").getAsJsonObject();
        List<CalculationPeriod> calculationPeriods = getCalculationPeriods(entry.getKey(), "Trajectory");
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
          getIfNonNull(trajectory, "useDSK"),
          calculationPeriods
          ));
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

  public void setDataGaps(Window[] newGaps, Duration newPadding) {
    dataGaps = newGaps;
    paddingAroundDataGaps = newPadding;
  }

}
