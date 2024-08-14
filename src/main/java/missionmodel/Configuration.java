package missionmodel;

import static gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Template;

public record Configuration(String spacecraftId) {
  public static String DEFAULT_SPICE_SCID = "PSYC";
  public static @Template Configuration defaultConfiguration() {
    return new Configuration(DEFAULT_SPICE_SCID);
  }
}
