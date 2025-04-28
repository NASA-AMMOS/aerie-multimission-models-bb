package missionmodel.dsn.support;

import missionmodel.dsn.adaptationInterface.SpacecraftSpecificDSNConstants;
import missionmodel.dsn.constants.GroundStationConstants;
import missionmodel.dsn.resources.GroundStationResources;

import java.util.ArrayList;

public class SetDSNStationConstants {

    // add all stations to the map
    static {
        DSNStationMap.addDSS("DSS-12",          new DSNStation("DSS-12",        "Goldstone",                "DSS_12",           "34M_HEF",  399012,     "DSS-12_TOPO",          "DSS12",    12));
        DSNStationMap.addDSS("DSS-14",          new DSNStation("DSS-14",        "Goldstone",                "DSS_14",           "70M",      399014,     "DSS-14_TOPO",          "DSS14",    14));
        DSNStationMap.addDSS("DSS-15",          new DSNStation("DSS-15",        "Goldstone",                "DSS_15",           "34M_HEF",  399015,     "DSS-15_TOPO",          "DSS15",    15));
        DSNStationMap.addDSS("DSS-16",          new DSNStation("DSS-16",        "Goldstone",                "DSS_16",           "26M",      399016,     "DSS-16_TOPO",          "DSS16",    16));
        DSNStationMap.addDSS("DSS-23",          new DSNStation("DSS-23",        "Goldstone",                "DSS_23",           "34M_BWG",  399023,     "DSS-23_TOPP",          "DSS23",    23));
        DSNStationMap.addDSS("DSS-24",          new DSNStation("DSS-24",        "Goldstone",                "DSS_24",           "34M_BWG",  399024,     "DSS-24_TOPO",          "DSS24",    24));
        DSNStationMap.addDSS("DSS-25",          new DSNStation("DSS-25",        "Goldstone",                "DSS_25",           "34M_BWG",  399025,     "DSS-25_TOPO",          "DSS25",    25));
        DSNStationMap.addDSS("DSS-26",          new DSNStation("DSS-26",        "Goldstone",                "DSS_26",           "34M_SBW",  399026,     "DSS-26_TOPO",          "DSS26",    26));
        DSNStationMap.addDSS("DSS-27",          new DSNStation("DSS-27",        "Goldstone",                "DSS_27",           "34M_BWG",  399027,     "DSS-27_TOPO",          "DSS27",    27));
        DSNStationMap.addDSS("DSS-34",          new DSNStation("DSS-34",        "Canberra",                 "DSS_34",           "34M_SBW",  399034,     "DSS-34_TOPO",          "DSS34",    34));
        DSNStationMap.addDSS("DSS-35",          new DSNStation("DSS-35",        "Canberra",                 "DSS_35",           "34M_SBW",  399035,     "DSS-35_TOPO",          "DSS35",    35));
        DSNStationMap.addDSS("DSS-36",          new DSNStation("DSS-36",        "Canberra",                 "DSS_36",           "34M_BWG",  399036,     "DSS-36_TOPO",          "DSS36",    36));
        DSNStationMap.addDSS("DSS-42",          new DSNStation("DSS-42",        "Canberra",                 "DSS_42",           "34M_HEF",  399042,     "DSS-42_TOPO",          "DSS42",    42));
        DSNStationMap.addDSS("DSS-43",          new DSNStation("DSS-43",        "Canberra",                 "DSS_43",           "70M",      399043,     "DSS-43_TOPO",          "DSS43",    43));
        DSNStationMap.addDSS("DSS-45",          new DSNStation("DSS-45",        "Canberra",                 "DSS_45",           "34M_HEF",  399045,     "DSS-45_TOPO",          "DSS45",    45));
        DSNStationMap.addDSS("DSS-46",          new DSNStation("DSS-46",        "Canberra",                 "DSS_46",           "26M",      399046,     "DSS-46_TOPO",          "DSS46",    46));
        DSNStationMap.addDSS("DSS-48",          new DSNStation("DSS-48",        "Usuda, Japan",             "Usuda",            "64M_JAXA", 398957,     "USUDA_TOPO",           "",         48));
        DSNStationMap.addDSS("DSS-53",          new DSNStation("DSS-53",        "Madrid",                   "DSS_53",           "34M_BWG",  399053,     "DSS-53_TOPP",          "DSS53",    53));
        DSNStationMap.addDSS("DSS-54",          new DSNStation("DSS-54",        "Madrid",                   "DSS_54",           "34M_BWG",  399054,     "DSS-54_TOPO",          "DSS54",    54));
        DSNStationMap.addDSS("DSS-55",          new DSNStation("DSS-55",        "Madrid",                   "DSS_55",           "34M_SBW",  399055,     "DSS-55_TOPO",          "DSS54",    55));
        DSNStationMap.addDSS("DSS-56",          new DSNStation("DSS-56",        "Madrid",                   "DSS_56",           "34M_BWG",  399056,     "DSS-56_TOPP",          "DSS56",    56));
        DSNStationMap.addDSS("DSS-61",          new DSNStation("DSS-61",        "Madrid",                   "DSS_61",           "34M_HEF",  399061,     "DSS-61_TOPO",          "DSS61",    61));
        DSNStationMap.addDSS("DSS-62",          new DSNStation("DSS-62",        "Madrid",                   "DSS_62",           "34M_HEF",  399062,     "DSS-62_TOPO",          "DSS62",    62));
        DSNStationMap.addDSS("DSS-63",          new DSNStation("DSS-63",        "Madrid",                   "DSS_63",           "70M",      399063,     "DSS-63_TOPO",          "DSS63",    63));
        DSNStationMap.addDSS("DSS-65",          new DSNStation("DSS-65",        "Madrid",                   "DSS_65",           "34M_HEF",  399065,     "DSS-65_TOPO",          "DSS65",    65));
        DSNStationMap.addDSS("DSS-66",          new DSNStation("DSS-66",        "Madrid",                   "DSS_66",           "26M",      399066,     "DSS-66_TOPO",          "DSS66",    66));
        DSNStationMap.addDSS("DSS-38",          new DSNStation("DSS-38",        "Kagoshima",                "Kagoshima",        "34M_JAXA", 398961,     "KAGOSHIMA_TOPO",       "",         38));
        DSNStationMap.addDSS("DSS-73",          new DSNStation("DSS-73",        "Perth, Australia",         "Perth",            "15M_ESA",  399503,     "PERTH_TOPO",           "",         73));
        DSNStationMap.addDSS("DSS-74",          new DSNStation("DSS-74",        "New Norcia, Australia",    "New_Norcia",       "35M_ESA",  399507,     "NEW_NORCIA_TOPO",      "",         74));
        DSNStationMap.addDSS("DSS-75",          new DSNStation("DSS-75",        "Kourou, French Guiana",    "Kourou",           "15M_ESA",  399501,     "KOUROU_TOPO",          "",         75));
        DSNStationMap.addDSS("DSS-83",          new DSNStation("DSS-83",        "Cebreros Spain",           "Cebreros",         "35M_ESA",  399508,     "CEBREROS_TOPO",        "",         83));
        DSNStationMap.addDSS("DSS-84",          new DSNStation("DSS-84",        "Malargue, Argentina",      "Malargue",         "35M_ESA",  399512,     "MALARGUE_TOPO",        "",         84));
        DSNStationMap.addDSS("DSS-85",          new DSNStation("DSS-85",        "Santiago, Chile",          "Santiago",         "13M_SSC",  399510,     "SANTIAGO_TOPO",        "",         85));
        DSNStationMap.addDSS("Mauna Kea",       new DSNStation("Mauna Kea",     "Mauna Kea",                "Mauna_Kea",        "Optical",  399000459,  "MAUNA_KEA_TOPO",       "",         0));
        DSNStationMap.addDSS("Cerro Paranal",   new DSNStation("Cerro Paranal", "Cerro Paranal",            "Cerro_Paranal",    "Optical",  399000250,  "CERRO_PARANAL_TOPO",   "",         0));
        DSNStationMap.addDSS("Cerro Tololo",    new DSNStation("Cerro Tololo",  "Cerro Tololo",             "Cerro_Tololo",     "Optical",  399000698,  "CERRO_TOLOLO_TOPO",    "",         0));
        DSNStationMap.addDSS("La Palma",        new DSNStation("La Palma",      "La Palma",                 "La_Palma",         "Optical",  399000833,  "LA_PALMA_TOPO",        "",         0));
        DSNStationMap.addDSS("Mount John",      new DSNStation("Mount John",    "Mount John",               "Mount_John",       "Optical",  399000365,  "MOUNT_JOHN_TOPO",      "",         0));
        DSNStationMap.addDSS("Greenbank",       new DSNStation("Greenbank",     "Greenbank (GBT)",          "Greenbank_(GBT)",  "Non-DSN",  399000004,  "GREENBANK_TOPO",       "",         0));
        DSNStationMap.addDSS("VLA",             new DSNStation("VLA",           "VLA",                      "VLA",              "Non-DSN",  399000003,  "VLA_TOPO",             "",         0));
        DSNStationMap.addDSS("OCTL",            new DSNStation("OCTL",          "Table Mountain",           "OCTL",             "Optical",  398962,     "OCTL_TOPO",            "",         0));
        DSNStationMap.addDSS("ASFS",            new DSNStation("ASFS",          "Alaska (ASF)",             "ASFS",             "11M",      399101720,  "NDOSL_ASFS_TOPO",      "",         1720));
        DSNStationMap.addDSS("AS3S",            new DSNStation("AS3S",          "Alaska (ASF)",             "AS3S",             "11M",      399101744,  "NDOSL_AS3S_TOPO",      "",         1744));
        DSNStationMap.addDSS("MC1S",            new DSNStation("MC1S",          "McMurdo, Antarctica",      "MC1S",             "10M",      399104848,  "NDOSL_MC1S_TOPO",      "",         4848));
        DSNStationMap.addDSS("DXAS",            new DSNStation("DXAS",          "Poker Flats, Alaska (USN)","DXAS",             "10M",      399101711,  "NDOSL_DXAS_TOPO",      "",         1711));
        DSNStationMap.addDSS("SG1S",            new DSNStation("SG1S",          "Svalbard, Norway",         "SG1S",             "11M",      399101702,  "NDOSL_SG1S_TOPO",      "",         1702));
        DSNStationMap.addDSS("KLMS",            new DSNStation("KLMS",          "Svalbard, Norway",         "KLMS",             "11M",      399101710,  "NDOSL_KLMS_TOPO",      "",         1710));
        DSNStationMap.addDSS("SG3S",            new DSNStation("SG3S",          "Svalbard, Norway",         "SG3S",             "13.6M",    399101733,  "NDOSL_SG3S_TOPO",      "",         1733));
        DSNStationMap.addDSS("TR2S",            new DSNStation("TR2S",          "TrollSat, Antarctica",     "TR2S",             "7.3M",     399101738,  "NDOSL_TR2S_TOPO",      "",         1738));
        DSNStationMap.addDSS("WAPS",            new DSNStation("WAPS",          "Wallops, Virginia",        "WAPS",             "11M",      399101341,  "NDOSL_WAPS_TOPO",      "",         1343));
    }

    // add station types to the type map
    static {
        DSNStationTypeMap.addType("34M_BWG",    new DSNStationType("34M_BWG",   0.4,    0.4,    89.4));
        DSNStationTypeMap.addType("34M_HEF",    new DSNStationType("34M_HEF",   0.4,    0.4,    89.4));
        DSNStationTypeMap.addType("70M",        new DSNStationType("70M",       0.25,   0.25,   89.0));
    }

    // initialize DSN resources
    static {
      // Carter: Don't initialize here; instead, we instantiate GroundStationResources as a sub-model
//        GroundStationResources.initializeGroundStationResources(
//                GroundStationConstants.DOWNLINK_BANDS, DSNStationTypeMap.getDSNStationTypes(), DSNStationMap.getDSSNames());

        ArrayList<String> antenna = new ArrayList<>();
        antenna.add("MGA");
//        GroundStationResources.initializeSpacecraftAntennaResources(antenna);

        SpacecraftSpecificDSNConstants.initializeSpacecraftSpecificDSNConstants("INSIGHT",189);

    }
}
