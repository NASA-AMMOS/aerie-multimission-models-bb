package gov.nasa.jpl.gncmodel.mmgenerator;

import gov.nasa.jpl.engine.Setup;
import gov.nasa.jpl.gncmodel.ck.CKAttitudeModel;
import gov.nasa.jpl.gncmodel.functions.AttitudeFunctions;
import gov.nasa.jpl.gncmodel.functions.AttitudeNotAvailableException;
import gov.nasa.jpl.gncmodel.interfaces.ADCModel;
import gov.nasa.jpl.gncmodel.interfaces.Observer;
import gov.nasa.jpl.gncmodel.interfaces.Target;
import gov.nasa.jpl.gncmodel.observers.CustomObserver;
import gov.nasa.jpl.gncmodel.observers.SpacecraftInstrumentObserver;
import gov.nasa.jpl.gncmodel.targets.primary.AheadCrossNadirPrimaryTarget;
import gov.nasa.jpl.gncmodel.targets.primary.BodyCenterPrimaryTarget;
import gov.nasa.jpl.gncmodel.targets.secondary.BodyCenterSecondaryTarget;
import gov.nasa.jpl.gncmodel.targets.secondary.BodyPlaneSecondaryTarget;
import gov.nasa.jpl.gncmodel.targets.secondary.CustomSecondaryTarget;
import gov.nasa.jpl.gncmodel.targets.secondary.OrbitPlaneSecondaryTarget;
import gov.nasa.jpl.spice.Spice;
import gov.nasa.jpl.time.Duration;
import gov.nasa.jpl.time.Time;
import junit.framework.TestCase;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.junit.Before;
import spice.basic.CSPICE;
import spice.basic.SpiceErrorException;
import java.util.Objects;
import static gov.nasa.jpl.gncmodel.functions.AttitudeFunctions.*;


public class GenerateAttitudeModelTest extends TestCase {

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

