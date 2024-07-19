package missionmodel.gnc;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.BooleanValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.Vector3DValueMapper;
import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import missionmodel.gnc.blackbird.interfaces.Orientation;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import static gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource.resource;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;

public class GncDataModel {
  public MutableResource<Discrete<Vector3D>> PointingAxis;
  public MutableResource<Discrete<Double>> PointingRotation;
  public MutableResource<Discrete<Boolean>> IsSlewing;

  public GncDataModel(Registrar registrar) {
    PointingAxis = resource(discrete(new Vector3D(0.0, 0.0, 1.0)));
    registrar.discrete("PointingAxis", PointingAxis, new Vector3DValueMapper(new DoubleValueMapper()));
    PointingRotation = resource(discrete(0.0));
    registrar.discrete("PointingRotation", PointingRotation, new DoubleValueMapper());
    IsSlewing = resource(discrete(Boolean.FALSE));
    registrar.discrete("IsSlewing", IsSlewing, new BooleanValueMapper());
  }
}
