package missionmodel.spice;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.spice.SpiceLoader;
import spice.basic.*;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Spice {

  public static void initialize(String metaKernelPath) throws SpiceErrorException {
    SpiceLoader.loadSpice();
    CSPICE.kclear();
    CSPICE.furnsh(metaKernelPath);
  }

  public static List<String> getFiles(String kind) throws SpiceErrorException, SpiceKernelNotLoadedException {
    List<String> files = new ArrayList<>();
    var n = KernelDatabase.ktotal(kind);
    for (int i=0; i < n; ++i) {
      var fileName = KernelDatabase.getFileName(i, kind);
      if (fileName != null && fileName.length() > 0) {
        files.add(fileName);
      }
    }
    return files;
  }

  public static SpiceWindow getCoverage(int bodyOrScId) throws SpiceException {
    var files = getFiles("SPK");
    SpiceWindow coverage = null;
    for (String file : files) {
      SPK spk = SPK.openForRead(file);
      SpiceWindow w = spk.getCoverage(bodyOrScId);
      //System.out.println("getCoverage(): " + file + " - " + w);
      if (coverage == null) coverage = w;
      else coverage = coverage.union(w);
    }
    return coverage;
  }

  public static Instant toUTC(double t) throws SpiceErrorException {
    // Convert ET to UTC string in ISO format with microseconds precision
    String utcStr = CSPICE.et2utc(t, "ISOC", 6);
    // SPICE returns format like "2000-01-01 12:00:00.000000"
    // Need to convert to ISO-8601 format for Instant.parse by:
    // 1. Replacing space with T
    // 2. Adding Z for UTC timezone
    Instant instant = Instant.parse(utcStr.replace(" ", "T") + "Z");
    return instant;
  }

  public static gov.nasa.jpl.aerie.merlin.protocol.types.Duration minus(java.time.Instant i1, java.time.Instant i2) {
    var micros1 = i1.getEpochSecond() * 1000000L + i1.getNano() / 1000L;
    var micros2 = i2.getEpochSecond() * 1000000L + i2.getNano() / 1000L;
    gov.nasa.jpl.aerie.merlin.protocol.types.Duration d = gov.nasa.jpl.aerie.merlin.protocol.types.Duration.of(micros1 - micros2, Duration.MICROSECONDS);
    return d;
  }

}



