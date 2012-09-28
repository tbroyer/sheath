package coffee;

import com.google.gwt.user.client.Window;

import dagger.Lazy;
import javax.inject.Inject;

class CoffeeMaker {
  @Inject Lazy<Heater> heater; // Don't want to create a possibly costly heater until we need it.
  @Inject Pump pump;

  public void brew() {
    heater.get().on();
    pump.pump();
    Window.alert(" [_]P coffee! [_]P ");
    heater.get().off();
  }
}
