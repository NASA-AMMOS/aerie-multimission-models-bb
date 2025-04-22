package missionmodel.dsn.adaptationInterface;

public class SpacecraftSpecificDSNConstants {
  public static String spacecraftName;
  public static Integer spacecraftID;

  public static void initializeSpacecraftSpecificDSNConstants(String inputSpacecraftName, Integer inputSpacecraftID) {
    spacecraftName = inputSpacecraftName;
    spacecraftID = inputSpacecraftID;
  }
}
