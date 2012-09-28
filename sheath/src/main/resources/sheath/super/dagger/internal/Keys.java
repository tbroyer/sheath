/*
 * Copyright (C) 2012 Square, Inc.
 * Copyright (C) 2012 Google, Inc.
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

import dagger.Lazy;
import dagger.MembersInjector;
import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;
import javax.inject.Provider;
import javax.inject.Qualifier;

/**
 * Formats strings that identify the value to be injected. Keys are of one of
 * three forms:
 * <ol>
 *   <li>{@code com.square.Foo}: provides instances of Foo.
 *   <li>{@code @com.square.Bar/com.square.Foo}: provides instances of Foo
 *       qualified by the annotation.
 *   <li>{@code members/com.square.Foo}: injects members of Foo.
 * </ol>
 * Bindings from {@code @Provides} methods are of the first two types. Bindings
 * created from {@code @Inject}-annotated members of a class are of the first
 * and last types.
 */
public final class Keys {
  private static final String PROVIDER_PREFIX = "javax.inject.Provider<";
  private static final String MEMBERS_INJECTOR_PREFIX = "dagger.MembersInjector<";
  private static final String LAZY_PREFIX = "dagger.Lazy<";
  private static final String SET_PREFIX = "java.util.Set<";

  Keys() {
  }

  /**
   * Returns a key for the type provided by, or injected by this key. For
   * example, if this is a key for a {@code Provider<Foo>}, this returns the
   * key for {@code Foo}. This retains annotations and supports both Provider
   * keys and MembersInjector keys.
   */
  static String getBuiltInBindingsKey(String key) {
    int start = startOfType(key);
    if (substringStartsWith(key, start, PROVIDER_PREFIX)) {
      return extractKey(key, start, key.substring(0, start), PROVIDER_PREFIX);
    } else if (substringStartsWith(key, start, MEMBERS_INJECTOR_PREFIX)) {
      return extractKey(key, start, "members/", MEMBERS_INJECTOR_PREFIX);
    } else {
      return null;
    }
  }

  /**
   * Returns a key for the underlying binding of a Lazy<T> value. For example,
   * if this is a key for a {@code Lazy<Foo>}, this returns the key for
   * {@code Foo}. This retains annotations.
   */
  static String getLazyKey(String key) {
    int start = startOfType(key);
    if (substringStartsWith(key, start, LAZY_PREFIX)) {
      return extractKey(key, start, key.substring(0, start), LAZY_PREFIX);
    } else {
      return null;
    }
  }

  /**
   * Returns the start of a key if it is a plain key, and the start of the
   * underlying key if it is an annotated key
   */
  private static int startOfType(String key) {
    return (key.startsWith("@")) ? key.lastIndexOf('/') + 1 : 0;
  }

  /**
   * Returns an unwrapped key (the key for T from a Provider<T> for example),
   * removing all wrapping key information, but preserving annotations or known
   * prefixes.
   *
   * @param key the key from which the delegate key should be extracted.
   * @param start
   *          an index into the key representing the key's "real" start after
   *          any annotations.
   * @param delegatePrefix
   *          key prefix elements extracted from the underlying delegate
   *          (annotations, "members/", etc.)
   * @param prefix the prefix to strip.
   */
  private static String extractKey(String key, int start, String delegatePrefix, String prefix) {
    return delegatePrefix + key.substring(start + prefix.length(), key.length() - 1);
  }

  /** Returns true if {@code string.substring(offset).startsWith(substring)}. */
  private static boolean substringStartsWith(String string, int offset, String substring) {
    return string.regionMatches(offset, substring, 0, substring.length());
  }

  /** Returns true if {@code key} is a binding that supports members injection. */
  public static boolean isMembersInjection(String key) {
    return key.startsWith("members/");
  }

  /** Returns true if {@code key} has a qualifier annotation. */
  public static boolean isAnnotated(String key) {
    return key.startsWith("@");
  }

  /**
   * Returns the class name for {@code key}, if {@code key} was created with a
   * class instance. Returns null if {@code key} represents a parameterized type
   * or an array type.
   */
  public static String getClassName(String key) {
    int start = 0;
    if (key.startsWith("@") || key.startsWith("members/")) {
      start = key.lastIndexOf('/') + 1;
    }
    return (key.indexOf('<', start) == -1 && key.indexOf('[') == -1)
        ? key.substring(start)
        : null;
  }

  /** Returns true if {@code name} is the name of a platform-provided class. */
  public static boolean isPlatformType(String name) {
    return name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("android.");
  }
}
