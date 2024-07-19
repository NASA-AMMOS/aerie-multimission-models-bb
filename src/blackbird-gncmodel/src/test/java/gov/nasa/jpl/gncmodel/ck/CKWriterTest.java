package gov.nasa.jpl.gncmodel.ck;

import gov.nasa.jpl.engine.Setup;
import gov.nasa.jpl.gncmodel.functions.AttitudeFunctions;
import gov.nasa.jpl.gncmodel.functions.AttitudeNotAvailableException;
import gov.nasa.jpl.gncmodel.interfaces.Observer;
import gov.nasa.jpl.gncmodel.interfaces.Orientation;
import gov.nasa.jpl.gncmodel.interfaces.Target;
import gov.nasa.jpl.gncmodel.mmgenerator.GenerateNoRateMatchAttitudeModel;
import gov.nasa.jpl.gncmodel.observers.CustomObserver;
import gov.nasa.jpl.gncmodel.observers.SpacecraftInstrumentObserver;
import gov.nasa.jpl.gncmodel.targets.primary.BodyCenterPrimaryTarget;
import gov.nasa.jpl.gncmodel.targets.secondary.BodyPlaneSecondaryTarget;
import gov.nasa.jpl.spice.Spice;
import gov.nasa.jpl.time.Duration;
import gov.nasa.jpl.time.Time;
import junit.framework.TestCase;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.junit.Before;
import spice.basic.SpiceErrorException;

import java.util.*;

public class CKWriterTest extends TestCase {

    @Before
    public void setUp(){
        Setup.initializeEngine();
    }

    private void initializePsycheNoCK(){
        String path = "src/test/resources/gov/nasa/jpl/gncmodel/ck/psyche/";
        String LSK = "naif0012.tls";
        String SCLK = "PSYC_69_SCLKSCET.00000.tsc";
        String FK = "psyche_fk_v04.tf";
        String IK1 = "psyche_struct_v01.ti";
        String IK2 = "psyche_imager_v02.ti";
        String SPK1 = "psyc_260220_260417_R811_i90_res70-9_gravity6_181015.bsp";
        String SPK8 = "de421.bsp";
        String SPK9 = "sb_psyche_ssd_180411.bsp";

        try {
            Spice.loadKernel(path.concat(LSK));
            Spice.loadKernel(path.concat(SCLK));
            Spice.loadKernel(path.concat(FK));
            Spice.loadKernel(path.concat(IK1));
            Spice.loadKernel(path.concat(IK2));
            Spice.loadKernel(path.concat(SPK1));
            Spice.loadKernel(path.concat(SPK8));
            Spice.loadKernel(path.concat(SPK9));
        } catch (SpiceErrorException e) {
            fail();
        }
    }

    public void testCKWriter() {

        // !!!!!!!!!!!!!!!!!!!!!!!!!!
        // This test no longer works after changing pxform to ckgpav, looking into this issue

        // First Science to COMM turn orbit A
        setUp();
        initializePsycheNoCK();

        Time et = new Time("2026-052T06:32:20.494");
        String scBaseFrame = "PSYC_SPACECRAFT";
        String obsBody = "PSYC";
        String relativeFrame = "J2000";
        Observer primaryObserver = new SpacecraftInstrumentObserver("PSYC_IMGA", obsBody);
        Target primaryTarget = new BodyCenterPrimaryTarget("PSYCHE", obsBody, relativeFrame);
        Observer secondaryObserver = new CustomObserver(new Vector3D(0, 1, 0));
        Target secondaryTarget = new BodyPlaneSecondaryTarget("SUN", obsBody, relativeFrame, primaryTarget, 0.0, true);
        Vector3D angularVelocityLimit = new Vector3D(0.00175, 0.00175, 0.0175);
        Vector3D angularAccelerationLimit = new Vector3D(1.92e-5, 1.92e-5, 1.92e-5);
        Duration sampleRate = new Duration("00:01:00");
        Duration turnLength = new Duration("00:40:00");
        GenerateNoRateMatchAttitudeModel generateAttitude = new GenerateNoRateMatchAttitudeModel(angularVelocityLimit, angularAccelerationLimit, Duration.SECOND_DURATION, sampleRate);

        Rotation beginningRotation = generateAttitude.getOrientation(et, primaryObserver, primaryTarget, secondaryObserver, secondaryTarget).getRotation();
        primaryObserver = new SpacecraftInstrumentObserver("PSYC_HGA", obsBody);
        primaryTarget = new BodyCenterPrimaryTarget("EARTH", obsBody, relativeFrame);
        secondaryTarget = new BodyPlaneSecondaryTarget("SUN", obsBody, relativeFrame, primaryTarget, 0.0, true);
        SortedMap<Time, Orientation> orderedOrientations = null;
        try {
            orderedOrientations = generateAttitude.getOrientations(et, new Orientation(beginningRotation), primaryObserver, primaryTarget, secondaryObserver, secondaryTarget, turnLength);
        } catch (AttitudeNotAvailableException e) {
            fail();
        }

        // Write the CK file
        String fileName = "CKWriterTest.bc";
        try {
            AttitudeFunctions.writeCK(orderedOrientations, fileName, -69, -69000, relativeFrame, false);
        } catch (SpiceErrorException e) {
            e.printStackTrace();
        }

        // Load the previously created CK file
        try {
            Spice.loadKernel(fileName);
        } catch (SpiceErrorException e) {
            fail();
        }
        Rotation rotationCK;
        double eps = 1.0E-8;
        CKAttitudeModel ckAttitude = new CKAttitudeModel(scBaseFrame, relativeFrame, sampleRate, false);
        for (Map.Entry<Time, Orientation> entry : orderedOrientations.entrySet()) {
            try {
                rotationCK = ckAttitude.getOrientation(entry.getKey(), null, null, null, null).getRotation();
                assertEquals(entry.getValue().getRotation().getQ0(), rotationCK.getQ0(), eps);
                assertEquals(entry.getValue().getRotation().getQ1(), rotationCK.getQ1(), eps);
                assertEquals(entry.getValue().getRotation().getQ2(), rotationCK.getQ2(), eps);
                assertEquals(entry.getValue().getRotation().getQ3(), rotationCK.getQ3(), eps);
            } catch (AttitudeNotAvailableException e) {
                fail();
            }
        }
    }
}