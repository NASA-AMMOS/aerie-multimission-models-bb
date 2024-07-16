package missionmodel.geometry.activities.atomic;

import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;
import missionmodel.Mission;

@ActivityType("ExitOccultation")
public class ExitOccultation {

  @Export.Parameter
  public String body = "";
  @Export.Parameter
  public String station = "DSS-24";

  public ExitOccultation() {};

  public ExitOccultation(String body, String station) {
    this.body = body;
    this.station = station;
  }

  @ActivityType.EffectModel
  public void run(Mission model){
    //setGroup("OccultationEvents");
    // setName("EnterOccultation_" + body + "_SeenFrom_" + station);
    DiscreteEffects.decrement(model.geometryResources.Occultation, 1);
    DiscreteEffects.turnOff(model.geometryResources.SpacecraftOccultationByBodyAndStation.get(body).get(station));
  }
}
