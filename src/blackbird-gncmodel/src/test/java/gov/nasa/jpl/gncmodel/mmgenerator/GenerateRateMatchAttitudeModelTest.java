package gov.nasa.jpl.gncmodel.mmgenerator;

import gov.nasa.jpl.engine.Setup;
import gov.nasa.jpl.gncmodel.ck.CKAttitudeModel;
import gov.nasa.jpl.gncmodel.functions.AttitudeNotAvailableException;
import gov.nasa.jpl.gncmodel.interfaces.ADCModel;
import gov.nasa.jpl.gncmodel.interfaces.Observer;
import gov.nasa.jpl.gncmodel.interfaces.Orientation;
import gov.nasa.jpl.gncmodel.interfaces.Target;
import gov.nasa.jpl.gncmodel.observers.CustomObserver;
import gov.nasa.jpl.gncmodel.observers.SpacecraftInstrumentObserver;
import gov.nasa.jpl.gncmodel.targets.primary.BodyCenterPrimaryTarget;
import gov.nasa.jpl.gncmodel.targets.secondary.BodyPlaneSecondaryTarget;
import gov.nasa.jpl.spice.Spice;
import gov.nasa.jpl.time.Duration;
import gov.nasa.jpl.time.Time;
import junit.framework.TestCase;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.junit.Before;
import spice.basic.SpiceErrorException;

import java.util.SortedMap;
import java.util.TreeMap;


public class GenerateRateMatchAttitudeModelTest extends TestCase {

    @Before
    public void setUp(){
        Setup.initializeEngine();
    }

    private void initializePsyche(){
        initializePsycheNoCK();

        String path = "src/test/resources/gov/nasa/jpl/gncmodel/ck/psyche/";
        String CK1 = "single_turn.bc";
        String CK2 = "PsycheOrbitA_PSYC_IMGA_wp0.bc";
        String CK3 = "PsycheOrbitB_PSYC_IMGA_wp0_op1.bc";
        String CK4 = "PsycheOrbitC_PSYC_IMGA_wp0_op1.bc";
        String CK5 = "PsycheOrbitD_PSYC_IMGA_wp0_op1.bc";

        try {
            Spice.loadKernel(path.concat(CK1));
            Spice.loadKernel(path.concat(CK2));
            Spice.loadKernel(path.concat(CK3));
            Spice.loadKernel(path.concat(CK4));
            Spice.loadKernel(path.concat(CK5));
        } catch (SpiceErrorException e) {
            e.printStackTrace();
        }
    }

