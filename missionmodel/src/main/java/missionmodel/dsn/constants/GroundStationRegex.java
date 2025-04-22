package missionmodel.dsn.constants;

import java.util.regex.Pattern;

public class GroundStationRegex {
  // DSN config code regex
  public static final Pattern DSN_CONFIG_CODE_SFDU_PATTERN = Pattern.compile(
    "(?<codeName>[0-9a-zA-Z]{4})\\s(?<ARRAY>[0-9])\\s(?<CCP>[0-9])\\s" +
      "(?<CMD>[0-9])\\s(?<HTR>[0-9])\\s(?<KHMT>[0-9])\\s(?<KUPL>[0-9])\\s" +
      "(?<LHMT>[0-9])\\s(?<NMC>[0-9])\\s(?<OLR>[0-9])\\s(?<RNG>[0-9])\\s" +
      "(?<RRPA>[0-9])\\s(?<RRPB>[0-9])\\s(?<SHMT>[0-9])\\s(?<STWM>[0-9])\\s" +
      "(?<STXH>[0-9])\\s(?<STXL>[0-9])\\s(?<TLPA>[0-9])\\s(?<TLPB>[0-9])\\s" +
      "(?<TSA>[0-9])\\s(?<UPL>[0-9])\\s(?<VRA>[0-9])\\s(?<XHMT>[0-9])\\s" +
      "(?<XTWM>[0-9])\\s(?<XTXH>[0-9])\\s(?<XTXL>[0-9])");

  // SAF regex
  public static final Pattern SAF_PATTERN = Pattern.compile(
    "^.(?<yy>\\d\\d)\\s(?<ddd>\\d{3})\\s(?<boa>\\d{4})\\s(?<bot>\\d{4})" +
      "\\s(?<eot>\\d{4})\\s(?<eoa>\\d{4})\\s(?<stationID>.{6})\\s(?<proj>.{5})" +
      "\\s(?<desc>.{16})\\s(?<pass>.{4})\\s(?<configCode>.{7})\\s(?<workCode>.{3})");

  // VP regex
  public static final String VP_START_IN_VIEW_REGEX = "START IN VIEW";

  public static final String VP_END_IN_VIEW_REGEX = "END IN VIEW";

  public static final String VP_RISE_REGEX = "RISE";

  public static final String VP_SET_REGEX = "SET";

  public static final String VP_TRX_ON_LIM_LOW_REGEX = "TRX ON LIM LOW";

  public static final String VP_TRX_OFF_LIM_LOW_REGEX = "TRX OFF LIM LOW";

  public static final String VP_AOS_HOR_MASK_REGEX = "AOS HOR MASK";

  public static final String VP_LOS_HOR_MASK_REGEX = "LOS HOR MASK";

  public static final String VP_TRX_OFF_REGEX = "TRX OFF LIM LOW"; // not in the VP file

  public static final String VP_TRX_ON_REGEX = "TRX ON LIM LOW"; // not in the VP file

  public static final String VP_ALL_EVENTS_REGEX =
    VP_START_IN_VIEW_REGEX + "|" +
      VP_END_IN_VIEW_REGEX + "|" +
      VP_RISE_REGEX + "|" +
      VP_SET_REGEX + "|" +
      VP_TRX_ON_LIM_LOW_REGEX + "|" +
      VP_TRX_OFF_LIM_LOW_REGEX + "|" +
      VP_AOS_HOR_MASK_REGEX + "|" +
      VP_LOS_HOR_MASK_REGEX;

  public static final Pattern VP_PATTERN = Pattern.compile(
    "^(?<yy>\\d\\d)" +
      "\\s(?<ddd>\\d{3})" +
      "\\/(?<hhmmss>\\d\\d:\\d\\d:\\d\\d)" +
      "\\s(?<event>" + VP_ALL_EVENTS_REGEX + ")" +
      "\\s+(?<spacecraftID>\\d{3})" +
      "\\s(?<stationID>\\d\\d)");

  // strings for doppler modes
  public static final String NONE         = "None";
  public static final String ONE_WAY      = "1-Way";
  public static final String TWO_WAY      = "2-Way";
  public static final String THREE_WAY    = "3-Way";

  // andOr strings
  public static final String AND          = "AND";
  public static final String OR           = "OR";

  // subsystem strings
  public static final String GroundStation = "GroundStation";

  // Downlink/Uplink Event strings
  public static final String TrackingAllocation   = "TrackingAllocation";
  public static final String UplinkAcquisition    = "UplinkAcquisition";
  public static final String Transmitter_On       = "Transmitter_On";
  public static final String Transmitter_Off      = "Transmitter_Off";
  public static final String EnterOccultation     = "EnterOccultation";
  public static final String ExitOccultation      = "ExitOccultation";
  public static final String OnEarthPoint         = "OnEarthPoint";
  public static final String OffEarthPoint        = "OffEarthPoint";
  public static final String AOSHorizonMask       = "AOSHorizonMask";
  public static final String LOSHorizonMask       = "LOSHorizonMask";
  public static final String CoherencyOff         = "CoherencyOff";
  public static final String CoherencyOn          = "CoherencyOn";
  public static final String TelemetryChange      = "TelemetryChange";
  public static final String DSSTransmitterOn     = "DSSTransmitterOn";
  public static final String DSSTransmitterOff    = "DSSTransmitterOff";
}
