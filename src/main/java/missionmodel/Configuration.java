package missionmodel;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.nio.file.Path;

import static gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Template;

public record Configuration(int spacecraftId, String spacecraftIdString, Path geometryPath) {
  public static int DEFAULT_SPICE_SCID = -74;
  public static String DEFAULT_SPICE_SCID_STR = "MRO";
  public static Path DEFAULT_GEOM_PATH = Path.of("src/test/resources/default_geometry_config.json");
  public static Vector3D ANGULAR_VELOCITY_LIMIT = new Vector3D(0.00175, 0.00175, 0.0175);
  public static Vector3D ANGULAR_ACCELERATION_LIMIT = new Vector3D(4.36e-6, 5.8*(4.36e-6), 4.36e-6);
  public static @Template Configuration defaultConfiguration() {
    return new Configuration(DEFAULT_SPICE_SCID, DEFAULT_SPICE_SCID_STR, DEFAULT_GEOM_PATH);
  }
}
