@MissionModel(model = Mission.class)
@WithMappers(BasicValueMappers.class)
@WithConfiguration(Configuration.class)
// Activity Types
@WithActivityType(Apoapsis.class)
@WithActivityType(Periapsis.class)
@WithActivityType(EnterOccultation.class)
@WithActivityType(ExitOccultation.class)
@WithActivityType(SpacecraftEnterEclipse.class)
@WithActivityType(SpacecraftExitEclipse.class)
@WithActivityType(AddPeriapsis.class)
@WithActivityType(AddApoapsis.class)
@WithActivityType(AddOccultations.class)
@WithActivityType(AddSpacecraftEclipses.class)
// Activity Types - GNC
@WithActivityType(PointingActivity.class)

// @WithMetadata(name = "unit", annotation = gov.nasa.jpl.aerie.contrib.metadata.Unit.class) // for unit support
package missionmodel;

import gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel.WithActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel.WithConfiguration;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel.WithMappers;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel.WithMetadata;
import missionmodel.geometry.activities.atomic.*;
import missionmodel.geometry.activities.spawner.AddApoapsis;
import missionmodel.geometry.activities.spawner.AddOccultations;
import missionmodel.geometry.activities.spawner.AddPeriapsis;
import missionmodel.geometry.activities.spawner.AddSpacecraftEclipses;
import missionmodel.gnc.activities.PointingActivity;
