package missionmodel;

import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;

import java.nio.file.Path;
import java.util.Map;

import static gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Template;

public record Configuration() {
  public static @Template Configuration defaultConfiguration() {
    return new Configuration();
  }
}
