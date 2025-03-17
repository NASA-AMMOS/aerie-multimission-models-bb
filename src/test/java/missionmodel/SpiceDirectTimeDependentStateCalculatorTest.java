package missionmodel;

import missionmodel.geometry.directspicecalls.SpiceDirectTimeDependentStateCalculator;
import missionmodel.geometry.interfaces.GeometryInformationNotAvailableException;
import missionmodel.geometry.returnedobjects.*;
import missionmodel.geometry.spiceinterpolation.Body;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gov.nasa.jpl.time.Time;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;
import java.util.HashMap;

import missionmodel.spice.Spice;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import spice.basic.SpiceErrorException;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(Lifecycle.PER_CLASS)
public class SpiceDirectTimeDependentStateCalculatorTest {

  public static final Path VERSIONED_KERNELS_ROOT_DIRECTORY = Path.of(System.getenv().getOrDefault("SPICE_DIRECTORY", "spice/kernels"));

  public static final String NAIF_META_KERNEL_PATH = VERSIONED_KERNELS_ROOT_DIRECTORY.toString() + "/latest_meta_kernel.tm";
  static SpiceDirectTimeDependentStateCalculator stateCalculatorNoCaching;
  static SpiceDirectTimeDependentStateCalculator stateCalculatorCaching;

