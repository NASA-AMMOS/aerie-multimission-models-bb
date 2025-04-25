package missionmodel.dsn;

import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import missionmodel.dsn.resources.GroundStationResources;
import missionmodel.dsn.support.SetDSNStationConstants;

public class DsnModel {
  public final GroundStationResources groundStationResources;
  public DsnModel(Registrar registrar) {
    new SetDSNStationConstants();
    this.groundStationResources = new GroundStationResources(registrar);
  }
}
