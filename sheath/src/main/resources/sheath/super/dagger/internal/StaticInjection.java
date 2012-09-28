/*
 * Copyright (C) 2012 Square Inc.
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
package dagger.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

/**
 * Injects the static fields of a class.
 *
 * @author Jesse Wilson
 */
public final class StaticInjection {
  // TODO: emulate for GWT

  public static StaticInjection get(Class<?> c) {
    throw new UnsupportedOperationException();
  }

  public void attach(Linker linker) {
    throw new UnsupportedOperationException();
  }

  public void inject() {
    throw new UnsupportedOperationException();
  }
}