  private final Time t = new Time("2024-01-02T00:00:00");
  private final String sc_id = "-74"; // MRO
  private final String target = "MARS";
  private final String abcorr = "LT+S";

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
    stateCalculatorNoCaching = new SpiceDirectTimeDependentStateCalculator(listOfBodies, false);
  }

  @Test
  public void testGetState() {
    System.out.println("testGetState() start");
    try{
      Vector3D[][] calcs = new Vector3D[][]{
        stateCalculatorCaching.getState(t, sc_id, target, abcorr),
        stateCalculatorCaching.getState(t, sc_id, target, abcorr),
        stateCalculatorNoCaching.getState(t, sc_id, target, abcorr),
      };

      // Results from MATLAB test script (test_mro_geom.m)
      // MRO State at 2024-01-02 00:00:00 UTC
      // X pos: = 834.60108
      // Y pos: = -720.70159
      // Z pos: = 3459.16649
      for(int i = 0; i<calcs.length; i++) {
        assertEquals(834.60108, calcs[i][0].getX(), 0.001);
        assertEquals(-720.70159, calcs[i][0].getY(), 0.001);
        assertEquals(3459.16649, calcs[i][0].getZ(), 0.001);
      }
    }
    catch(GeometryInformationNotAvailableException e){
      fail();
    }
    System.out.println("testGetState() passes");
  }

  @Test
  public void testGetRange() {
    System.out.println("testGetRange() start");
    try{

      // Results from MATLAB test script (test_mro_geom.m)
      // MRO Range at 2024-01-02 00:00:00 UTC
      // Range: = 3630.67522
      double[] calcs = new double[]{
        stateCalculatorCaching.getRange(t, sc_id, target, abcorr),
        stateCalculatorCaching.getRange(t, sc_id, target, abcorr),
        stateCalculatorNoCaching.getRange(t, sc_id, target, abcorr)
      };

      for(int i = 0; i<calcs.length; i++) {
        assertEquals(3630.67522, calcs[i], 0.001);
      }
    }
    catch(GeometryInformationNotAvailableException e){
      fail();
    }
    System.out.println("testGetRange() passes");
  }

  @Test
  public void testGetSpeed() {
    System.out.println("testGetSpeed() start");
    // Results from MATLAB test script (test_mro_geom.m)
    // MRO Speed at 2024-01-02 00:00:00 UTC
    // Speed: = 3.44354
    try{
      double[] calcs = new double[]{
        stateCalculatorCaching.getSpeed(t, sc_id, target, abcorr),
        stateCalculatorCaching.getSpeed(t, sc_id, target, abcorr),
        stateCalculatorNoCaching.getSpeed(t, sc_id, target, abcorr)
      };

      for(int i = 0; i<calcs.length; i++) {
        assertEquals(3.44354, calcs[i], 0.001);
      }
    }
    catch(GeometryInformationNotAvailableException e){
      fail();
    }
    System.out.println("testGetSpeed() passes");
  }

  @Test
  public void testGetSpacecraftAltitude() {
    System.out.println("testGetSpacecraftAltitude() start");
    // Results from MATLAB test script (test_mro_geom.m)
    // MRO Altitude at 2024-01-02 00:00:00 UTC
    // Altitude: = 252.27442
    try{
      double[] calcs = new double[]{
        stateCalculatorCaching.getSpacecraftAltitude(t, sc_id, target, abcorr, false),
        stateCalculatorCaching.getSpacecraftAltitude(t, sc_id, target, abcorr, false),
        stateCalculatorNoCaching.getSpacecraftAltitude(t, sc_id, target, abcorr, false)
      };

      for(int i = 0; i<calcs.length; i++) {
        assertEquals(252.27442, calcs[i], 0.001);
      }
    }
    catch(GeometryInformationNotAvailableException e){
      fail();
    }
    System.out.println("testGetSpacecraftAltitude() passes");
  }

  @Test
  public void testGetSunBodySpacecraftAngle() {
    System.out.println("testGetSunBodySpacecraftAngle() start");
    // Results from MATLAB test script (test_mro_geom.m)
    // MRO SEP angle at 2024-01-02 00:00:00 UTC
    // SEP: = 13.01936
    try{
      double[] calcs = new double[]{
        stateCalculatorCaching.getSunBodySpacecraftAngle(t, sc_id, "EARTH", abcorr),
        stateCalculatorCaching.getSunBodySpacecraftAngle(t, sc_id, "EARTH", abcorr),
        stateCalculatorNoCaching.getSunBodySpacecraftAngle(t, sc_id, "EARTH", abcorr),
      };

      for(int i = 0; i<calcs.length; i++) {
        assertEquals(13.01936, calcs[i], 0.001);
      }
    }
    catch(GeometryInformationNotAvailableException e){
      fail();
    }
    System.out.println("testGetSunBodySpacecraftAngle() passes");
  }

  @Test
  public void testGetSunSpacecraftBodyAngle() {
    System.out.println("testGetSunSpacecraftBodyAngle() start");
    // Results from MATLAB test script (test_mro_geom.m)
    // MRO SPE angle at 2024-01-02 00:00:00 UTC
    // SPE: = 8.60335
    try{
      double[] calcs = new double[]{
        stateCalculatorCaching.getSunSpacecraftBodyAngle(t, sc_id, "EARTH", abcorr),
        stateCalculatorCaching.getSunSpacecraftBodyAngle(t, sc_id, "EARTH", abcorr),
        stateCalculatorNoCaching.getSunSpacecraftBodyAngle(t, sc_id, "EARTH", abcorr),
      };

      for(int i = 0; i<calcs.length; i++) {
        assertEquals(8.60335, calcs[i], 0.001);
      }
    }
    catch(GeometryInformationNotAvailableException e){
      fail();
    }
    System.out.println("testGetSunSpacecraftBodyAngle() passes");
  }

  @Test
  public void testGetEarthSpacecraftBodyAngle() {
    System.out.println("testGetEarthSpacecraftBodyAngle() start");
    // Results from MATLAB test script (test_mro_geom.m)
    // MRO Earth-Probe-Target angle at 2024-01-02 00:00:00 UTC
    // Earth-Probe-Target: = 77.57672
    try{
      double[] calcs = new double[]{
        stateCalculatorCaching.getEarthSpacecraftBodyAngle(t, sc_id, target, abcorr),
        stateCalculatorCaching.getEarthSpacecraftBodyAngle(t, sc_id, target, abcorr),
        stateCalculatorNoCaching.getEarthSpacecraftBodyAngle(t, sc_id, target, abcorr),
      };

      for(int i = 0; i<calcs.length; i++) {
        assertEquals(77.57672, calcs[i], 0.001);
      }
    }
    catch(GeometryInformationNotAvailableException e){
      fail();
    }
    System.out.println("testGetEarthSpacecraftBodyAngle() passes");
  }

  @Test
  public void testGetEarthSunProbeAngle() {
    System.out.println("testGetEarthSunProbeAngle() start");
    // Results from MATLAB test script (test_mro_geom.m)
    // MRO ESP angle at 2024-01-02 00:00:00 UTC
    // ESP: = 158.37732
    try{
      double[] calcs = new double[]{
        stateCalculatorCaching.getEarthSunProbeAngle(t, sc_id, abcorr),
        stateCalculatorCaching.getEarthSunProbeAngle(t, sc_id, abcorr),
        stateCalculatorNoCaching.getEarthSunProbeAngle(t, sc_id, abcorr),
      };

      for(int i = 0; i<calcs.length; i++) {
        assertEquals(158.37732, calcs[i], 0.001);
      }
    }
    catch(GeometryInformationNotAvailableException e){
      fail();
    }
    System.out.println("testGetEarthSunProbeAngle() passes");
  }

  @Test
  public void testGetSubPointInformation() {
    System.out.println("testGetSubPointInformation() start");
    // Results from MATLAB test script (test_mro_geom.m)
    // MRO Sub-Spacecraft Point at 2024-01-02 00:00:00 UTC
    // Lat: = -70.54205
    // Long: = -162.87486
    // Radius: = 3378.40081
    try{
      SubPointInformation[] calcs = new SubPointInformation[]{
        stateCalculatorCaching.getSubPointInformation(t, sc_id, target, abcorr, false),
        stateCalculatorCaching.getSubPointInformation(t, sc_id, target, abcorr, false),
        stateCalculatorNoCaching.getSubPointInformation(t, sc_id, target, abcorr, false),
      };

      for(int i = 0; i<calcs.length; i++) {
        LatLonCoord latLonLST = new LatLonCoord(calcs[i].getSpoint());
        assertEquals(-70.54205,  latLonLST.getLatitude()*(180/Math.PI),  0.001);
        assertEquals(-162.87486, latLonLST.getLongitude()*(180/Math.PI), 0.001);
        assertEquals(3378.40081, latLonLST.getRadius(),    0.001);
      }
    }
    catch(GeometryInformationNotAvailableException e){
      fail();
    }
    System.out.println("testGetSubPointInformation() passes");
  }

  @Test
  public void testGetIlluminationAngles() {
    System.out.println("testGetIlluminationAngles() start");
    // Results from MATLAB test script (test_mro_geom.m)
    // MRO Sub-Spacecraft Ilumination Angles at 2024-01-02 00:00:00 UTC
    // Phase: = 104.58995
    // Incidence = 104.46234
    // Emission: = 0.21215
    try{
      IlluminationAngles[] calcs = new IlluminationAngles[]{
        stateCalculatorCaching.getIlluminationAngles(t, sc_id, target, abcorr, false),
        stateCalculatorCaching.getIlluminationAngles(t, sc_id, target, abcorr, false),
        stateCalculatorNoCaching.getIlluminationAngles(t, sc_id, target, abcorr, false),
      };

      for(int i = 0; i<calcs.length; i++) {
        assertEquals( 0.21215, calcs[i].getEmissionAngle(),  0.001);
        assertEquals(104.46234, calcs[i].getIncidenceAngle(), 0.001);
        assertEquals(104.58995, calcs[i].getPhaseAngle(),     0.001);
      }
    }
    catch(GeometryInformationNotAvailableException e){
      fail();
    }
    System.out.println("testGetIlluminationAngles() passes");
  }