    public void testGetOrientationWithoutCK() throws AttitudeNotAvailableException {

        setUp();
        initializePsycheNoCK();

        // Look at the beginning of Orbit A (we know this is a COMM period)
        // and get the orientation without a C-Kernel
        Time et = new Time("2026 APR 6 00:00:00");
        String scBaseFrame = "PSYC_SPACECRAFT";
        String obsBody = "PSYC";
        String relativeFrame = "J2000";
        Observer primaryObserver = new SpacecraftInstrumentObserver("PSYC_HGA", obsBody);
        Target primaryTarget = new BodyCenterPrimaryTarget("EARTH", obsBody, relativeFrame);
        Observer secondaryObserver = new CustomObserver(new Vector3D(0,1,0));
        Target secondaryTarget = new BodyPlaneSecondaryTarget("SUN", obsBody, relativeFrame, primaryTarget, 0.0, true);

        ADCModel generateAttitude = new GenerateNoRateMatchAttitudeModel(null, null, Duration.SECOND_DURATION, new Duration("00:01:00"));
        Rotation spacecraftToJ2000NoCK = generateAttitude.getOrientation(et, primaryObserver, primaryTarget, secondaryObserver, secondaryTarget).getRotation();

        // Now look at the same time but using the C-Kernel and see
        // if both results are the same
        initializePsyche();

        ADCModel ckAttitude = new CKAttitudeModel(scBaseFrame, relativeFrame, new Duration("00:01:00"), false);
        Rotation spacecraftToJ2000WithCK = null;
        try {
            spacecraftToJ2000WithCK = ckAttitude.getOrientation(et, null, null, null, null).getRotation();
        } catch (AttitudeNotAvailableException e) {
            e.printStackTrace();
        }

        assertEquals(spacecraftToJ2000NoCK.getQ0(), Objects.requireNonNull(spacecraftToJ2000WithCK).getQ0(),1E-4);
        assertEquals(spacecraftToJ2000NoCK.getQ1(),spacecraftToJ2000WithCK.getQ1(),1E-4);
        assertEquals(spacecraftToJ2000NoCK.getQ2(),spacecraftToJ2000WithCK.getQ2(),1E-4);
        assertEquals(spacecraftToJ2000NoCK.getQ3(),spacecraftToJ2000WithCK.getQ3(),1E-4);

        // Test offset secondary pointing
        double offset = 7.0;
        Vector3D hgaBoresightSCFrame = new Vector3D(Math.sin(Math.toRadians(10.0)),0.0,Math.cos(Math.toRadians(10.0))).normalize();
        Vector3D scYAxis = new Vector3D(0.0,1.0,0.0);
        secondaryTarget = new BodyPlaneSecondaryTarget("SUN", obsBody, relativeFrame, primaryTarget, offset, true);
        Rotation offsetRotationToJ2000Exp = generateAttitude.getOrientation(et, primaryObserver, primaryTarget, secondaryObserver, secondaryTarget).getRotation();
        assertEquals(Math.toDegrees(Math.acos(Vector3D.dotProduct(spacecraftToJ2000NoCK.applyTo(scYAxis), offsetRotationToJ2000Exp.applyTo(scYAxis)))), Math.abs(offset),1E-8);
        assertEquals(Math.toDegrees(Math.acos(Math.round(Vector3D.dotProduct(primaryTarget.getPosition(et).normalize(), offsetRotationToJ2000Exp.applyTo(hgaBoresightSCFrame).normalize())))), 0.0,1E-8);

        // Look two days after the beginning of Orbit A (we know this
        // is a science period) and get the orientation with a C-Kernel
        et = new Time("2026 MAY 4 19:22:00");
        primaryObserver = new SpacecraftInstrumentObserver("PSYC_IMGA", obsBody);
        primaryTarget = new BodyCenterPrimaryTarget("PSYCHE", obsBody, relativeFrame);
        secondaryTarget = new BodyPlaneSecondaryTarget("SUN", obsBody, relativeFrame, primaryTarget, 0.0, true);

        try {
            spacecraftToJ2000WithCK = ckAttitude.getOrientation(et, null, null, null, null).getRotation();
        } catch (AttitudeNotAvailableException e) {
            e.printStackTrace();
        }

        // Unload C-Kernel so orientation will be generated without it
        try {
            Spice.unLoadKernel("src/test/resources/gov/nasa/jpl/gncmodel/ck/psyche/PsycheOrbitA_negX_Psyche_PS_with_dark_PS_COMM_rcsDesat.bc");
        } catch (SpiceErrorException e) {
            e.printStackTrace();
        }

        // Get orientation without C-Kernel
        spacecraftToJ2000NoCK = generateAttitude.getOrientation(et,primaryObserver,primaryTarget,secondaryObserver,secondaryTarget).getRotation();

        assertEquals(spacecraftToJ2000NoCK.getQ0(),spacecraftToJ2000WithCK.getQ0(),1E-4);
        assertEquals(spacecraftToJ2000NoCK.getQ1(),spacecraftToJ2000WithCK.getQ1(),1E-4);
        assertEquals(spacecraftToJ2000NoCK.getQ2(),spacecraftToJ2000WithCK.getQ2(),1E-4);
        assertEquals(spacecraftToJ2000NoCK.getQ3(),spacecraftToJ2000WithCK.getQ3(),1E-4);
    }

    public void testArraysInPlaneOrientation(){

        // This test confirms that secondary pointing
        // satisfies gravity gradient concerns (arrays in
        // orbit plane) is met when power steering is not
        // secondary target
        setUp();

        Time et = new Time("2026 JUN 2 12:31:00");
        String obsBody = "PSYC";
        String centerBody = "PSYCHE";
        String scBaseFrame = "PSYC_SPACECRAFT";
        String relativeFrame = "J2000";
        Observer primaryObserver = new SpacecraftInstrumentObserver("PSYC_HGA",obsBody);
        Target primaryTarget = new BodyCenterPrimaryTarget("EARTH", obsBody, relativeFrame);
        Observer secondaryObserver = new CustomObserver(new Vector3D(0,1,0));
        Target secondaryTarget = new OrbitPlaneSecondaryTarget(centerBody, obsBody, relativeFrame);

        initializePsycheNoCK();

        // Get orientation by generating it ourselves
        GenerateNoRateMatchAttitudeModel generateAttitude = new GenerateNoRateMatchAttitudeModel(null, null, Duration.SECOND_DURATION, new Duration("00:01:00"));
        Rotation spacecraftToJ2000NoCK = generateAttitude.getOrientation(et, primaryObserver, primaryTarget, secondaryObserver, secondaryTarget).getRotation();

        // Get orientation using CK data
        initializePsyche();

        CKAttitudeModel ckAttitude = new CKAttitudeModel(scBaseFrame, relativeFrame, new Duration("00:01:00"), false);
        Rotation spacecraftToJ2000WithCK = null;
        try {
            spacecraftToJ2000WithCK = ckAttitude.getOrientation(et, null, null, null, null).getRotation();
        } catch (AttitudeNotAvailableException e) {
            e.printStackTrace();
        }

        assertEquals(spacecraftToJ2000NoCK.getQ0(), Objects.requireNonNull(spacecraftToJ2000WithCK).getQ0(),1E-4);
        assertEquals(spacecraftToJ2000NoCK.getQ1(),spacecraftToJ2000WithCK.getQ1(),1E-4);
        assertEquals(spacecraftToJ2000NoCK.getQ2(),spacecraftToJ2000WithCK.getQ2(),1E-4);
        assertEquals(spacecraftToJ2000NoCK.getQ3(),spacecraftToJ2000WithCK.getQ3(),1E-4);
    }

