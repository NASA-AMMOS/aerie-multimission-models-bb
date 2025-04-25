package missionmodel.dsn.constants;

import gov.nasa.jpl.time.Duration;

import java.util.ArrayList;
import java.util.List;

public class GroundStationConstants {
    public static Duration MIN_DUR                            = new Duration("00:00:00.001");//
    public static Duration DSN_TXR_ON_DELAY                   = new Duration("00:05:00");//
    public static Duration DSN_TXR_OFF_DELAY                  = new Duration("00:05:00");//
    public static Duration DSN_TXR_OFF_EXTRA                  = new Duration("00:10:00");//
    public static Duration ROUND_TRANSMITTER_ONOFF_RESOLUTION = new Duration("00:01:00");//
    public static Duration DSN_UL_ACQ_Time                    = new Duration("00:05:00");
    public static Duration DSN_RNG_ON_WAIT                    = new Duration("00:05:00");
    public static Duration DSN_RNG_ACQ_DELAY                  = new Duration("00:05:00");
    public static Duration DSN_Lockup_Time                    = new Duration("00:05:00");
    public static Duration DSN_Minimum_Transmitter_On_Time    = new Duration("00:15:00");//
    public static Duration DSN_Precal_Time                    = new Duration("00:10:00");
    public static Duration DSN_Postcal_Time                   = new Duration("00:05:00");
    public static Duration MIN_DSN_TXR_ON_DURATION            = new Duration("00:15:00");//
    public static Duration MIN_DSN_TXR_OFF_DURATION           = new Duration("00:05:00");
    public static Duration DSN_TRACK_MODULUS                  = new Duration("00:05:00");
    public static Duration DSN_TRANSMITTER_MODULUS            = new Duration("00:01:00");
    public static Duration MIN_UL_TRANSFER_TIME               = new Duration("00:05:00");//
    public static Double   DSN_Transmitter_Elevation_Limit    = 10.5;

    // this is needed because we don't want to depend on the geometry model
    // (if this naif ID changed then way more than the ground station model would break)
    public static final Integer earthNaifID = 399;


    // available downlink bands
    public static final List<String> DOWNLINK_BANDS = new ArrayList<>();
    static {
        DOWNLINK_BANDS.add("X");
        DOWNLINK_BANDS.add("KA");
    }
}
