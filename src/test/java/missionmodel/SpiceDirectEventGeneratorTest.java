package missionmodel;

import missionmodel.geometry.directspicecalls.SpiceDirectEventGenerator;
import missionmodel.geometry.directspicecalls.SpiceDirectTimeDependentStateCalculator;
import missionmodel.geometry.interfaces.GeometryInformationNotAvailableException;
import missionmodel.geometry.returnedobjects.*;
import missionmodel.geometry.spiceinterpolation.Body;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gov.nasa.jpl.time.Time;
import gov.nasa.jpl.time.Duration;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import missionmodel.spice.Spice;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import spice.basic.SpiceErrorException;

import static missionmodel.Debug.debug;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(Lifecycle.PER_CLASS)
public class SpiceDirectEventGeneratorTest {

  public static final Path VERSIONED_KERNELS_ROOT_DIRECTORY = Path.of(System.getenv().getOrDefault("SPICE_DIRECTORY", "spice/kernels"));

  public static final String NAIF_META_KERNEL_PATH = VERSIONED_KERNELS_ROOT_DIRECTORY.toString() + "/latest_meta_kernel.tm";
  static SpiceDirectTimeDependentStateCalculator stateCalculatorCaching;

  static SpiceDirectEventGenerator eventGenerator;

  private final Time t = new Time("2024-01-02T00:00:00");
  private final String sc_id = "-74"; // MRO
  private final String target = "MARS";
  private final String abcorr = "CN";

  @BeforeAll
  static void beforeAll() {
    // The test kernel set has the following valid data interval for MRO, which is the limiting case
    // Body: MARS RECON ORBITER (-74)
    // Start of Interval (ET)              End of Interval (ET)
    // -----------------------------       -----------------------------
    // 2024 JAN 01 00:01:10.000            2024 MAY 06 10:40:00.000
    // The bodies in the test data set (default_geometry_config.json) against which MRO geometry will be calculated are
    // the Sun (10), Earth (399), and Mars (499). Each body has an associated body-fixed frame, iau_<body>.
    try {
      Spice.initialize(NAIF_META_KERNEL_PATH);
    }
    catch (SpiceErrorException e) {
      System.out.println(e.getMessage());
    }

    Body mars = new Body("MARS", 499, "IAU_MARS", .17);
    Body earth = new Body("EARTH", 399, "IAU_EARTH", .30);
    Body sun = new Body("SUN", 10, "IAU_SUN", 1.0);
    HashMap<String, Body> listOfBodies = new HashMap<>();
    listOfBodies.put("MARS", mars);
    listOfBodies.put("EARTH", earth);
    listOfBodies.put("SUN", sun);

    stateCalculatorCaching = new SpiceDirectTimeDependentStateCalculator(listOfBodies, true);
    eventGenerator = new SpiceDirectEventGenerator(listOfBodies);
  }

  @Test
  public void testGetOccultations() {
    if (debug) System.out.println("testGetOccultations() start");
    // Results from MATLAB test script (test_mro_geom.m)
    // Eclipse Times for MRO between 2024-01-02 00:00:00 UTC and 2024-01-02 04:00:00 UTC
    // [757430442.38465, 757432124.34934]
    // [757437175.76832, 757438857.55937]
    List<Window> eclipseWindows1 =  new ArrayList<Window>();
    eclipseWindows1.add( new Window(Time.fromET(757430442.38465), Time.fromET(757432124.34934))  );
    eclipseWindows1.add( new Window(Time.fromET(757437175.76832), Time.fromET(757438857.55937))  );
    // Occultation Times for MRO between 2024-01-02 00:00:00 UTC and 2024-01-02 04:00:00 UTC
    // [757430137.20259, 757432111.62785]
    // [757436869.74876, 757438845.00248]
    List<Window> occultWindows1 =  new ArrayList<Window>();
    occultWindows1.add( new Window(Time.fromET(757430137.20259), Time.fromET(757432111.62785))  );
    occultWindows1.add( new Window(Time.fromET(757436869.74876), Time.fromET(757438845.00248))  );
    // DSS-24 Occultation Times for MRO between 2024-01-02 00:00:00 UTC and 2024-01-02 04:00:00 UTC
    // [757425669.18394, 757426589.34433]
    // [757431344.33722, 757433319.47216]
    // [757438076.76388, 757440052.72965]
    List<Window> DSNStationOccultation1 =  new ArrayList<Window>();
    DSNStationOccultation1.add( new Window(Time.fromET(757425669.18394), Time.fromET(757426589.34433))  );
    DSNStationOccultation1.add( new Window(Time.fromET(757431344.33722), Time.fromET(757433319.47216))  );
    DSNStationOccultation1.add( new Window(Time.fromET(757438076.76388), Time.fromET(757440052.72965))  );
    try {
      List<Window> eclipseWindows2 = eventGenerator.getOccultations(new Time("2024-01-02T00:00:00"), new Time("2024-01-02T04:00:00"), new Duration("0:1:0"), sc_id,      "SUN", target, "CN", true, false, false);
      assertSameWindowListsToWithin(eclipseWindows1, eclipseWindows2, new Duration("00:00:20"));

      List<Window> occultWindows2 = eventGenerator.getOccultations(new Time("2024-01-02T00:00:00"), new Time("2024-01-02T04:00:00"), new Duration("0:1:0"), sc_id,      "EARTH", target, "CN", true, false, false);
      assertSameWindowListsToWithin(occultWindows1, occultWindows2, new Duration("00:00:20"));

      List<Window> DSNStationOccultation2 = eventGenerator.getOccultations(new Time("2024-01-02T00:00:00"), new Time("2024-01-02T04:00:00"), new Duration("0:1:0"), "DSS-24", sc_id, target, "CN", true, true, false);
      assertSameWindowListsToWithin(DSNStationOccultation1, DSNStationOccultation2, new Duration("00:00:20"));

    } catch (GeometryInformationNotAvailableException e) {
      e.printStackTrace();
      fail();
    }
    if (debug) System.out.println("testGetOccultations() passes");
  }

