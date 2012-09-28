package coffee;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;

import javax.inject.Inject;

import sheath.Modules;
import sheath.SheathEntryPoint;

class CoffeeApp implements EntryPoint {
  @Modules(DripCoffeeModule.class)
  interface CoffeeGagger extends SheathEntryPoint<CoffeeApp> { }

  @Inject CoffeeMaker coffeeMaker;

  @Override
  public void onModuleLoad() {
    CoffeeGagger gagger = GWT.create(CoffeeGagger.class);
    gagger.inject(this);

    Button btn = Button.wrap(Document.get().getElementById("coffeeMaker"));
    btn.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          coffeeMaker.brew();
        }
      });
    btn.setEnabled(true);
  }
}
