package missionmodel.geometry.activities.atomic;

import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import missionmodel.Mission;
import missionmodel.geometry.resources.EclipseTypes;
import missionmodel.geometry.spiceinterpolation.GenericGeometryCalculator;
import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial.polynomial;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad.effect;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Naming.name;

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
    set(model.geometryResources.SpacecraftEclipseByBody().get(body), EclipseTypes.NONE);

    EclipseTypes worstOverallEclipseType = getWorstEclipseFromAllBodies(model);
    set(model.geometryResources.AnySpacecraftEclipse(), worstOverallEclipseType);

    boolean linear = model.geometryCalculator instanceof GenericGeometryCalculator ggc && ggc.useLinearResources;
    var polyRes = model.geometryResources.FractionOfSunNotInEclipse().polynomial();
    MutableResource<Polynomial> polyMutRes = null;
    if (polyRes instanceof MutableResource<Polynomial> pmr) {
      polyMutRes = pmr;
    }

    if(worstOverallEclipseType.equals(EclipseTypes.NONE)){
      if (linear) {
        pset(polyMutRes, polynomial(1.0));
      } else {
        set(model.geometryResources.FractionOfSunNotInEclipse().discrete(), 1.0);
      }
    }
    delay(Duration.SECOND);
  }

  private void pset(MutableResource<Polynomial> resource, Polynomial p) {
    if (resource != null) {
      resource.emit(name(effect($ -> p), "Set %s discretely", p));
    }
  }
}