  @Test
  public void testGetPeriapses() {
    if (debug) System.out.println("testGetPeriapses() start");
    // Results from MATLAB test script (test_mro_geom.m)
    // Periapsis Times for MRO between 2024-01-02 00:00:00 UTC and 2024-01-02 04:00:00 UTC
    // 757426028.12514
    // 757432784.65443
    // 757439542.14604
    List<Time> periapses1 =  new ArrayList<Time>();
    periapses1.add(Time.fromET(757426028.12514));
    periapses1.add(Time.fromET(757432784.65443));
    periapses1.add(Time.fromET(757439542.14604));
    try {
      List<Time> periapses2 = eventGenerator.getPeriapses(new Time("2024-01-02T00:00:00"), new Time("2024-01-02T04:00:00"), new Duration("0:5:00"), sc_id,      target, 10000, abcorr);
      assertSameTimeListsToWithin(periapses1, periapses2, new Duration("00:00:06"));
    } catch (GeometryInformationNotAvailableException e) {
      e.printStackTrace();
      fail();
    }
    if (debug) System.out.println("testGetPeriapses() passes");
  }

  @Test
  public void testGetApoapses() {
    if (debug) System.out.println("testGetApoapses() start");
    // Results from MATLAB test script (test_mro_geom.m)
    // Apoapsis Times for MRO between 2024-01-02 00:00:00 UTC and 2024-01-02 04:00:00 UTC
    // 757429409.47531
    // 757436175.16261
    List<Time> apoapses1 =  new ArrayList<Time>();
    apoapses1.add(Time.fromET(757429409.47531));
    apoapses1.add(Time.fromET(757436175.16261));
    try {
      List<Time> apoapses2 = eventGenerator.getApoapses(new Time("2024-01-02T00:00:00"), new Time("2024-01-02T04:00:00"), new Duration("0:5:00"), sc_id,      target, 0, abcorr);
      assertSameTimeListsToWithin(apoapses1, apoapses2, new Duration("00:00:03"));
    } catch (GeometryInformationNotAvailableException e) {
      e.printStackTrace();
      fail();
    }
    if (debug) System.out.println("testGetApoapses() passes");
  }

  @Test
  public void testGetConjunctions() {
    if (debug) System.out.println("testGetConjunctions() start");
    try {
      Spice.initialize(NAIF_META_KERNEL_PATH);
    }
    catch (SpiceErrorException e) {
      System.out.println(e.getMessage());
    }
    // Mars had a conjunction in on 7 Nov 2023 21:14, so we will make sure our search interval covers that time frame
    // Conjunction Times for MARS between 2023-07-01 00:00:00 UTC and 2024-01-02 04:00:00 UTC
    // [752705612.18608, 754420777.63239]
    try {
      List<Window> conjunctions = eventGenerator.getConjunctions(new Time("2024-180T00:00:00"), new Time("2027-001T00:00:00"), new Duration("1:0:0"), "EARTH", "MARS", "SUN", "CN", 3.0);
      assertEquals(1, conjunctions.size());
      Duration conj_dur = conjunctions.get(0).getDuration();
      if (debug) System.out.println(conj_dur);
      assertTrue(new Duration("23T01:41:31.903672").equalToWithin(conjunctions.get(0).getDuration(), Duration.HOUR_DURATION));

    } catch (GeometryInformationNotAvailableException e) {
      e.printStackTrace();
      fail();
    }
    if (debug) System.out.println("testGetConjunctions() passes");
  }

