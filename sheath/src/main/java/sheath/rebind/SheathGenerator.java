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
package sheath.rebind;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.IncrementalGenerator;
import com.google.gwt.core.ext.RebindMode;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import dagger.internal.ModuleAdapter;

import dagger.internal.StaticInjection;

import sheath.AbstractSheath;
import sheath.Sheath;
import sheath.Modules;

import dagger.internal.Binding;

import dagger.Module;

public class SheathGenerator extends IncrementalGenerator {

  @Override
  public RebindResult generateIncrementally(TreeLogger logger, GeneratorContext context,
      String typeName) throws UnableToCompleteException {
    TypeOracle oracle = context.getTypeOracle();
    JClassType toGenerate = oracle.findType(typeName).isInterface();
    if (toGenerate == null) {
      logger.log(TreeLogger.ERROR, typeName + " is not an interface type");
      throw new UnableToCompleteException();
    }

    JClassType sheathType = oracle.findType(Sheath.class.getCanonicalName());
    if (!toGenerate.isAssignableTo(sheathType)) {
      logger.log(TreeLogger.ERROR, typeName + " is not assignable to " + Sheath.class.getCanonicalName());
      throw new UnableToCompleteException();
    }
    if (toGenerate.equals(sheathType)) {
      logger.log(TreeLogger.ERROR, "You must declare an interface that extends " + Sheath.class.getCanonicalName());
      throw new UnableToCompleteException();
    }

    // TODO: really generate incrementally!

    Class<?>[] moduleClasses = collectAllModules(logger, toGenerate);

    Map<String, String> injectableTypes = new LinkedHashMap<String, String>();
    Set<Class<?>> staticInjections = new LinkedHashSet<Class<?>>();
    for (Class<?> module : moduleClasses) {
      Module annotation = module.getAnnotation(Module.class);
      String moduleName = module.getCanonicalName();
      for (Class<?> key : annotation.injects()) {
        injectableTypes.put(key.getName(), moduleName);
      }
      for (Class<?> c : annotation.staticInjections()) {
        staticInjections.add(c);
      }
    }

    String packageName = toGenerate.getPackage().getName();
    String simpleSourceName = toGenerate.getName().replace('.', '_') + "Impl";
    PrintWriter pw = context.tryCreate(logger, packageName, simpleSourceName);
    if (pw == null) {
      return new RebindResult(RebindMode.USE_ALL_NEW_WITH_NO_CACHING, packageName + "." + simpleSourceName);
    }

    ClassSourceFileComposerFactory factory = new ClassSourceFileComposerFactory(packageName, simpleSourceName);
    factory.setSuperclass(AbstractSheath.class.getCanonicalName());
    factory.addImplementedInterface(typeName);
    SourceWriter sw = factory.createSourceWriter(context, pw);

    // Linker
    sw.println("private static class Plugin extends AbstractSheathPlugin {");
    sw.indent();
    sw.println("@java.lang.Override");
    sw.println("public native %1$s<?> getAtInjectBinding(%2$s key, %2$s className, boolean mustBeInjectable) /*-{",
        Binding.class.getCanonicalName(), String.class.getCanonicalName());
    sw.indent();
    sw.println("switch (className) {");
    LinkedHashMap<String, String> factoryMethodsToGenerate = new LinkedHashMap<String, String>();
    for (JClassType type : oracle.getTypes()) {
      // XXX: workaround for http://code.google.com/p/google-web-toolkit/issues/detail?id=6799
      // We really should use type.getName().endsWith("$$InjectAdapter") but GWT makes a JClassType
      // with name "InjectAdapter", thinking it's a nested class, and we lose the part before the
      // dollar. So in the mean time, look for the InjectAdapter in the classpath FOR EACH CLASS,
      // and we additionally generate a non-JSNI class to instantiate it, so that processing of the
      // class name is done by JDT rather than GWT's TypeOracle.
      String adapterName = type.getQualifiedSourceName() + "$$InjectAdapter";
      boolean found;
      try {
        Class.forName(adapterName, false, Thread.currentThread().getContextClassLoader());
        found = true;
      } catch (Throwable t) {
        found = false;
      }
      if (found /*&& type.isAssignableTo(oracle.findType(Binding.class.getCanonicalName()))*/) {
        String name = type.getQualifiedSourceName();
        String factoryName = "create_" + name.replace('.', '_') + "_InjectAdapter";
        factoryMethodsToGenerate.put(adapterName, factoryName);
        sw.println("case '%s': return this.@%s.Plugin::%s()();", name, factory.getCreatedClassName(), factoryName);
      }
    }
    sw.println("default: return null;");
    sw.println("}");
    sw.outdent();
    sw.println("}-*/;");
    for (Map.Entry<String, String> factoryMethodToGenerate : factoryMethodsToGenerate.entrySet()) {
      sw.println();
      sw.println("private %s %s() {", factoryMethodToGenerate.getKey(), factoryMethodToGenerate.getValue());
      sw.indentln("return new %s();", factoryMethodToGenerate.getKey());
      sw.println("}");
    }
    sw.println();
    sw.println("@java.lang.Override");
    sw.println("public %s[] createStaticInjections() {", StaticInjection.class.getCanonicalName());
    sw.indent();
    sw.println("return new %s[] {", StaticInjection.class.getCanonicalName());
    sw.indent();
    for (Class<?> staticInjection : staticInjections) {
      sw.println("new %s$$StaticInjection(),", staticInjection.getName());
    }
    sw.outdent();
    sw.println("};");
    sw.outdent();
    sw.println("}");
    sw.outdent();
    sw.print("}");

    // Constructor
    sw.println();
    sw.println("public %s() {", simpleSourceName);
    sw.indent();
    sw.println("super(new Plugin(), ");
    sw.println("new %s<?>[] {", ModuleAdapter.class.getCanonicalName());
    sw.indent();
    for (Class<?> module : moduleClasses) {
      sw.println("new %s$$ModuleAdapter(),", module.getName());
    }
    sw.outdent();
    sw.println("});");
    sw.outdent();
    sw.outdent();
    sw.println("}");

    for (JMethod method : toGenerate.getOverridableMethods()) {
      // TODO: check arguments (number, injectable types)
      if (method.getParameterTypes().length != 1) {
        // could be injectStatics()
        continue;
      }
      JType toInject = method.getParameterTypes()[0];
      sw.println("@java.lang.Override");
      sw.println("public %s %s(%s instance) {",
          method.getReturnType().getParameterizedQualifiedSourceName(),
          method.getName(),
          toInject.getParameterizedQualifiedSourceName());
      sw.indent();
      sw.println("doInject(instance, \"members/%s\", %s.class);",
          toInject.getParameterizedQualifiedSourceName(),
          injectableTypes.get(toInject.getQualifiedBinaryName()));
      if (!JPrimitiveType.VOID.equals(method.getReturnType())) {
        sw.println("return arg;");
      }
      sw.outdent();
      sw.println("}");
    }

    sw.commit(logger);

    return new RebindResult(RebindMode.USE_ALL_NEW_WITH_NO_CACHING, packageName + "." + simpleSourceName);
  }

  @Override
  public long getVersionId() {
    return 0;
  }

  private Class<?>[] collectAllModules(TreeLogger logger, JClassType toGenerate) throws UnableToCompleteException {
    Modules modules = toGenerate.getAnnotation(Modules.class);
    if (modules == null) {
      logger.log(TreeLogger.ERROR,
          String.format("No @%s annotation found on type %s.", Modules.class.getCanonicalName(), toGenerate.getQualifiedSourceName()));
      throw new UnableToCompleteException();
    }
    LinkedHashSet<Class<?>> result = new LinkedHashSet<Class<?>>();
    collectModulesRecursively(modules.value(), result);
    return result.toArray(new Class<?>[result.size()]);
  }

  private void collectModulesRecursively(Class<?>[] modules, Set<Class<?>> result) {
    for (Class<?> module : modules) {
      if (result.add(module)) {
        collectModulesRecursively(module.getAnnotation(Module.class).includes(), result);
      }
    }
  }
}
