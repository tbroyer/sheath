package coffee;

import com.google.gwt.user.client.Window;

class ElectricHeater implements Heater {
  boolean heating;

  @Override public void on() {
    Window.alert("~ ~ ~ heating ~ ~ ~");
    this.heating = true;
  }

  @Override public void off() {
    this.heating = false;
  }

  @Override public boolean isHot() {
    return heating;
  }
}
