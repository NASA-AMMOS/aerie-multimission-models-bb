package missionmodel.geometry.activities.atomic;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import missionmodel.Mission;
import missionmodel.geometry.resources.EclipseTypes;

import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.set;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;
import static missionmodel.geometry.activities.atomic.SpacecraftEnterEclipse.getWorstEclipseFromAllBodies;

@ActivityType("SpacecraftExitEclipse")
public class SpacecraftExitEclipse {

  @Export.Parameter
  public String body;

  public SpacecraftExitEclipse() {};

  public SpacecraftExitEclipse(String body) {
    this.body = body;
  }

  @ActivityType.EffectModel
  public void run(Mission model){
    set(model.geometryResources.getSpacecraftEclipseByBody().get(body), EclipseTypes.NONE);

    EclipseTypes worstOverallEclipseType = getWorstEclipseFromAllBodies(model);
    set(model.geometryResources.getAnySpacecraftEclipse(), worstOverallEclipseType);

    if(worstOverallEclipseType.equals(EclipseTypes.NONE)){
      set(model.geometryResources.getFractionOfSunNotInEclipse(), 1.0);
    }
    delay(Duration.SECOND);
  }

}