    private void initializePsycheNoCK(){
        String path = "src/test/resources/gov/nasa/jpl/gncmodel/ck/psyche/";
        String LSK = "naif0012.tls";
        String SCLK = "PSYC_69_SCLKSCET.00000.tsc";
        String FK = "psyche_fk_v04.tf";
        String IK1 = "psyche_struct_v01.ti";
        String IK2 = "psyche_imager_v02.ti";
        String SPK1 = "psyc_260220_260417_R811_i90_res70-9_gravity6_181015.bsp";
        String SPK2 = "psyc_260417_260504_AtoB_gravity6_181015.bsp";
        String SPK3 = "psyc_260504_260811_R401_i90_res46-17_gravity6_181015.bsp";
        String SPK4 = "psyc_260811_260903_BtoC_gravity6_181015.bsp";
        String SPK5 = "psyc_260903_261212_R281_i89_res78-49_gravity6_181015.bsp";
        String SPK6 = "psyc_261212_270429_CtoD_gravity6_181015.bsp";
        String SPK7 = "psyc_270429_271031_R192_i160_res161-206_gravity6_181015.bsp";
        String SPK8 = "de421.bsp";
        String SPK9 = "sb_psyche_ssd_180411.bsp";


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
            Spice.loadKernel(path.concat(SPK5));
            Spice.loadKernel(path.concat(SPK6));
            Spice.loadKernel(path.concat(SPK7));
            Spice.loadKernel(path.concat(SPK8));
            Spice.loadKernel(path.concat(SPK9));
        } catch (SpiceErrorException e) {
            e.printStackTrace();
        }
    }

    private void rateMatchingTurnTest(Time et, Observer primaryObserverBeg, Target primaryTargetBeg, Observer secondaryObserverBeg, Target secondaryTargetBeg,
                                      Observer primaryObserverEnd, Target primaryTargetEnd, Observer secondaryObserverEnd, Target secondaryTargetEnd,
                                      double maxAllowableAngle, Duration expectedTurnLength) throws AttitudeNotAvailableException{
        setUp();
        initializePsycheNoCK();

        Duration turnAllocation = new Duration("00:40:00");
        Duration sampleRate = Duration.MINUTE_DURATION;
        double a_max = Math.toRadians(2.5e-4);
        double v_max = Math.toRadians(0.1);
        Vector3D angularVelocityLimit = new Vector3D(v_max, v_max, v_max);
        Vector3D angularAccelerationLimit = new Vector3D(a_max, 5.8*a_max, a_max);
        ADCModel generateAttitude = new GenerateRateMatchAttitudeModel(angularVelocityLimit, angularAccelerationLimit, Duration.SECOND_DURATION, sampleRate);

        // Find the orientation at the beginning of the turn
        Orientation initialOrientation = generateAttitude.getOrientation(et, primaryObserverBeg, primaryTargetBeg, secondaryObserverBeg, secondaryTargetBeg);

        // Generate the series of orientations that describe the desired turn
        SortedMap<Time, Orientation> orderedOrientationsNoCK = generateAttitude.getOrientations(et, initialOrientation, primaryObserverEnd, primaryTargetEnd, secondaryObserverEnd, secondaryTargetEnd, turnAllocation);

        // Use Steve's CKs to find the series of orientations that describe the desired turn
        initializePsyche();
        String scBaseFrame = "PSYC_SPACECRAFT";
        ADCModel ckadcModel = new CKAttitudeModel(scBaseFrame, "J2000", sampleRate, false);
        SortedMap<Time, Orientation> orderedOrientationsCK = ckadcModel.getOrientations(et, null, null, null, null, null, turnAllocation);

        // Compare the two series of orientations
        Time currentTime = et;
        Vector3D xAxis = new Vector3D(1,0,0);
        Vector3D yAxis = new Vector3D(0,1,0);
        Vector3D zAxis = new Vector3D(0,0,1);
        double maxAngleDifference = 0.0;

        Duration turnLength = orderedOrientationsNoCK.lastKey().subtract(orderedOrientationsNoCK.firstKey());
        while(currentTime.lessThanOrEqualTo(et.add(turnLength))){
            Vector3D xAxisCK = orderedOrientationsCK.get(currentTime).getRotation().applyTo(xAxis);
            Vector3D xAxisNoCK = orderedOrientationsNoCK.get(currentTime).getRotation().applyTo(xAxis);
            double xAxisAngle = Math.toDegrees(Math.acos(Vector3D.dotProduct(xAxisCK, xAxisNoCK)));

            Vector3D yAxisCK = orderedOrientationsCK.get(currentTime).getRotation().applyTo(yAxis);
            Vector3D yAxisNoCK = orderedOrientationsNoCK.get(currentTime).getRotation().applyTo(yAxis);
            double yAxisAngle = Math.toDegrees(Math.acos(Vector3D.dotProduct(yAxisCK, yAxisNoCK)));

            Vector3D zAxisCK = orderedOrientationsCK.get(currentTime).getRotation().applyTo(zAxis);
            Vector3D zAxisNoCK = orderedOrientationsNoCK.get(currentTime).getRotation().applyTo(zAxis);
            double zAxisAngle = Math.toDegrees(Math.acos(Vector3D.dotProduct(zAxisCK, zAxisNoCK)));

            if(xAxisAngle > maxAngleDifference){
                maxAngleDifference = xAxisAngle;
            }
            if(yAxisAngle > maxAngleDifference){
                maxAngleDifference = yAxisAngle;
            }
            if(zAxisAngle > maxAngleDifference){
                maxAngleDifference = zAxisAngle;
            }
            currentTime = currentTime.add(sampleRate);
        }

        assertTrue(maxAngleDifference <= maxAllowableAngle);

        // false for truncateMapWhenTurnPhaseFinishes means that the map should go until start + override
        assertEquals(turnLength, turnAllocation);

        // true for truncateMapWhenTurnPhaseFinishes means that the map should stop when the turn does
        ADCModel generateAttitudeTruncated = new GenerateRateMatchAttitudeModel(angularVelocityLimit, angularAccelerationLimit, Duration.SECOND_DURATION, sampleRate, true, true);
        SortedMap<Time, Orientation> orderedOrientationsTruncated = generateAttitudeTruncated.getOrientations(et, initialOrientation, primaryObserverEnd, primaryTargetEnd, secondaryObserverEnd, secondaryTargetEnd, turnAllocation);

        assertEquals(expectedTurnLength, orderedOrientationsTruncated.lastKey().subtract(orderedOrientationsTruncated.firstKey()));

        // test to make sure if limit is a lot lower, we get error that we expect
        Vector3D angularVelocityLimitSlow = new Vector3D(v_max/1000, v_max/1000, v_max/1000);
        Vector3D angularAccelerationLimitSlow = new Vector3D(a_max/1000, 5.8*a_max/1000, a_max/1000);
        ADCModel generateAttitudeSlowTurn = new GenerateRateMatchAttitudeModel(angularVelocityLimitSlow, angularAccelerationLimitSlow, Duration.SECOND_DURATION, sampleRate);
        ADCModel generateAttitudeSlowTurnNoMessage = new GenerateRateMatchAttitudeModel(angularVelocityLimitSlow, angularAccelerationLimitSlow, Duration.SECOND_DURATION, sampleRate, false, true);

        try {
            TreeMap<Time, Orientation> orderedOrientationsNoCKSlowTurn = (TreeMap<Time, Orientation>) generateAttitudeSlowTurn.getOrientations(et, initialOrientation, primaryObserverEnd, primaryTargetEnd, secondaryObserverEnd, secondaryTargetEnd, turnLength);
            fail();
        }
        catch(AttitudeNotAvailableException e){
            assertTrue(e.getMessage().contains("longer than the allocated"));
        }

        try {
            // expect to just get back, without errors, just the first part of the turn
            TreeMap<Time, Orientation> orderedOrientationsNoCKSlowTurn = (TreeMap<Time, Orientation>) generateAttitudeSlowTurnNoMessage.getOrientations(et, initialOrientation, primaryObserverEnd, primaryTargetEnd, secondaryObserverEnd, secondaryTargetEnd, turnLength);
        }
        catch(AttitudeNotAvailableException e){
            fail();
        }
    }

    public void testRateMatchingNonInertialTarget() throws AttitudeNotAvailableException {
        // COMM to Science turn orbit A

        Time et = new Time("2026 FEB 23 06:21:10");
        String obsBody = "PSYC";
        String relativeFrame = "J2000";
        double maxAllowableAngle = 0.2; //deg

        Observer primaryObserverBeg = new SpacecraftInstrumentObserver("PSYC_HGA", obsBody);
        Target primaryTargetBeg = new BodyCenterPrimaryTarget("EARTH", obsBody, relativeFrame);
        Observer secondaryObserverBeg = new CustomObserver(new Vector3D(0,1,0));
        Target secondaryTargetBeg = new BodyPlaneSecondaryTarget("SUN", obsBody, relativeFrame, primaryTargetBeg, 0.0, true);

        Observer primaryObserverEnd = new SpacecraftInstrumentObserver("PSYC_IMGA", obsBody);
        Target primaryTargetEnd = new BodyCenterPrimaryTarget("PSYCHE", obsBody, relativeFrame);
        Observer secondaryObserverEnd =  new CustomObserver(new Vector3D(0,1,0));
        Target secondaryTargetEnd = new BodyPlaneSecondaryTarget("SUN", obsBody, relativeFrame, primaryTargetEnd, 0.0, true);

        rateMatchingTurnTest(et, primaryObserverBeg, primaryTargetBeg, secondaryObserverBeg, secondaryTargetBeg,
                primaryObserverEnd, primaryTargetEnd, secondaryObserverEnd, secondaryTargetEnd, maxAllowableAngle, new Duration("00:32:00"));

        }

    public void testRateMatchingInertialTarget() throws AttitudeNotAvailableException {
        // Science to COMM turn orbit A

        Time et = new Time("2026 FEB 22 14:29:11");
        String obsBody = "PSYC";
        String relativeFrame = "J2000";
        double maxAllowableAngle = 0.2; // deg

        Observer primaryObserverBeg = new SpacecraftInstrumentObserver("PSYC_IMGA", obsBody);
        Target primaryTargetBeg = new BodyCenterPrimaryTarget("PSYCHE", obsBody, relativeFrame);
        Observer secondaryObserverBeg = new CustomObserver(new Vector3D(0,1,0));
        Target secondaryTargetBeg = new BodyPlaneSecondaryTarget("SUN", obsBody, relativeFrame, primaryTargetBeg, 0.0, true);

        Observer primaryObserverEnd = new SpacecraftInstrumentObserver("PSYC_HGA", obsBody);
        Target primaryTargetEnd = new BodyCenterPrimaryTarget("EARTH", obsBody, relativeFrame);
        Observer secondaryObserverEnd =  new CustomObserver(new Vector3D(0,1,0));
        Target secondaryTargetEnd = new BodyPlaneSecondaryTarget("SUN", obsBody, relativeFrame, primaryTargetEnd, 0.0, true);

        rateMatchingTurnTest(et, primaryObserverBeg, primaryTargetBeg, secondaryObserverBeg, secondaryTargetBeg,
                primaryObserverEnd, primaryTargetEnd, secondaryObserverEnd, secondaryTargetEnd, maxAllowableAngle, new Duration("00:34:00"));

    }

}