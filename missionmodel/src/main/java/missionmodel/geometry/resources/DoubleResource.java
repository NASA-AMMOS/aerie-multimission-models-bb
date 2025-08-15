package missionmodel.geometry.resources;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.Unstructured;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear;

public class DoubleResource {
  public static final DoubleValueMapper dvm = new DoubleValueMapper();

  private final MutableResource<Discrete<Double>> discrete;
  private final Resource<Unstructured<Double>> unstructured;
  private final Resource<Linear> linear;

  DoubleResource(MutableResource<Discrete<Double>> resource, Resource<Unstructured<Double>> unstructured,
                 Resource<Linear> linear) {
    this.discrete = resource;
    this.unstructured = unstructured;
    this.linear = linear;
  }

  public MutableResource<Discrete<Double>> discrete() {
    return discrete;
  }

  public Resource<Unstructured<Double>> unstructured() {
    return unstructured;
  }

  public Resource<Linear> linear() {
    return linear;
  }
}
