package missionmodel;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.nio.file.Path;
import java.util.List;

import static gov.nasa.jpl.aerie.merlin.framework.annotations.Export.Template;

public record Configuration(int spacecraftId,
                            String spacecraftIdString,
                            Path geometryPath,
                            List<Double> gncAngularVelocityLimit,
                            List<Double> gncAngularAccelerationLimit,
                            boolean gncRateMatching,
                            boolean useLinearResources
                           ) {
  public static int DEFAULT_SPICE_SCID = -74;
  public static String DEFAULT_SPICE_SCID_STR = "MRO";
  public static Path DEFAULT_GEOM_PATH = Path.of("src/test/resources/default_geometry_config.json");
  public static List<Double> ANGULAR_VELOCITY_LIMIT = List.of(1e-3, 1e-3, 1e-3);
  public static List<Double> ANGULAR_ACCELERATION_LIMIT = List.of(5e-6, 5e-6, 5e-6);
  public static boolean DEFAULT_GNC_RATE_MATCHING = false;
  public static boolean DEFAULT_USE_LINEAR_RESOURCES = false;
  public static @Template Configuration defaultConfiguration() {
    return new Configuration(DEFAULT_SPICE_SCID, DEFAULT_SPICE_SCID_STR, DEFAULT_GEOM_PATH,
      ANGULAR_VELOCITY_LIMIT, ANGULAR_ACCELERATION_LIMIT, DEFAULT_GNC_RATE_MATCHING, DEFAULT_USE_LINEAR_RESOURCES);
  }
  public static @Template Configuration linearConfiguration() {
    return new Configuration(DEFAULT_SPICE_SCID, DEFAULT_SPICE_SCID_STR, DEFAULT_GEOM_PATH,
      ANGULAR_VELOCITY_LIMIT, ANGULAR_ACCELERATION_LIMIT, DEFAULT_GNC_RATE_MATCHING, true);
  }
}
