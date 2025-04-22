package missionmodel.dsn.activities;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;
import gov.nasa.jpl.time.Duration;
import gov.nasa.jpl.time.Time;
import missionmodel.Mission;
import missionmodel.dsn.resources.GroundStationResources;

import static gov.nasa.jpl.time.Time.ERT2ETT;

@ActivityType("AcquireDL")
public class AcquireDL {
  @Export.Parameter
  public String stationID;
  @Export.Parameter
  public String downlinkBand;
  @Export.Parameter
  public String desc;

  @ActivityType.EffectModel
  public AcquireDL(String stationID, String downlinkBand, String desc) {
    this.stationID = stationID;
    this.downlinkBand = downlinkBand;
    this.desc = desc;
  }

  public void run(Mission model) {}

  /**
   * Updates the DssDopplerMode for an individual station based on
   * AnyDssTransmitting and DssTransmitter at the computed ETT.
   * @param ERT
   * @param stationID
   * @param downlinkBand
   */
  public static void acquireIndividualDL(GroundStationResources model, Time ERT, String stationID, String downlinkBand) {
    // make sure handover is complete and outgoing transmitter is off
    Time ETT = ERT2ETT(ERT.add(new Duration("00:00:01.100")));
    if(!model.AnyDssTransmitting.valueAt(ETT)) {
      DssDopplerMode.get(stationID).get(downlinkBand).set(GroundStationRegex.ONE_WAY);
    }
    else if(DssTransmitter.get(stationID).valueAt(ETT)) {
      DssDopplerMode.get(stationID).get(downlinkBand).set(GroundStationRegex.TWO_WAY);
    }
    else {
      DssDopplerMode.get(stationID).get(downlinkBand).set(GroundStationRegex.THREE_WAY);
    }
  }
}
