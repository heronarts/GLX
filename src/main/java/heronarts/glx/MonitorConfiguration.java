package heronarts.glx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MonitorConfiguration {

  private final List<Monitor> mutableMonitors = new ArrayList<>();
  public final List<Monitor> monitors = Collections.unmodifiableList(this.mutableMonitors);

  public MonitorConfiguration(List<Monitor> monitors) {
    this.mutableMonitors.addAll(monitors);
  }

  // TODO: add equals() method

}

