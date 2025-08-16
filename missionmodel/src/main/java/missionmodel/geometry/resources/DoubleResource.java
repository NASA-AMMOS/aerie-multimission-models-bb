package missionmodel.geometry.resources;

import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.Unstructured;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear;

public record DoubleResource(
  MutableResource<Discrete<Double>> discrete,
  Resource<Unstructured<Double>> unstructured,
  Resource<Linear> linear
) {
}
