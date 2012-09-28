/*
 * Copyright (C) 2012 Thomas Broyer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sheath;

import java.util.List;
import java.util.Map;

import dagger.internal.Binding;
import dagger.internal.Linker;
import dagger.internal.ModuleAdapter;
import dagger.internal.UniqueMap;

public abstract class AbstractSheath implements Sheath {

  protected static abstract class AbstractLinker extends Linker {

    @Override protected void reportErrors(List<String> errors) {
      if (errors.isEmpty()) {
        return;
      }
      StringBuilder message = new StringBuilder();
      message.append("Errors creating object graph:");
      for (String error : errors) {
        message.append("\n  ").append(error);
      }
      throw new IllegalArgumentException(message.toString());
    }
  }

  private final Linker linker;

  protected AbstractSheath(Linker linker, ModuleAdapter<?>...adapters) {
    // Extract bindings in the 'base' and 'overrides' set. Within each set no
    // duplicates are permitted.
    Map<String, Binding<?>> baseBindings = new UniqueMap<String, Binding<?>>();
    Map<String, Binding<?>> overrideBindings = new UniqueMap<String, Binding<?>>();
    for (ModuleAdapter<?> adapter : adapters) {
      initModuleAdapter(adapter);

      Map<String, Binding<?>> addTo = adapter.overrides ? overrideBindings : baseBindings;
      adapter.getBindings(addTo);
    }

    // Install all of the user's bindings.
    this.linker = linker;
    linker.installBindings(baseBindings);
    linker.installBindings(overrideBindings);
  }

  private native void initModuleAdapter(ModuleAdapter<?> adapter) /*-{
    adapter.@dagger.internal.ModuleAdapter::module = adapter.@dagger.internal.ModuleAdapter::newModule()();
  }-*/;

  @SuppressWarnings("unchecked")
  protected void doInject(Object instance, String key, Class<?> moduleClass) {
    Binding<?> binding = linker.requestBinding(key, moduleClass);
    if (binding == null || !binding.linked) {
      linker.linkRequested();
      binding = linker.requestBinding(key, moduleClass);
    }
    ((Binding<Object>) binding).injectMembers(instance);
  }
}