//  @Test
//  public void testGetOrbitConicElements() {
//    // Results from MATLAB test script (test_mro_geom.m)
//    // MRO Orbital Elements at 2024-01-02 00:00:00 UTC
//    // Orbital Period = 6694.42529
//    // Inclination: = 92.61727
//    try {
//      OrbitConicElements[] calcs = new OrbitConicElements[]{
//        stateCalculatorCaching.getOrbitConicElements(t, sc_id, target, abcorr),
//        stateCalculatorCaching.getOrbitConicElements(t, sc_id, target, abcorr),
//        stateCalculatorNoCaching.getOrbitConicElements(t, sc_id, target, abcorr),
//      };
//
//      for(int i = 0; i < calcs.length; i++) {
//        assertEquals(6694.42529, calcs[i].getOrbitPeriod(), 0.001);
//        assertEquals(92.61727, calcs[i].getInclination()*(180/Math.PI), 0.001);
//      }
//    }
//    catch(GeometryInformationNotAvailableException e){
//      fail();
//    }
//  }

  @Test
  public void testGetBetaAngle() {
    System.out.println("testGetBetaAngle() start");
    // Results from MATLAB test script (test_mro_geom.m)
    // MRO Beta Angle at 2024-01-02 00:00:00 UTC
    // Beta Angle: = 57.73393
    try{
      double[] calcs = new double[]{
        stateCalculatorCaching.getBetaAngle(t, sc_id, target, abcorr),
        stateCalculatorCaching.getBetaAngle(t, sc_id, target, abcorr),
        stateCalculatorNoCaching.getBetaAngle(t, sc_id, target, abcorr)
      };
      for(int i = 0; i<calcs.length; i++) {
        assertEquals(57.73393, calcs[i], 0.001);
      }

    }
    catch(GeometryInformationNotAvailableException e){
      fail();
    }
    System.out.println("testGetBetaAngle() passes");
  }

  @Test
  public void testGetBodyHalfAngleSize() {
    System.out.println("testGetBodyHalfAngleSize() start");
    // Results from MATLAB test script (test_mro_geom.m)
    // MRO Body Half Angle Size at 2024-01-02 00:00:00 UTC
    // Half Angle Size: = 69.29538
    try{
      double[] calcs = new double[]{
        stateCalculatorCaching.getBodyHalfAngleSize(t, sc_id, target, abcorr),
        stateCalculatorCaching.getBodyHalfAngleSize(t, sc_id, target, abcorr),
        stateCalculatorNoCaching.getBodyHalfAngleSize(t, sc_id, target, abcorr),
      };
      for(int i = 0; i<calcs.length; i++) {
        assertEquals(69.29538, calcs[i], 0.001);
      }

    }
    catch(GeometryInformationNotAvailableException e){
      fail();
    }
    System.out.println("testGetBodyHalfAngleSize() passes");
  }

  @Test
  public void testGetRADec() {
    System.out.println("testGetRADec() start");
    // Results from MATLAB test script (test_mro_geom.m)
    // MRO RA/DEC at 2024-01-02 00:00:00 UTC
    // RA: = -92.49891
    // DEC: = -23.97684
    try {
      RADec[] calcs = new RADec[]{
        stateCalculatorCaching.getRADec(t, "EARTH", sc_id, abcorr),
        stateCalculatorCaching.getRADec(t, "EARTH", sc_id, abcorr),
        stateCalculatorNoCaching.getRADec(t, "EARTH", sc_id, abcorr)
      };

      for(int i = 0; i<calcs.length; i++) {
        assertEquals(-92.49891,calcs[i].getRA(), 0.001);
        assertEquals(-23.97684,calcs[i].getDec(), 0.001);
      }

    } catch (GeometryInformationNotAvailableException e) {
      fail();
    }
    System.out.println("testGetRADec() passes");
  }

  @Test
  public void testGetLST() {
    System.out.println("testGetLST() start");
    // Results from MATLAB test script (test_mro_geom.m)
    // MRO LST at 2024-01-02 00:00:00 UTC
    // LST: = 3.37972
    try {
      double[] calcs = new double[]{
        stateCalculatorCaching.getLST(t, sc_id, target, abcorr, false),
        stateCalculatorCaching.getLST(t, sc_id, target, abcorr, false),
        stateCalculatorNoCaching.getLST(t, sc_id, target, abcorr, false)
      };
      for(int i = 0; i<calcs.length; i++) {
        assertEquals(3.37972, calcs[i], 0.001);
      }
    }
    catch(GeometryInformationNotAvailableException e){
      fail();
    }
    System.out.println("testGetLST() passes");
  }
}
