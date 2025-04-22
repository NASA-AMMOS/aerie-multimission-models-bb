package missionmodel.dsn.resources;
import static gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource.resource;

import gov.nasa.jpl.aerie.contrib.serialization.mappers.StringValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.IntegerValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.BooleanValueMapper;
import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import missionmodel.dsn.constants.GroundStationRegex;

import java.util.List;
import java.util.Map;

import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;
public class GroundStationResources {
  public MutableResource<Discrete<String>> ActiveTransmitter;
  public MutableResource<Discrete<String>> FirstDdorStation;
  public MutableResource<Discrete<String>> SecondDdorStation;
  public MutableResource<Discrete<Integer>> FirstDdorStationCounter;
  public MutableResource<Discrete<Integer>> SecondDdorStationCounter;
  public MutableResource<Discrete<Integer>> NumberOfAvailableDdorStations;
  public MutableResource<Discrete<Boolean>> SpacecraftInOccultation;
  public MutableResource<Discrete<Boolean>> DsnTrackScheduleTransition;
  public MutableResource<Discrete<Boolean>> SpacecraftReceiverInLock;
  public MutableResource<Discrete<Boolean>> TelemetryChange;

//  Jump...
  public Map<String, MutableResource<Discrete<Boolean>>> DssTransmitter;

  private static final StringValueMapper stringValueMapper = new StringValueMapper();
  private static final IntegerValueMapper integerValueMapper = new IntegerValueMapper();
  private static final BooleanValueMapper booleanValueMapper = new BooleanValueMapper();
 public GroundStationResources(Registrar registrar) {
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

   DssTransmitter =
 }

  public static void initializeGroundStationResources(List<String> downlinkBands, List<String> dssTypes, List<String> dssNames) {
//   TODO
    String[] downlinkBandsArray = downlinkBands.toArray(new String[downlinkBands.size()]);
    String[] dssTypesArray = dssTypes.toArray(new String[dssTypes.size()]);
    String[] dssNamesArray = dssNames.toArray(new String[dssNames.size()]);
  }

}
