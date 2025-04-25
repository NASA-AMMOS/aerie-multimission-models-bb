package missionmodel.dsn.support;

public class DSNStation {
  private String dssName;         // key for the station
  private String complex;         // location of the station (Goldstone, Canberra, etc.)
  private String actName;         // activity name for the station (underscores and different name)
  private String antennaType;     // 34M_HEF, 34M_BWG, 70M, etc.
  private Integer antennaNAIFId;  // ID of station in NAIF
  private String antennaFrame;    // name of topocentric frame used for station
  private String MMTATDSS;        // Multiple Mirror Telescope at DSS
  private Integer antennaNumber;  // integer for number of station

  public DSNStation(String dssName, String complex, String actName, String antennaType,
                    Integer antennaNAIFId, String antennaFrame, String MMTATDSS, Integer antennaNumber) {
    this.dssName = dssName;
    this.complex = complex;
    this.actName = actName;
    this.antennaType = antennaType;
    this.antennaNAIFId = antennaNAIFId;
    this.antennaFrame = antennaFrame;
    this.MMTATDSS = MMTATDSS;
    this.antennaNumber = antennaNumber;
  }

  public String getDssName() {
    return dssName;
  }

  public String getComplex() {
    return complex;
  }

  public String getActName() {
    return actName;
  }

  public String getAntennaType() {
    return antennaType;
  }

  public Integer getAntennaNAIFId() {
    return antennaNAIFId;
  }

  public String getAntennaFrame() {
    return antennaFrame;
  }

  public String getMMTATDSS() {
    return MMTATDSS;
  }

  public Integer getAntennaNumber() {
    return antennaNumber;
  }

  public Double getMaxAzTrackingRate() {
    return DSNStationTypeMap.getType(antennaType).getMaxAzTrackingRate();
  }

  public Double getMaxElTrackingRate() {
    return DSNStationTypeMap.getType(antennaType).getMaxElTrackingRate();
  }

  public Double getMaxElevation() {
    return DSNStationTypeMap.getType(antennaType).getMaxElevation();
  }
}
