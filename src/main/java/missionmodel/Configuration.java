package missionmodel;

import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;

import java.nio.file.Path;
import java.util.Map;

import static gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Template;

public record Configuration(Path geometryConfigDataPath, Integer spiceSpacecraftId) {

  public static final Path DEFAULT_GEO_DATA_PATH = Path.of("/etc/os-release");

  public static final Integer DEFAULT_SPICE_SCID = -61; // Juno

  @Export.Validation("data path must exist")
  public boolean validateGeometryConfigDataPath() {
    return geometryConfigDataPath.toFile().exists();
  }

  public static @Template Configuration defaultConfiguration() {
    return new Configuration(DEFAULT_GEO_DATA_PATH, DEFAULT_SPICE_SCID);
  }
}
