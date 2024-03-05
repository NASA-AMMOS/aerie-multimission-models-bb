package missionmodel.geometry.activities.spawner;

import missionmodel.geometry.activities.atomic.Periapsis;
import missionmodel.geometry.directspicecalls.SpiceDirectEventGenerator;
import missionmodel.geometry.interfaces.GeometryInformationNotAvailableException;
import gov.nasa.jpl.time.Duration;
import gov.nasa.jpl.time.Time;

import java.util.ArrayList;
import java.util.List;

public class AddPeriapsis {
  String body;
  String target;
  Duration stepSize;
  double maxDistanceFilter;

  public AddPeriapsis(Time t, Duration searchLength, String body, String target, Duration stepSize, double maxDistanceFilter) {
    super(t, searchLength, body, target, stepSize, maxDistanceFilter);
    setDuration(searchLength);
    this.body = body;
    this.target = target;
    this.stepSize = stepSize;
    this.maxDistanceFilter = maxDistanceFilter;
  }

  public void decompose(){
    SpiceDirectEventGenerator generator = new SpiceDirectEventGenerator();

    List<Time> periapsisTimes;

    try {
      periapsisTimes = generator.getPeriapses(getStart(), getEnd(), stepSize, body, target, maxDistanceFilter, "NONE");
    } catch (GeometryInformationNotAvailableException e) {
      periapsisTimes = new ArrayList<>();
    }

    for(Time periapsisTime : periapsisTimes){
      spawn(new Periapsis(periapsisTime, target));
    }
  }
}
