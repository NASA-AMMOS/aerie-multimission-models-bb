package missionmodel.dsn.support;

import gov.nasa.jpl.engine.ParameterDeclaration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DSNStationMap extends ParameterDeclaration {
    private static Map<String, DSNStation> DSNStationMap = new HashMap<>();

    public static void addDSS(String dssName, DSNStation dsnStation) {
        DSNStationMap.put(dssName, dsnStation);
    }

    public static DSNStation getDSS(String DSSName) {
        if(DSNStationMap.containsKey(DSSName)) {
            return DSNStationMap.get(DSSName);
        }
        else {
            throw new RuntimeException("Error calling getDSS with DSS name " + DSSName +
                ". DSNStationMap does not contain this DSS name.");
        }
    }

    public static List<String> getDSSNames() {
        return new ArrayList<>(DSNStationMap.keySet());
    }
}
