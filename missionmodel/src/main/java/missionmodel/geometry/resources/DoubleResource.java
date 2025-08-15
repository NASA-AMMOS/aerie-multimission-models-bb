package missionmodel.geometry.resources;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.Approximation;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.SecantApproximation;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.Unstructured;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.linear.Linear;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import missionmodel.geometry.spiceinterpolation.Body;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource.resource;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.IntervalFunctions.byUniformSampling;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box.UnstructuredResources.approximateAsLinear;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.assumeLinear;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialResources.constant;
import static missionmodel.JPLTimeConvertUtility.getDuration;

public class DoubleResource {
  private static final String EARTH = "EARTH";
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

  // DoubleResource(String name, String body, Map<String, Body> bodyObjects, boolean useLinearResources, boolean registerLinear, boolean registerDiscrete, boolean optimizeSampling, Function<Duration, Double> f, Optional<Registrar> reg) {
  //   discrete = resource(Discrete.discrete(0.0));
  //   unstructured = resource(Unstructured.timeBased(f));
  //   linear = !useLinearResources ? null : maybeApproximateAsLinear(unstructured, body, bodyObjects, useLinearResources, optimizeSampling);
  //   register_p(reg, name, discrete, linear, dvm, registerLinear, registerDiscrete);
  // }


  // private Resource<Linear> maybeApproximateAsLinear(Resource<Unstructured<Double>> resource, String body, Map<String, Body> bodyObjects, boolean useLinearResources, boolean optimizeSampling) {
  //   if (!useLinearResources) {
  //     return assumeLinear(constant(0.0)); // dummy resource
  //   }
  //   if (optimizeSampling) {
  //     return approximateAsLinear(resource);
  //   }
  //   var periods = bodyObjects.get(body).calculationPeriods();
  //   if (periods.isEmpty() && !body.equalsIgnoreCase(EARTH)) {
  //     periods = bodyObjects.get(EARTH).calculationPeriods();
  //   }
  //   Duration samplePeriod = periods.isEmpty() ? Duration.of(24, Duration.HOURS) : getDuration(periods.get(0).getMaxTimeStep());
  //   return approximateUniformalyAsLinear(resource, samplePeriod);
  // }

  // public static Resource<Linear> approximateUniformalyAsLinear(Resource<Unstructured<Double>> resource, Duration samplePeriod) {
  //   if ( samplePeriod == null ) samplePeriod = Duration.HOUR;
  //   return Approximation.approximate(resource, SecantApproximation.<Unstructured<Double>>secantApproximation(byUniformSampling(samplePeriod)));
  // }

  // private void register_p(Optional<Registrar> r, String name, Resource<Discrete<Double>> rd, Resource<Linear> rl,
  //                         ValueMapper<Double> vm, boolean registerLinear, boolean registerDiscrete) {
  //   if (r.isEmpty()) return;
  //   if (registerLinear) r.get().real(name, rl);
  //   if (registerDiscrete) r.get().discrete(name, rd, vm);
  // }

  // private void register_u(Optional<Registrar> r, String name, Resource<Discrete<Double>> rd,
  //                         Resource<Unstructured<Double>> ru, String body, Map<String, Body> bodyObjects, boolean linear, boolean registerDiscrete, boolean optimizeSampling) {
  //   if (r.isEmpty()) return;
  //   if (linear) r.get().real(name, maybeApproximateAsLinear(ru, body, bodyObjects, linear, optimizeSampling));
  //   if (registerDiscrete) r.get().discrete(name, rd, dvm);
  // }

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
