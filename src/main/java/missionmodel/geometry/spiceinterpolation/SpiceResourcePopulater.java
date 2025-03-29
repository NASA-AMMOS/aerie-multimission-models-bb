package missionmodel.geometry.spiceinterpolation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gov.nasa.jpl.time.Duration;
import gov.nasa.jpl.time.EpochRelativeTime;
import gov.nasa.jpl.time.Time;
import missionmodel.AbsoluteClock;
import missionmodel.JPLTimeConvertUtility;
import missionmodel.Window;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.spawn;
import static missionmodel.config.ConfigObject.jsonObjHasKey;
import static missionmodel.config.RecursiveConfigAccess.getArbitraryJSON;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.io.File;

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

  public SpiceResourcePopulater(GenericGeometryCalculator geoCalc, AbsoluteClock absoluteClock, Window[] dataGaps, Duration paddingAroundDataGaps, String geomPath) {
    InputStreamReader reader = null;
    if (geomPath == null) {
      geomPath = "default_geometry_config.json";
    }
    doStuff("default_geometry_config.json");
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
    try {
      bodiesJsonObject = JsonParser.parseReader(reader).getAsJsonObject();
    } catch (Exception e) {
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

  public SpiceResourcePopulater(GenericGeometryCalculator geoCalc, AbsoluteClock absoluteClock, String geomPath) {
    this(geoCalc, absoluteClock, new Window[0], Duration.ZERO_DURATION, geomPath);
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

  /**
   * Main method for testing
   */
  public static void doStuff(String resourcePath) {
    // Method 1: Find a specific resource
    //String resourcePath = "your-resource.json"; // Replace with your resource path
    URL resourceUrl = SpiceResourcePopulater.class.getClassLoader().getResource(resourcePath);
    System.out.println("Looking for specific resource: " + resourcePath);
    System.out.println("Resource URL: " + resourceUrl);

    if (resourceUrl != null) {
      System.out.println("Resource found at: " + resourceUrl);
    } else {
      System.out.println("Resource not found. Let's try to list available resources...");
    }

    // Method 2: Try different ClassLoaders
    System.out.println("\nTrying different ClassLoaders:");
    tryMultipleClassloaders(resourcePath);

    // Method 3: List all resources in the JAR file
    System.out.println("\nListing all resources in JAR files in the classpath:");
    try {
      List<String> resources = listResourcesFromClasspath();
      System.out.println("Found " + resources.size() + " resources:");
      resources.forEach(resource -> System.out.println(" - " + resource));

      // Filter for JSON files
      System.out.println("\nJSON files found:");
      resources.stream()
        .filter(name -> name.endsWith(".json"))
        .forEach(name -> System.out.println(" - " + name));
    } catch (IOException e) {
      System.err.println("Error listing resources: " + e.getMessage());
      e.printStackTrace();
    }
  }


  /**
   * Try loading a resource using multiple different ClassLoaders
   */
  private static void tryMultipleClassloaders(String resourcePath) {
    // 1. Current class's ClassLoader
    URL url1 = SpiceResourcePopulater.class.getClassLoader().getResource(resourcePath);
    System.out.println("Using ResourceFinder.class.getClassLoader(): " + url1);

    // 2. Current thread's context ClassLoader
    URL url2 = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
    System.out.println("Using Thread.currentThread().getContextClassLoader(): " + url2);

    // 3. System ClassLoader
    URL url3 = ClassLoader.getSystemClassLoader().getResource(resourcePath);
    System.out.println("Using ClassLoader.getSystemClassLoader(): " + url3);

    // 4. Using absolute path (no leading slash)
    URL url4 = SpiceResourcePopulater.class.getClassLoader().getResource(resourcePath);
    System.out.println("Using absolute path (no leading slash): " + url4);

    // 5. Using absolute path (with leading slash)
    URL url5 = SpiceResourcePopulater.class.getClassLoader().getResource("/" + resourcePath);
    System.out.println("Using absolute path (with leading slash): " + url5);
  }

  /**
   * Lists all resources found in JAR files on the classpath
   */
  private static List<String> listResourcesFromClasspath() throws IOException {
    List<String> resources = new ArrayList<>();
    String classpath = System.getProperty("java.class.path");
    String[] classpathEntries = classpath.split(File.pathSeparator);

    for (String classpathEntry : classpathEntries) {
      File file = new File(classpathEntry);

      if (file.isFile() && file.getName().endsWith(".jar")) {
        // Process JAR file
        try (JarFile jarFile = new JarFile(file)) {
          Enumeration<JarEntry> entries = jarFile.entries();
          while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();

            // Skip directories and class files
            if (!entry.isDirectory() && !entryName.endsWith(".class")) {
              resources.add(entryName);
            }
          }
        }
      }
    }

    return resources;
  }

  /**
   * Alternative method to find all resources in the classpath matching a pattern
   */
  private static List<String> findResourcesWithPattern(String pattern) throws IOException {
    List<String> result = new ArrayList<>();
    ClassLoader classLoader = SpiceResourcePopulater.class.getClassLoader();

    // Get all resources with the given name pattern from all locations
    Enumeration<URL> resources = classLoader.getResources(pattern);
    while (resources.hasMoreElements()) {
      URL resource = resources.nextElement();
      String decodedPath = URLDecoder.decode(resource.getPath(), StandardCharsets.UTF_8);
      result.add(decodedPath);
    }

    return result;
  }

  /**
   * Helper method to actually load a resource, if you know the path
   */
  private static String loadResource(String resourcePath) throws IOException {
    URL url = SpiceResourcePopulater.class.getClassLoader().getResource(resourcePath);
    if (url == null) {
      throw new IOException("Resource not found: " + resourcePath);
    }

    // Once you have the URL, you can open a stream and read the content
    // For example with java.nio.file.Files.readString(Path.of(url.toURI()))
    return url.toString();
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