    public void testOffNadirOrientation(){

        // This test confirms that offset angles from
        // the primary target work
        setUp();

        // Time in Orbit C in a mapping cycle with off nadir
        // pointing (check Psyche mission plan)
        Time et = new Time("2026 SEP 21 21:31:00");
        String obsBody = "PSYC";
        String scBaseFrame = "PSYC_SPACECRAFT";
        String centerBody = "PSYCHE";
        String relativeFrame = "J2000";
        Observer primaryObserver = new SpacecraftInstrumentObserver("PSYC_IMGA", obsBody);
        Target primaryTarget = new AheadCrossNadirPrimaryTarget(centerBody, obsBody, relativeFrame, 12.0, 10.0);
        Observer secondaryObserver = new CustomObserver(new Vector3D(0,1,0));
        Target secondaryTarget = new BodyPlaneSecondaryTarget("SUN", obsBody, relativeFrame, primaryTarget, 0.0, true);

        initializePsycheNoCK();

        // Get orientation by generating it ourselves
        GenerateNoRateMatchAttitudeModel generateAttitude = new GenerateNoRateMatchAttitudeModel(null, null, Duration.SECOND_DURATION, new Duration("00:01:00"));
        Rotation spacecraftToJ2000NoCK = generateAttitude.getOrientation(et, primaryObserver, primaryTarget, secondaryObserver, secondaryTarget).getRotation();

        // Get orientation using CK data
        initializePsyche();

        CKAttitudeModel ckAttitude = new CKAttitudeModel(scBaseFrame, relativeFrame, new Duration("00:01:00"), false);
        Rotation spacecraftToJ2000WithCK = null;
        try {
            spacecraftToJ2000WithCK = ckAttitude.getOrientation(et, primaryObserver, primaryTarget, secondaryObserver, secondaryTarget).getRotation();
        } catch (AttitudeNotAvailableException e) {
            e.printStackTrace();
        }

        assertEquals(spacecraftToJ2000NoCK.getQ0(), Objects.requireNonNull(spacecraftToJ2000WithCK).getQ0(),1E-4);
        assertEquals(spacecraftToJ2000NoCK.getQ1(),spacecraftToJ2000WithCK.getQ1(),1E-4);
        assertEquals(spacecraftToJ2000NoCK.getQ2(),spacecraftToJ2000WithCK.getQ2(),1E-4);
        assertEquals(spacecraftToJ2000NoCK.getQ3(),spacecraftToJ2000WithCK.getQ3(),1E-4);
    }

    public void testOutOfXZPlanePrimaryPointing(){
        // Want to show that if the primary target is not in
        // the XZ-plane the secondary constraint is still met
        // to within some tolerance

        setUp();

        Time et = new Time("2026 SEP 15 21:45:00");
        String obsBody = "PSYC";
        String scBaseFrame = "PSYC_SPACECRAFT";
        String centerBody = "PSYCHE";
        String relativeFrame = "J2000";
        Observer primaryObserver = new SpacecraftInstrumentObserver("PSYC_IMGB", obsBody);
        Target primaryTarget = new BodyCenterPrimaryTarget(centerBody, obsBody, relativeFrame);
        Observer secondaryObserver = new CustomObserver(new Vector3D(0,1,0));
        Target secondaryTarget = new BodyPlaneSecondaryTarget("SUN", obsBody, relativeFrame, primaryTarget, 0.0, false);

        initializePsycheNoCK();

        // Get orientation by generating it ourselves
        GenerateNoRateMatchAttitudeModel generateAttitude = new GenerateNoRateMatchAttitudeModel(null, null, Duration.SECOND_DURATION, new Duration("00:01:00"));
        Rotation spacecraftToJ2000NoCK = generateAttitude.getOrientation(et, primaryObserver, primaryTarget, secondaryObserver, secondaryTarget).getRotation();

        // First show that we are pointing at 16 Psyche with IMGB

        double[] targetRelative = new double[3];
        double[] lightTimeDelay = new double[1];
        try {
            CSPICE.spkpos(centerBody, et.toET(), relativeFrame,"LT+S", obsBody, targetRelative, lightTimeDelay);
        } catch (SpiceErrorException er) {
            er.printStackTrace();
        }
        double angleBetweenIMGBAndNadir = AttitudeFunctions.angleBetweenObjectAndBoresight(spacecraftToJ2000NoCK, getFixedFrameRotationWithSpiceCrashOnError("PSYC_IMGB", scBaseFrame), new Vector3D(targetRelative).normalize());

        // Now make sure that the Solar incidence angle is bounded
        try {
            CSPICE.spkpos("SUN", et.toET(), relativeFrame,"LT+S", obsBody, targetRelative, lightTimeDelay);
        } catch (SpiceErrorException er) {
            er.printStackTrace();
        }
        double solarIncidenceAngle = Math.abs(angleBetweenObjectAndVector(spacecraftToJ2000NoCK, Rotation.IDENTITY, new Vector3D(targetRelative), new Vector3D(0,1,0))-90.0);

        assertTrue(solarIncidenceAngle < 1);
        assertEquals(0, angleBetweenIMGBAndNadir, 1E-13);
    }

