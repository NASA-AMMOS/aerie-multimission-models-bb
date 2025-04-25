package missionmodel.dsn.resources;
import static gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource.resource;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.StringValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.IntegerValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.BooleanValueMapper;
import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import missionmodel.dsn.constants.GroundStationConstants;
import missionmodel.dsn.constants.GroundStationRegex;
import missionmodel.dsn.support.DSNStationTypeMap;

import java.util.Map;
import java.util.stream.Collectors;

import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;
public class GroundStationResources {
  // non-arrayed resources
  public MutableResource<Discrete<Boolean>> SpacecraftInOccultation;
  public MutableResource<Discrete<String>> ActiveTransmitter;
  public MutableResource<Discrete<String>> FirstDdorStation;
  public MutableResource<Discrete<String>> SecondDdorStation;
  public MutableResource<Discrete<Integer>> FirstDdorStationCounter;
  public MutableResource<Discrete<Integer>> SecondDdorStationCounter;
  public MutableResource<Discrete<Integer>> NumberOfAvailableDdorStations;
  public MutableResource<Discrete<Boolean>> DsnTrackScheduleTransition;
  public MutableResource<Discrete<Boolean>> SpacecraftReceiverInLock;
  public MutableResource<Discrete<Boolean>> TelemetryChange;


  // resources per antenna
  // this resource needs to be set by the mission adaptation, but it is checked in the model
  public static Map<String, MutableResource<Discrete<Boolean>>> SpacecraftAntennaEarthPointed;


  // TODO Carter note: these are commented out in the BB model as well
  // resources per station type
  // these resources are not being used yet
//    public Map<String, MutableResource<Discrete<String>>> DsnTrackMode;
//    public Map<String, MutableResource<Discrete<Integer>>> DsnTracksPerWeek;
//    public Map<String, MutableResource<Discrete<Integer>>> DsnDdorPairsPerMonth;
//    public Map<String, MutableResource<Discrete<Duration>>> DsnTrackDuration;


  // resources per downlink band
  // the spacecraft resources need to be set by the mission-specific telecom model,
  // but they are checked in the dsn model
  public Map<String, MutableResource<Discrete<String>>> SpacecraftDownlinkAntenna;
  public Map<String, MutableResource<Discrete<Double>>> SpacecraftDownlinkBitRate;
  public Map<String, MutableResource<Discrete<Boolean>>> SpacecraftCoherency;
  public Map<String, MutableResource<Discrete<Boolean>>> SpacecraftTransmitter;
  public Map<String, MutableResource<Discrete<Boolean>>> SpacecraftAntennaPointed;


  // resources per DSN station
  public Map<String, MutableResource<Discrete<Boolean>>> Dss1Way;
  public Map<String, MutableResource<Discrete<Boolean>>> DssCoherent;

  public Map<String, MutableResource<Discrete<Boolean>>> DssDdorActive;
  public Map<String, MutableResource<Discrete<Boolean>>> DssTransmitter;
  public Map<String, MutableResource<Discrete<Boolean>>> DssTransmitterSupp;

  public Map<String, MutableResource<Discrete<Boolean>>> DssAvailableForPreCal;
  public Map<String, MutableResource<Discrete<Boolean>>> DssAvailableForUplink;
  public Map<String, MutableResource<Discrete<Boolean>>> DssAvailableForDownlink;

  public Map<String, MutableResource<Discrete<Boolean>>> StationView;
  public Map<String, MutableResource<Discrete<Boolean>>> StationAntennaPointing;
  public Map<String, MutableResource<Discrete<Boolean>>> TransmitterView;
  public Map<String, MutableResource<Discrete<Boolean>>> TelemetryCoverageWindow;

  public Map<String, MutableResource<Discrete<String>>> SAFDescriptionPerStation;



  // Carter: mappers needed for Aerie!
  private static final StringValueMapper stringValueMapper = new StringValueMapper();
  private static final IntegerValueMapper integerValueMapper = new IntegerValueMapper();
  private static final BooleanValueMapper booleanValueMapper = new BooleanValueMapper();


  /**
   * Carter: this ctor is ported from the BB `initializeGroundStationResources`. I turned the singleton implementation
   * into a class instance to fit the established pattern for model implementations in Aerie.
   *
   * @param registrar
   */
  public GroundStationResources(Registrar registrar) {
   // TODO Carter doesn't know enough Java to understand why we convert a List<String> to String[]
//   String[] downlinkBandsArray = GroundStationConstants.DOWNLINK_BANDS.toArray(new String[GroundStationConstants.DOWNLINK_BANDS.size()]);
//   String[] dssTypesArray = DSNStationTypeMap.getDSNStationTypes().toArray(new String[DSNStationTypeMap.getDSNStationTypes().size()]);
//   String[] dssNamesArray = DSNStationTypeMap.getDSNStationTypes().toArray(new String[DSNStationTypeMap.getDSNStationTypes().size()]);
   var dssTypesArray = DSNStationTypeMap.getDSNStationTypes();

   ActiveTransmitter = resource(discrete(GroundStationRegex.NONE));
   registrar.discrete("ActiveTransmitter", ActiveTransmitter, stringValueMapper);
   FirstDdorStation = resource(discrete(GroundStationRegex.NONE));
   registrar.discrete("FirstDdorStation", FirstDdorStation, stringValueMapper);
   SecondDdorStation = resource(discrete(GroundStationRegex.NONE));
   registrar.discrete("SecondDdorStation", SecondDdorStation, stringValueMapper);
   FirstDdorStationCounter = resource(discrete(0));
   registrar.discrete("FirstDdorStationCounter", FirstDdorStationCounter, integerValueMapper);
   SecondDdorStationCounter = resource(discrete(0));
   registrar.discrete("SecondDdorStationCounter", SecondDdorStationCounter, integerValueMapper);
   NumberOfAvailableDdorStations = resource(discrete(0));
   registrar.discrete("NumberOfAvailableDdorStations", NumberOfAvailableDdorStations, integerValueMapper);
   SpacecraftInOccultation = resource(discrete(false));
   registrar.discrete("SpacecraftInOccultation", SpacecraftInOccultation, booleanValueMapper);

   DssTransmitter = dssTypesArray.stream().collect(Collectors.toMap(s -> s, s -> {
     var r = resource(discrete(false));
     registrar.discrete("DssTransmitter_" + s, r, booleanValueMapper);
     return r;
   }));

 }

 // Replaced initializeGroundStationResources and singleton with a class instantiation so we can access registrar
//  public static void initializeGroundStationResources(List<String> downlinkBands, List<String> dssTypes, List<String> dssNames) {

}
