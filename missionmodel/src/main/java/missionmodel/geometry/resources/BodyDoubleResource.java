package missionmodel.geometry.resources;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.Unstructured;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear;

import java.util.HashMap;
import java.util.Map;

public class BodyDoubleResource {
  public static final DoubleValueMapper dvm = new DoubleValueMapper();

  private final Map<String, MutableResource<Discrete<Double>>> discrete;
  private final Map<String, Resource<Unstructured<Double>>> unstructured;
  private final Map<String, Resource<Linear>> linear;

  BodyDoubleResource() {
    this.discrete = new HashMap<>();
    this.unstructured = new HashMap<>();
    this.linear = new HashMap<>();
  }

  public void put(String body, MutableResource<Discrete<Double>> discrete, 
                  Resource<Unstructured<Double>> unstructured, 
                  Resource<Linear> linear) {
    this.discrete.put(body, discrete);
    this.unstructured.put(body, unstructured);
    this.linear.put(body, linear);
  }

  public MutableResource<Discrete<Double>> discrete(String body) {
    return discrete.get(body);
  }

  public Resource<Unstructured<Double>> unstructured(String body) {
    return unstructured.get(body);
  }

  public Resource<Linear> linear(String body) {
    return linear.get(body);
  }

  // Backward compatibility methods
  public Map<String, MutableResource<Discrete<Double>>> discrete() {
    return discrete;
  }

  public Map<String, Resource<Linear>> linear() {
    return linear;
  }
}
