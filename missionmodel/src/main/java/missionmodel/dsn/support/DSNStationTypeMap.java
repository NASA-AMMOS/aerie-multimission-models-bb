package missionmodel.dsn.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DSNStationTypeMap {
    private static Map<String, DSNStationType> DSNStationTypeMap = new HashMap<>();

    protected static void addType(String type, DSNStationType dsnStationType) {
        DSNStationTypeMap.put(type, dsnStationType);
    }

    public static DSNStationType getType(String dsnStationType) {
        if(DSNStationTypeMap.containsKey(dsnStationType)) {
            return DSNStationTypeMap.get(dsnStationType);
        }
        else {
            throw new RuntimeException("Error calling getType with DSN station type " + dsnStationType +
                    ". DSNStationTypeMap does not contain this DSN station type.");
        }
    }

    public static List<String> getDSNStationTypes() {
        return new ArrayList<>(DSNStationTypeMap.keySet());
    }
}
