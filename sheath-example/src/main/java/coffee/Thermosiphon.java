package coffee;

import com.google.gwt.user.client.Window;

import javax.inject.Inject;

class Thermosiphon implements Pump {
  private final Heater heater;

  @Inject
  Thermosiphon(Heater heater) {
    this.heater = heater;
  }

  @Override public void pump() {
    if (heater.isHot()) {
      Window.alert("=> => pumping => =>");
    }
  }
}
