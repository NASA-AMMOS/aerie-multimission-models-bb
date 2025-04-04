package missionmodel.spice;

import gov.nasa.jpl.aerie.spice.SpiceLoader;
import spice.basic.CSPICE;
import spice.basic.SpiceErrorException;

import java.io.File;
import java.util.ArrayList;

public class Spice {
  private static boolean spiceImported = false;

  public static void initialize(String metaKernelPath) throws SpiceErrorException {
    if (!spiceImported) {
      SpiceLoader.loadSpice();
      spiceImported = true;
    }
    CSPICE.kclear();
    CSPICE.furnsh(metaKernelPath);
  }

}



