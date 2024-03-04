package missionmodel;

import java.util.Map;

import static gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Template;

public record Configuration() {

  //public static Map<String, Object> DEFAULT_BODIES = {};
  public static @Template Configuration defaultConfiguration() {
    return new Configuration();
  }
}
