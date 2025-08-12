package missionmodel;

import gov.nasa.jpl.aerie.contrib.streamline.core.Resources;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.time.Time;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class AbsoluteClock {

  private final Instant startTime;

  public AbsoluteClock(final Instant startTime) {
    this.startTime = startTime;
  }

  public Instant getStartTime() {
    return startTime;
  }

  public Instant now() {
    return getStartTime().plusMillis(Resources.currentTime().in(Duration.MILLISECONDS));
  }

}