    public void testGenericBodySecondaryPointing(){
        // Here we are testing secondary directional targeting rather than
        // plane targeting. We will use IMGA and IMB as our primary
        // observer pointing at nadir (not comparing to CK where there is
        // some offset at this ET). All three LGAs (MZ, MX, PX) will be
        // used as secondary observers trying to target the Earth. The
        // resultant angles will be examined to make sure they are what
        // they should be
        setUp();
        initializePsycheNoCK();

        Time et = new Time("2026 SEP 15 21:45:00");
        String obsBody = "PSYC";
        String scBaseFrame = "PSYC_SPACECRAFT";
        String centerBody = "PSYCHE";
        String relativeFrame = "J2000";

        // IMGB as primary observer and LGA_PX as secondary observer
        Observer primaryObserver = new SpacecraftInstrumentObserver("PSYC_IMGB", obsBody);
        Target primaryTarget = new BodyCenterPrimaryTarget(centerBody, obsBody, relativeFrame);
        Observer secondaryObserver = new SpacecraftInstrumentObserver("PSYC_LGA_PX", obsBody);
        Target secondaryTarget = new BodyCenterSecondaryTarget("EARTH", obsBody, relativeFrame, primaryTarget, 0.0);

        GenerateNoRateMatchAttitudeModel generateAttitude = new GenerateNoRateMatchAttitudeModel(null, null, Duration.SECOND_DURATION, new Duration("00:01:00"));
        Rotation spacecraftToJ2000NoCK = generateAttitude.getOrientation(et, primaryObserver, primaryTarget, secondaryObserver, secondaryTarget).getRotation();

        double[] psyche16Pos = new double[3];
        double[] lightTimeDelay = new double[1];
        try {
            CSPICE.spkpos(centerBody, et.toET(), relativeFrame,"LT+S", obsBody, psyche16Pos, lightTimeDelay);
        } catch (SpiceErrorException er) {
            er.printStackTrace();
        }
        double angleBetweenIMGBAndNadir = AttitudeFunctions.angleBetweenObjectAndBoresight(spacecraftToJ2000NoCK, getFixedFrameRotationWithSpiceCrashOnError("PSYC_IMGB", scBaseFrame), new Vector3D(psyche16Pos).normalize());

        double[] earthPos = new double[3];
        try {
            CSPICE.spkpos("EARTH", et.toET(), relativeFrame,"LT+S", obsBody, earthPos, lightTimeDelay);
        } catch (SpiceErrorException er) {
            er.printStackTrace();
        }
        double earthLGAAngle = Math.abs(angleBetweenObjectAndBoresight(spacecraftToJ2000NoCK, getFixedFrameRotationWithSpiceCrashOnError("PSYC_LGA_PX", scBaseFrame), new Vector3D(earthPos)));
        double nadirLGAAngle = Math.abs(angleBetweenObjectAndBoresight(spacecraftToJ2000NoCK, getFixedFrameRotationWithSpiceCrashOnError("PSYC_LGA_PX", scBaseFrame), new Vector3D(psyche16Pos)));
        double earthNadirAngle = Math.toDegrees(Vector3D.angle(new Vector3D(psyche16Pos), new Vector3D(earthPos)));

        assertEquals(0, angleBetweenIMGBAndNadir, 1E-13);
        assertEquals(nadirLGAAngle-earthNadirAngle, earthLGAAngle,1E-13);


        // IMGB as primary observer and LGA_MX as secondary observer
        secondaryObserver = new SpacecraftInstrumentObserver("PSYC_LGA_MX", obsBody);
        spacecraftToJ2000NoCK = generateAttitude.getOrientation(et, primaryObserver, primaryTarget, secondaryObserver, secondaryTarget).getRotation();

        try {
            CSPICE.spkpos(centerBody, et.toET(), relativeFrame,"LT+S", obsBody, psyche16Pos, lightTimeDelay);
        } catch (SpiceErrorException er) {
            er.printStackTrace();
        }
        angleBetweenIMGBAndNadir = AttitudeFunctions.angleBetweenObjectAndBoresight(spacecraftToJ2000NoCK, getFixedFrameRotationWithSpiceCrashOnError("PSYC_IMGB", scBaseFrame), new Vector3D(psyche16Pos).normalize());

        try {
            CSPICE.spkpos("EARTH", et.toET(), relativeFrame,"LT+S", obsBody, earthPos, lightTimeDelay);
        } catch (SpiceErrorException er) {
            er.printStackTrace();
        }
        earthLGAAngle = Math.abs(angleBetweenObjectAndBoresight(spacecraftToJ2000NoCK, getFixedFrameRotationWithSpiceCrashOnError("PSYC_LGA_MX", scBaseFrame), new Vector3D(earthPos)));
        nadirLGAAngle = Math.abs(angleBetweenObjectAndBoresight(spacecraftToJ2000NoCK, getFixedFrameRotationWithSpiceCrashOnError("PSYC_LGA_MX", scBaseFrame), new Vector3D(psyche16Pos)));
        earthNadirAngle = Math.toDegrees(Vector3D.angle(new Vector3D(psyche16Pos), new Vector3D(earthPos)));

        assertEquals(0, angleBetweenIMGBAndNadir, 1E-13);
        assertEquals(earthNadirAngle-nadirLGAAngle, earthLGAAngle,1E-12);


        // IMGB as primary observer and LGA_MZ as secondary observer
        secondaryObserver = new SpacecraftInstrumentObserver("PSYC_LGA_MZ", obsBody);
        spacecraftToJ2000NoCK = generateAttitude.getOrientation(et, primaryObserver, primaryTarget, secondaryObserver, secondaryTarget).getRotation();

        try {
            CSPICE.spkpos(centerBody, et.toET(), relativeFrame,"LT+S", obsBody, psyche16Pos, lightTimeDelay);
        } catch (SpiceErrorException er) {
            er.printStackTrace();
        }
        angleBetweenIMGBAndNadir = AttitudeFunctions.angleBetweenObjectAndBoresight(spacecraftToJ2000NoCK, getFixedFrameRotationWithSpiceCrashOnError("PSYC_IMGB", scBaseFrame), new Vector3D(psyche16Pos).normalize());

        try {
            CSPICE.spkpos("EARTH", et.toET(), relativeFrame,"LT+S", obsBody, earthPos, lightTimeDelay);
        } catch (SpiceErrorException er) {
            er.printStackTrace();
        }
        earthLGAAngle = Math.abs(angleBetweenObjectAndBoresight(spacecraftToJ2000NoCK, getFixedFrameRotationWithSpiceCrashOnError("PSYC_LGA_MZ", scBaseFrame), new Vector3D(earthPos)));
        nadirLGAAngle = Math.abs(angleBetweenObjectAndBoresight(spacecraftToJ2000NoCK, getFixedFrameRotationWithSpiceCrashOnError("PSYC_LGA_MZ", scBaseFrame), new Vector3D(psyche16Pos)));
        earthNadirAngle = Math.toDegrees(Vector3D.angle(new Vector3D(psyche16Pos), new Vector3D(earthPos)));

        assertEquals(0, angleBetweenIMGBAndNadir, 1E-13);
        assertEquals(earthNadirAngle-nadirLGAAngle, earthLGAAngle,1E-12);

        // Lastly, take this last example and test the offset pointing capability
        // with observers that are not orthogonal to each other
        double offset = 5.0;
        Vector3D imgbBoresightSCFrame = new Vector3D( -0.9979, -0.0645, 0.0);
        Vector3D scZAxis = new Vector3D(0.0,0.0,1.0);
        secondaryTarget = new BodyCenterSecondaryTarget("EARTH", obsBody, relativeFrame, primaryTarget, offset);
        Rotation offsetRotationToJ2000Exp = generateAttitude.getOrientation(et, primaryObserver, primaryTarget, secondaryObserver, secondaryTarget).getRotation();

        // The orientation that uses the offset is the nominal orientation
        // rotated by the offset value about the primary target and the
        // primary observer is aligned with the primary target
        assertEquals(Math.toDegrees(Math.acos(Vector3D.dotProduct(spacecraftToJ2000NoCK.applyTo(scZAxis), offsetRotationToJ2000Exp.applyTo(scZAxis)))), Math.abs(offset),1E-8);
        assertEquals(Math.toDegrees(Math.acos(Math.round(Vector3D.dotProduct(primaryTarget.getPosition(et).normalize(), offsetRotationToJ2000Exp.applyTo(imgbBoresightSCFrame).normalize())))), 0.0,1E-8);


        // IMGA as secondary observer and LGA_PX as secondary target
        secondaryObserver = new SpacecraftInstrumentObserver("PSYC_LGA_PX", obsBody);
        primaryObserver = new SpacecraftInstrumentObserver("PSYC_IMGA", obsBody);
        secondaryTarget = new BodyCenterSecondaryTarget("EARTH", obsBody, relativeFrame, primaryTarget, 0.0);
        spacecraftToJ2000NoCK = generateAttitude.getOrientation(et, primaryObserver, primaryTarget, secondaryObserver, secondaryTarget).getRotation();

        try {
            CSPICE.spkpos(centerBody, et.toET(), relativeFrame,"LT+S", obsBody, psyche16Pos, lightTimeDelay);
        } catch (SpiceErrorException er) {
            er.printStackTrace();
        }
        double angleBetweenIMGAAndNadir = AttitudeFunctions.angleBetweenObjectAndBoresight(spacecraftToJ2000NoCK, getFixedFrameRotationWithSpiceCrashOnError("PSYC_IMGA", scBaseFrame), new Vector3D(psyche16Pos).normalize());

        try {
            CSPICE.spkpos("EARTH", et.toET(), relativeFrame,"LT+S", obsBody, earthPos, lightTimeDelay);
        } catch (SpiceErrorException er) {
            er.printStackTrace();
        }
        earthLGAAngle = Math.abs(angleBetweenObjectAndBoresight(spacecraftToJ2000NoCK, getFixedFrameRotationWithSpiceCrashOnError("PSYC_LGA_PX", scBaseFrame), new Vector3D(earthPos)));
        nadirLGAAngle = Math.abs(angleBetweenObjectAndBoresight(spacecraftToJ2000NoCK, getFixedFrameRotationWithSpiceCrashOnError("PSYC_LGA_PX", scBaseFrame), new Vector3D(psyche16Pos)));
        earthNadirAngle = Math.toDegrees(Vector3D.angle(new Vector3D(psyche16Pos), new Vector3D(earthPos)));

        assertEquals(0, angleBetweenIMGAAndNadir, 1E-13);
        assertEquals(nadirLGAAngle-earthNadirAngle, earthLGAAngle,1E-12);


        // IMGA as primary observer and LGA_MX as secondary observer
        secondaryObserver = new SpacecraftInstrumentObserver("PSYC_LGA_MX", obsBody);
        spacecraftToJ2000NoCK = generateAttitude.getOrientation(et, primaryObserver, primaryTarget, secondaryObserver, secondaryTarget).getRotation();

        try {
            CSPICE.spkpos(centerBody, et.toET(), relativeFrame,"LT+S", obsBody, psyche16Pos, lightTimeDelay);
        } catch (SpiceErrorException er) {
            er.printStackTrace();
        }
        angleBetweenIMGAAndNadir = AttitudeFunctions.angleBetweenObjectAndBoresight(spacecraftToJ2000NoCK, getFixedFrameRotationWithSpiceCrashOnError("PSYC_IMGA", scBaseFrame), new Vector3D(psyche16Pos).normalize());

        try {
            CSPICE.spkpos("EARTH", et.toET(), relativeFrame,"LT+S", obsBody, earthPos, lightTimeDelay);
        } catch (SpiceErrorException er) {
            er.printStackTrace();
        }
        earthLGAAngle = Math.abs(angleBetweenObjectAndBoresight(spacecraftToJ2000NoCK, getFixedFrameRotationWithSpiceCrashOnError("PSYC_LGA_MX", scBaseFrame), new Vector3D(earthPos)));
        nadirLGAAngle = Math.abs(angleBetweenObjectAndBoresight(spacecraftToJ2000NoCK, getFixedFrameRotationWithSpiceCrashOnError("PSYC_LGA_MX", scBaseFrame), new Vector3D(psyche16Pos)));
        earthNadirAngle = Math.toDegrees(Vector3D.angle(new Vector3D(psyche16Pos), new Vector3D(earthPos)));

        assertEquals(0, angleBetweenIMGAAndNadir, 1E-13);
        assertEquals(earthNadirAngle-nadirLGAAngle, earthLGAAngle,1E-12);


        // IMGA as primary observer and LGA_MZ as secondary observer
        secondaryObserver = new SpacecraftInstrumentObserver("PSYC_LGA_MZ", obsBody);
        spacecraftToJ2000NoCK = generateAttitude.getOrientation(et, primaryObserver, primaryTarget, secondaryObserver, secondaryTarget).getRotation();

        try {
            CSPICE.spkpos(centerBody, et.toET(), relativeFrame,"LT+S", obsBody, psyche16Pos, lightTimeDelay);
        } catch (SpiceErrorException er) {
            er.printStackTrace();
        }
        angleBetweenIMGAAndNadir = AttitudeFunctions.angleBetweenObjectAndBoresight(spacecraftToJ2000NoCK, getFixedFrameRotationWithSpiceCrashOnError("PSYC_IMGA", scBaseFrame), new Vector3D(psyche16Pos).normalize());

        try {
            CSPICE.spkpos("EARTH", et.toET(), relativeFrame,"LT+S", obsBody, earthPos, lightTimeDelay);
        } catch (SpiceErrorException er) {
            er.printStackTrace();
        }
        earthLGAAngle = Math.abs(angleBetweenObjectAndBoresight(spacecraftToJ2000NoCK, getFixedFrameRotationWithSpiceCrashOnError("PSYC_LGA_MZ", scBaseFrame), new Vector3D(earthPos)));
        nadirLGAAngle = Math.abs(angleBetweenObjectAndBoresight(spacecraftToJ2000NoCK, getFixedFrameRotationWithSpiceCrashOnError("PSYC_LGA_MZ", scBaseFrame), new Vector3D(psyche16Pos)));
        earthNadirAngle = Math.toDegrees(Vector3D.angle(new Vector3D(psyche16Pos), new Vector3D(earthPos)));

        assertEquals(0, angleBetweenIMGAAndNadir, 1E-13);
        assertEquals(earthNadirAngle-nadirLGAAngle, earthLGAAngle,1E-12);

        // Lastly, take this last example and test the offset pointing capability
        // with observers that are orthogonal to each other
        Vector3D imgaBoresightSCFrame = new Vector3D(-1.0,0.0,0.0);
        Vector3D scYAxis = new Vector3D(0.0,1.0,0.0);
        secondaryTarget = new BodyCenterSecondaryTarget("EARTH", obsBody, relativeFrame, primaryTarget, offset);
        Rotation offsetRotationToJ2000Exp2 = generateAttitude.getOrientation(et, primaryObserver, primaryTarget, secondaryObserver, secondaryTarget).getRotation();

        // The orientation that uses the offset is the nominal orientation
        // rotated by the offset value about the primary target and the
        // primary observer is aligned with the primary target
        assertEquals(Math.toDegrees(Math.acos(Vector3D.dotProduct(spacecraftToJ2000NoCK.applyTo(scYAxis), offsetRotationToJ2000Exp2.applyTo(scYAxis)))), Math.abs(offset),1E-8);
        assertEquals(Math.toDegrees(Math.acos(Math.round(Vector3D.dotProduct(primaryTarget.getPosition(et).normalize(), offsetRotationToJ2000Exp2.applyTo(imgaBoresightSCFrame).normalize())))), 0.0,1E-8);
    }

