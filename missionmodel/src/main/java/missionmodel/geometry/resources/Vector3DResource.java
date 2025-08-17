package missionmodel.geometry.resources;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.Unstructured;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.util.List;

public class Vector3DResource {
  public static final DoubleValueMapper dvm = new DoubleValueMapper();

  private final MutableResource<Discrete<Vector3D>> discrete;
  private final Resource<Unstructured<Vector3D>> unstructured;
  private final Resource<Linear>[] linear; // [x, y, z, magnitude]

  Vector3DResource(MutableResource<Discrete<Vector3D>> discrete, 
                   Resource<Unstructured<Vector3D>> unstructured,
                   Resource<Linear>[] linear) {
    this.discrete = discrete;
    this.unstructured = unstructured;
    this.linear = linear;
  }

  public MutableResource<Discrete<Vector3D>> discrete() {
    return discrete;
  }

  public Resource<Unstructured<Vector3D>> unstructured() {
    return unstructured;
  }

  public Resource<Linear>[] linear() {
    return linear;
  }

  // Convenience methods for individual components
  public Resource<Linear> x() {
    return linear != null && linear.length > 0 ? linear[0] : null;
  }

  public Resource<Linear> y() {
    return linear != null && linear.length > 1 ? linear[1] : null;
  }

  public Resource<Linear> z() {
    return linear != null && linear.length > 2 ? linear[2] : null;
  }

  public Resource<Linear> magnitude() {
    return linear != null && linear.length > 3 ? linear[3] : null;
  }

  // Convert to list for backward compatibility
  public List<Resource<Linear>> toList() {
    if (linear == null) return List.of();
    return List.of(linear);
  }
}
