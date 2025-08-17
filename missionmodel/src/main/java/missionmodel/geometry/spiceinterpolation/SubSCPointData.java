package missionmodel.geometry.spiceinterpolation;

/**
 * Record to hold all subSC point data values.
 * This avoids redundant calls to getSubPointInformation.
 */
public record SubSCPointData(
  double distance,
  double latitude,
  double longitude,
  double radius,
  double lst
) {}
