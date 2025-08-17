package missionmodel.geometry.resources;

import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.asPolynomial$;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.assumeLinear;

import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.Unstructured;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.Polynomial;

public record DoubleResource(
  MutableResource<Discrete<Double>> discrete,
  Resource<Unstructured<Double>> unstructured,
  Resource<Polynomial> polynomial,
  Resource<Linear> linear
) {
  public static DoubleResource makeDUP(MutableResource<Discrete<Double>> discrete, Resource<Unstructured<Double>> unstructured, Resource<Polynomial> polynomial) {
    return new DoubleResource(discrete, unstructured, polynomial, assumeLinear(polynomial));
  }

  public static DoubleResource makeDUL(MutableResource<Discrete<Double>> discrete, Resource<Unstructured<Double>> unstructured, Resource<Linear> linear) {
    return new DoubleResource(discrete, unstructured, linear);
  }
  
  public DoubleResource(MutableResource<Discrete<Double>> discrete, Resource<Unstructured<Double>> unstructured, Resource<Linear> linear) {
    this(discrete, unstructured, asPolynomial$(linear), linear);
  }
  
}
