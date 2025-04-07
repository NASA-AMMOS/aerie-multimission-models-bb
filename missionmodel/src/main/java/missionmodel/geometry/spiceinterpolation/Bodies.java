package missionmodel.geometry.spiceinterpolation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import missionmodel.Mission;

import java.io.*;
import java.util.*;
import java.nio.file.Path;

import static missionmodel.config.ConfigObject.jsonObjHasKey;

public class Bodies {

  private JsonObject bodiesJsonObject;
  private HashMap<String, Body> bodies;

  public Bodies() {
    this(null);
  }

  public Bodies(String geomPath) {
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
    JsonObject bodiesJsonObject = null;
    try {
      bodiesJsonObject = JsonParser.parseReader(reader).getAsJsonObject();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return bodiesJsonObject;
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

}
