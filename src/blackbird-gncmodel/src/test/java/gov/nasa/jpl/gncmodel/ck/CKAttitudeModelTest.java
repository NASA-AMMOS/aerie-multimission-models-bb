package gov.nasa.jpl.gncmodel.ck;


import gov.nasa.jpl.gncmodel.functions.AttitudeNotAvailableException;
import gov.nasa.jpl.gncmodel.interfaces.ADCModel;
import gov.nasa.jpl.gncmodel.interfaces.Orientation;
import gov.nasa.jpl.spice.Spice;
import junit.framework.TestCase;
import gov.nasa.jpl.engine.Setup;
import gov.nasa.jpl.time.Duration;
import gov.nasa.jpl.time.Time;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.junit.Before;
import spice.basic.CSPICE;
import spice.basic.SpiceErrorException;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import gov.nasa.jpl.gncmodel.functions.AttitudeFunctions;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static gov.nasa.jpl.gncmodel.functions.AttitudeFunctions.getFixedFrameRotationWithSpice;


public class CKAttitudeModelTest extends TestCase {

    @Before
    public void setUp(){
        Setup.initializeEngine();
    }

    private void initializePsyche(){

        String path = "src/test/resources/gov/nasa/jpl/gncmodel/ck/psyche/";
        String LSK = "naif0012.tls";
        String SCLK = "PSYC_69_SCLKSCET.00000.tsc";
        String FK = "psyche_fk_v04.tf";
        String IK1 = "psyche_struct_v01.ti";
        String IK2 = "psyche_imager_v02.ti";
        String SPK1 = "psyc_260220_260417_R811_i90_res70-9_gravity6_181015.bsp";
        String SPK2 = "psyc_260504_260811_R401_i90_res46-17_gravity6_181015.bsp";
        String SPK3 = "de421.bsp";
        String SPK4 = "sb_psyche_ssd_180411.bsp";
        String CK1 = "single_turn.bc";
        String CK2 = "PsycheOrbitA_PSYC_IMGA_wp0.bc";

        try {
            Spice.loadKernel(path.concat(LSK));
            Spice.loadKernel(path.concat(SCLK));
            Spice.loadKernel(path.concat(FK));
            Spice.loadKernel(path.concat(IK1));
            Spice.loadKernel(path.concat(IK2));
            Spice.loadKernel(path.concat(SPK1));
            Spice.loadKernel(path.concat(SPK2));
            Spice.loadKernel(path.concat(SPK3));
            Spice.loadKernel(path.concat(SPK4));
            Spice.loadKernel(path.concat(CK1));
            Spice.loadKernel(path.concat(CK2));
        } catch (SpiceErrorException e) {
            e.printStackTrace();
        }
    }

    private void initializeCassini(){
        // Define kernel path and kernel names
        String path = "src/test/resources/gov/nasa/jpl/gncmodel/ck/cassini/";
        String LSK = "naif0008.tls";
        String SCLK = "cas00084.tsc";
        String FK = "cas_v37.tf";
        String CK1 = "04135_04171pc_psiv2.bc";
        String SPK1 = "020514_SE_SAT105.bsp";
        String SPK2 = "030201AP_SK_SM546_T45.bsp";
        String SPK3 = "981005_PLTEPH-DE405S.bsp";

        try {
            Spice.loadKernel(path.concat(LSK));
            Spice.loadKernel(path.concat(SCLK));
            Spice.loadKernel(path.concat(FK));
            Spice.loadKernel(path.concat(CK1));
            Spice.loadKernel(path.concat(SPK1));
            Spice.loadKernel(path.concat(SPK2));
            Spice.loadKernel(path.concat(SPK3));
        } catch (SpiceErrorException e) {
            e.printStackTrace();
        }
    }

    public void testGetOrientation() {
        // Test 1: Remote Sensing Orientation Lesson from NAIF
        initializeCassini();

        // The ephemeris time of the desired quaternion
        Time et = new Time("2004 jun 11 19:32:00");
        double[] positionI = new double[3];
        double[] lightTime = new double[1];
        try {
            CSPICE.spkpos("Earth", et.toET(), "J2000", "LT+S", "CASSINI", positionI, lightTime);
        } catch (SpiceErrorException e) {
            e.printStackTrace();
        }

        // Initialize a CKADCModel with the needed parameters
        String hgaFrame = "CASSINI_HGA";
        ADCModel ckadcModel = new CKAttitudeModel("-82000", "J2000", Duration.MINUTE_DURATION.multiply(5), true);

       // Use a tolerance of 0.001 because NAIF rounded answer to three decimal points
        try {
            assertEquals(71.924, AttitudeFunctions.angleBetweenObjectAndBoresight(ckadcModel.getOrientation(et, null,null, null, null).getRotation(), getFixedFrameRotationWithSpice(hgaFrame, "CASSINI_SC_COORD"),new Vector3D(positionI)), 0.001);
        } catch (AttitudeNotAvailableException | SpiceErrorException e) {
            fail();
        }
    }

