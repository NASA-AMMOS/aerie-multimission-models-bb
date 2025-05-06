package missionmodel.utils;
import static gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource.resource;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;

import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

public final class ArrayedState {
  private ArrayedState() {throw new UnsupportedOperationException("Class is not instantiable.");}

//  public static <K> ArrayedStateBuilder<K> onKeys(final K[] keys) {return onKeys(Arrays.asList(keys));}

  /**
   * First call in fluent call chain to create arrayed states using given keys.
   * @param keys the keys to generate resources for
   * @return builder for continuing call chain
   * @param <K> key type
   */
  public static <K> ArrayedStateBuilder<K> onKeys(Collection<? extends K> keys) {
    return new ArrayedStateBuilder<>() {
      @Override
      public <V> InitialValuedArrayedStateBuilder<K, V> withInitialValues(Function<K, V> initialValueFunction) {
        return new InitialValuedArrayedStateBuilder<>() {
          @Override
          public Map<K, MutableResource<Discrete<V>>> registeredAs(String prefix, Registrar registrar, ValueMapper<V> mapper) {
            return keys.stream().collect(ImmutableMap.toImmutableMap(Function.identity(), k -> {
              var r = resource(discrete(initialValueFunction.apply(k)));
              registrar.discrete(prefix + k, r, mapper);
              return r;
            }));
          }
        };
//        return (prefix, registrar, mapper) -> keys.stream().collect(Collectors.toMap(Function.identity(), k -> {
//          var r = resource(discrete(initialValueFunction.apply(k)));
//          registrar.discrete(prefix + k, r, mapper);
//          return r;
//        }));
//        return new InitialValuedArrayedStateBuilder<>() {
//          @Override
//          public <S extends Resource<Discrete<V>>> InstantiatedArrayedStateBuilder<K, V, S> withStates(Function<V, S> stateFunction) {
//            return (prefix, registrar, mapper) -> keys.stream().collect(Collectors.toMap(k -> k, k -> {
//              var r = stateFunction.compose(initialValueFunction).apply(k);
//              registrar.discrete(prefix + k, r, mapper);
//              return r;
//            }));
//          }
//        };
      }
    };
  }

  public interface ArrayedStateBuilder<K> {
    /**
     * Call in a fluent call chain setting up the initial values for a collection of arrayed states.
     * @param initialValueFunction function to fetch the initial value for a resource with given key
     * @param <V> the value type stored in the resource
     * @return builder for continuing fluent call chain
     */
    <V> InitialValuedArrayedStateBuilder<K, V> withInitialValues(Function<K, V> initialValueFunction);
  }

  public interface InitialValuedArrayedStateBuilder<K, V> {
//    <S extends Resource<Discrete<V>>> InstantiatedArrayedStateBuilder<K, V, S> withStates(Function<V, S> stateFunction);
    Map<K, MutableResource<Discrete<V>>> registeredAs(String prefix, Registrar registrar, ValueMapper<V> mapper);
  }

//  public interface InstantiatedArrayedStateBuilder<K, V, S> {
//    Map<K, S> registeredAs(String prefix, Registrar registrar, ValueMapper<V> valueMapper);
//  }
}
