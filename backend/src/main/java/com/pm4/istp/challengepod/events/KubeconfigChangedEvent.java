package com.pm4.istp.challengepod.events;

import java.time.Instant;

public record KubeconfigChangedEvent(Instant changedAt) {
  public KubeconfigChangedEvent() {
    this(Instant.now());
  }
}