    public void testGetOrientations() {
        /*
         * here we want to test the single_turn.bc kernel, which has the following properties according to ckbrief:
         *
         * Object:  -69000
         *   Interval Begin ET        Interval End ET          AV
         *   ------------------------ ------------------------ ---
         *   2026-MAY-04 05:16:08.185 2026-MAY-04 06:19:08.185 Y
         *
         *
         *  This is intended to be a turn FROM IMGA PSYCHE nadir pointed TO HGA Earth-pointed
         */

        initializePsyche();

        String obsBody = "PSYC";
        String scBaseFrame = "PSYC_SPACECRAFT";
        String referenceFrame = "J2000";
        Time etStart = new Time("2026 MAY 04 05:16:09");
        Duration turnDuration = new Duration("00:45:00");

        ADCModel ckadcModel = new CKAttitudeModel(scBaseFrame, referenceFrame, Duration.MINUTE_DURATION.multiply(5), false);

        SortedMap<Time, Orientation> orientations = null;
        try {
            orientations = ckadcModel.getOrientations(etStart, null, null, null, null, null, turnDuration);
        } catch (AttitudeNotAvailableException e) {
            fail();
        }

        // test that since we are sampling a 45 minute turn every 5 minutes, there are 10 orientations returned
        assertEquals(10, orientations.size());


        // Test with showing that the angle between IMGA boresight vector and (16) Psyche vector isn't large at beginning of track
        double[] positionPSYCHE = new double[3];
        double[] lightTime = new double[1];

        try {
            CSPICE.spkpos("PSYCHE", etStart.toET(), referenceFrame, "LT+S", obsBody, positionPSYCHE, lightTime);
        } catch (SpiceErrorException e) {
            e.printStackTrace();
        }
        assertEquals(0.0, AttitudeFunctions.angleBetweenObjectAndBoresight(orientations.get(etStart).getRotation(), getFixedFrameRotationWithSpiceCrashOnError("PSYC_IMGA",scBaseFrame), new Vector3D(positionPSYCHE)),0.00001);

        // Test with showing that the angle between HGA boresight vector and Earth vector isn't large at end of track
        double[] positionEarth = new double[3];

        try {
            CSPICE.spkpos("EARTH", orientations.lastKey().toET(), referenceFrame, "LT+S", obsBody, positionEarth, lightTime);
        } catch (SpiceErrorException e) {
            e.printStackTrace();
        }

        assertEquals(0.0, AttitudeFunctions.angleBetweenObjectAndBoresight(orientations.get(orientations.lastKey()).getRotation(), getFixedFrameRotationWithSpiceCrashOnError("PSYC_HGA", scBaseFrame), new Vector3D(positionEarth)),0.00001);
    }

    public void testAttitudeEffects(){
        // we are trying to find which LGA is closest to pointing at Earth

        initializePsyche();

        Time et = new Time("2026 MAY 04 05:16:09");

        String obsBody = "PSYC";
        String scBaseFrame = "PSYC_SPACECRAFT";
        String referenceFrame = "J2000";
        double[] positionEarth = new double[3];
        double[] lightTime = new double[1];

        try {
            CSPICE.spkpos("EARTH", et.toET(), referenceFrame, "LT+S", obsBody, positionEarth, lightTime);
        } catch (SpiceErrorException e) {
            e.printStackTrace();
        }
        ADCModel ckadcModel = new CKAttitudeModel(scBaseFrame, referenceFrame, new Duration("00:05:00"), false);

        Rotation LGA_MZ = null, LGA_MX = null, LGA_PX = null;
        try {
            LGA_MZ = ckadcModel.getOrientation(et, null, null, null, null).getRotation();
            LGA_MX = ckadcModel.getOrientation(et, null, null, null, null).getRotation();
            LGA_PX = ckadcModel.getOrientation(et, null, null, null, null).getRotation();
        }
        catch (AttitudeNotAvailableException e){
            fail();
        }

        double MZAngle = AttitudeFunctions.angleBetweenObjectAndBoresight(LGA_MZ, getFixedFrameRotationWithSpiceCrashOnError(scBaseFrame, "PSYC_LGA_MZ"), new Vector3D(positionEarth));
        double MXAngle = AttitudeFunctions.angleBetweenObjectAndBoresight(LGA_MX, getFixedFrameRotationWithSpiceCrashOnError(scBaseFrame, "PSYC_LGA_MX"), new Vector3D(positionEarth));
        double PXAngle = AttitudeFunctions.angleBetweenObjectAndBoresight(LGA_PX, getFixedFrameRotationWithSpiceCrashOnError(scBaseFrame, "PSYC_LGA_PX"), new Vector3D(positionEarth));

        String smallestAngle;

        if (MZAngle <= MXAngle && MZAngle <= PXAngle){
            smallestAngle = "PSYC_LGA_MZ";
        } else if (MXAngle <= PXAngle){
            smallestAngle = "PSYC_LGA_PX";
        } else{
            smallestAngle = "PSYC_LGA_MX";
        }

        assertEquals("PSYC_LGA_MZ", smallestAngle);
    }

    private Rotation getFixedFrameRotationWithSpiceCrashOnError(String fromFrame, String toFrame){
        try {
            return AttitudeFunctions.getFixedFrameRotationWithSpice(fromFrame, toFrame);
        } catch (SpiceErrorException e) {
            e.printStackTrace();
        }
        return null;
    }
}