    private Rotation getFixedFrameRotationWithSpiceCrashOnError(String fromFrame, String toFrame){
        try {
            return AttitudeFunctions.getFixedFrameRotationWithSpice(fromFrame, toFrame);
        } catch (SpiceErrorException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void testCustomVectorSecondaryPointing() {
        // Tests if the secondary observer is as close as possible to the
        // custom secondary vector target while still maintaining the
        // primary observer's lock on the primary target. This is tested by
        // rotating the secondary observer vector about the primary target.
        // If the angle between the secondary observer and secondary target
        // every gets bigger than the original angle then the orientation
        // derived is not the optimal orientation.

        setUp();
        initializePsycheNoCK();

        boolean success = true;
        Time et = new Time("2026 SEP 15 21:45:00");
        String obsBody = "PSYC";
        String scBaseFrame = "PSYC_SPACECRAFT";
        String centerBody = "PSYCHE";
        String relativeFrame = "J2000";
        double[] psycheState = new double[6];
        double[] lightTimeDelay = new double[1];

        // Find the state of Psyche16 relative to the Sun
        try {
            CSPICE.spkezr("PSYCHE", et.toET(), relativeFrame, "LT+S", "SUN", psycheState, lightTimeDelay);
        } catch (SpiceErrorException e) {
            e.printStackTrace();
        }
        // Find the normal to 16Psyche's plane
        Vector3D psychePlaneNormal = (new Vector3D(psycheState[0], psycheState[1], psycheState[2]).crossProduct(new Vector3D(psycheState[3], psycheState[4], psycheState[5]))).normalize();
        Vector3D psychePlaneNormalUnit = psychePlaneNormal.normalize();
        // Set primary pointing to be IMGA pointing nadir and secondary
        // pointing to be spacecraft y-axis pointing as close to the normal
        // of 16Psyche's plane as possible
        Observer primaryObserver = new SpacecraftInstrumentObserver("PSYC_IMGA", obsBody);
        Target primaryTarget = new BodyCenterPrimaryTarget(centerBody, obsBody, relativeFrame);
        Observer secondaryObserver = new CustomObserver(new Vector3D(0, 1, 0));
        Target secondaryTarget = new CustomSecondaryTarget(psychePlaneNormalUnit, primaryTarget, 0.0);

        // Get orientation by generating it ourselves
        GenerateNoRateMatchAttitudeModel generateAttitude = new GenerateNoRateMatchAttitudeModel(null, null, Duration.SECOND_DURATION, new Duration("00:01:00"));
        Rotation spacecraftToJ2000NoCK = generateAttitude.getOrientation(et, primaryObserver, primaryTarget, secondaryObserver, secondaryTarget).getRotation();

        // First show that we are pointing at 16Psyche with IMGA
        double[] psycToPsyche = new double[3];
        try {
            CSPICE.spkpos(centerBody, et.toET(), relativeFrame,"LT+S", obsBody, psycToPsyche, lightTimeDelay);
        } catch (SpiceErrorException er) {
            er.printStackTrace();
        }
        Vector3D psycToPsycheUnit = new Vector3D(psycToPsyche).normalize();

        double angleBetweenIMGAAndPsyche = AttitudeFunctions.angleBetweenObjectAndBoresight(spacecraftToJ2000NoCK, getFixedFrameRotationWithSpiceCrashOnError("PSYC_IMGA", scBaseFrame), psycToPsycheUnit);

        // Find how close the secondary observer can get to the secondary
        // target
        double angleBetweenPosYAndPsycheNormal = AttitudeFunctions.angleBetweenObjectAndVector(spacecraftToJ2000NoCK, Rotation.IDENTITY, psychePlaneNormalUnit, new Vector3D(0,1,0));

        // Find if any other possible orientation (that still satisfies
        // primary constraints) is better than the previously derived
        // orientation
        for (int i = 1; i < 3600; i++) {
            double[] newSecondaryObserverVector = {0, 0, 0};
            try {
                newSecondaryObserverVector = CSPICE.vrotv(spacecraftToJ2000NoCK.applyTo(new Vector3D(0, 1, 0)).toArray(), psycToPsycheUnit.toArray(), Math.toRadians(i/10.0));
            } catch (SpiceErrorException e) {
                e.printStackTrace();
            }
            double newAngleBetweenPosYAndPsycheNormal = Math.toDegrees(Vector3D.angle(new Vector3D(newSecondaryObserverVector), psychePlaneNormalUnit));
            if (newAngleBetweenPosYAndPsycheNormal < angleBetweenPosYAndPsycheNormal){
                success = false;
            }
        }

        // Confirm the projection of the target onto the plane of
        // rotation is aligned with the observer (Note: if the primary
        // and secondary observers are not orthogonal, like they are
        // in this case, you would need to get the projection of the
        // observer onto the rotation plane as well)
        Vector3D projectedTargetVector = psychePlaneNormal.subtract(psycToPsycheUnit.scalarMultiply(psychePlaneNormal.dotProduct(psycToPsycheUnit)));
        double angleBetweenProjectedTargetAndObserver = AttitudeFunctions.angleBetweenObjectAndVector(spacecraftToJ2000NoCK, Rotation.IDENTITY, projectedTargetVector, new Vector3D(0,1,0));


        // Fails if primary observer isn't pointing at the primary target
        // or if the secondary observer isn't pointed as close as possible
        // to the secondary target
        assertTrue(success);
        assertEquals(0, angleBetweenIMGAAndPsyche, 10E-14);
        assertEquals(0, angleBetweenProjectedTargetAndObserver, 10E-14);

    }
}