  private void assertSameTimeListsToWithin(List<Time> t1, List<Time> t2, Duration tolerance){
    if(t1.size() != t2.size()){
      System.out.println("Time lists not same size:");
      for(int i = 0; i<Integer.max(t1.size(), t2.size()); i++){
        System.out.println(printTimePair(i < t1.size() ? t1.get(i) : null, i < t2.size() ? t2.get(i) : null));
      }
      fail();
    }

    int firstDiff = -1;
    Duration sumDifference = Duration.ZERO_DURATION;
    Duration maxDifference = Duration.ZERO_DURATION;
    for(int j = 0; j<t1.size(); j++){
      if(t1.get(j).absoluteDifference(t2.get(j)).greaterThan(tolerance)){
        firstDiff = j;
        sumDifference = sumDifference.add(t1.get(j).absoluteDifference(t2.get(j)));
      }
      maxDifference = Duration.max(maxDifference, t1.get(j).absoluteDifference(t2.get(j)));
    }
    if(firstDiff != -1){
      System.out.println("Two time lists differ. First difference outside tolerance at line " + firstDiff);
      System.out.println("Max difference is: " + maxDifference.toString(3));
      System.out.println("Average difference is: " + sumDifference.divide(t1.size()).toString(3));
      for(int i = 0; i<Integer.max(t1.size(), t2.size()); i++){
        System.out.println(printTimePair(i < t1.size() ? t1.get(i) : null, i < t2.size() ? t2.get(i) : null));
      }
      fail();
    }
    assertTrue(true);
  }

  private void assertSameWindowListsToWithin(List<Window> win1, List<Window> win2, Duration tolerance){
    if(win1.size() != win2.size()){
      System.out.println("Window lists not same size:");
      for(int i = 0; i<Integer.max(win1.size(), win2.size()); i++){
        System.out.println(printWindowPair(i < win1.size() ? win1.get(i) : null, i < win2.size() ? win2.get(i): null));
      }
      fail();
    }

    int firstDiff = -1;
    Duration sumDifference = Duration.ZERO_DURATION;
    Duration maxDifference = Duration.ZERO_DURATION;
    for(int j = 0; j<win1.size(); j++){
      if(win1.get(j).getStart().absoluteDifference(win2.get(j).getStart()).greaterThan(tolerance) ||
        win1.get(j).getEnd().absoluteDifference(win2.get(j).getEnd()).greaterThan(tolerance)){
        firstDiff = j;
        sumDifference = sumDifference.add(win1.get(j).getStart().absoluteDifference(win2.get(j).getStart())).add(win1.get(j).getEnd().absoluteDifference(win2.get(j).getEnd()));
      }
      maxDifference = Duration.max(maxDifference, win1.get(j).getStart().absoluteDifference(win2.get(j).getStart()), win1.get(j).getEnd().absoluteDifference(win2.get(j).getEnd()));
    }
    if(firstDiff != -1){
      System.out.println("Two window lists differ. First difference outside tolerance at line " + firstDiff);
      System.out.println("Max difference is: " + maxDifference.toString(3));
      System.out.println("Average difference is: " + sumDifference.divide(2*win1.size()).toString(3));
      for(int i = 0; i<Integer.max(win1.size(), win2.size()); i++){
        System.out.println(printWindowPair(i < win1.size() ? win1.get(i) : null, i < win2.size() ? win2.get(i): null));
      }
      fail();
    }
    assertTrue(true);
  }

  private String printTimePair(Time t1, Time t2){
    String toReturn = "";
    if(t1 != null){
      toReturn =  toReturn + t1.toUTC(3) + "   ";
    }
    else{
      toReturn = toReturn + String.join("", Collections.nCopies(24, " "));
    }
    if(t2 != null){
      toReturn = toReturn + t2.toUTC(3);
    }
    return toReturn;
  }

  private String printWindowPair(Window win1, Window win2){
    String toReturn = "";
    if(win1 != null){
      toReturn =  toReturn + "[" + win1.getStart().toUTC(3) + "," + win1.getEnd().toUTC(3) + "]" + "   ";
    }
    else{
      toReturn = toReturn + String.join("", Collections.nCopies(48, " "));
    }
    if(win2 != null){
      toReturn = toReturn + "[" + win2.getStart().toUTC(3) + "," + win2.getEnd().toUTC(3) + "]";
    }
    return toReturn;
  }

}
