package missionmodel;

import java.nio.file.Path;

import static gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Template;

public record Configuration(int spacecraftId, String spacecraftIdString, Path geometryPath) {
  public static int DEFAULT_SPICE_SCID = -74;
  public static String DEFAULT_SPICE_SCID_STR = "MRO";
  public static @Template Configuration defaultConfiguration() {
    return new Configuration(DEFAULT_SPICE_SCID, DEFAULT_SPICE_SCID_STR, Path.of("src/test/resources/default_geometry_config.json"));
  }
}
