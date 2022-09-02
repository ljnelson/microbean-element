/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2022 microBean™.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.microbean.element;

import java.lang.module.ModuleDescriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.Directive;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.NoType;

public class DefaultModuleElement extends AbstractElement implements ModuleElement {


  /*
   * Static fields.
   */

  
  static final Lock cacheLock = new ReentrantLock();

  // @GuardedBy("cacheLock")
  static final Map<AnnotatedName, DefaultModuleElement> cache = new HashMap<>();


  /*
   * Instance fields.
   */

  
  private final DefaultName simpleName;

  private final boolean open;

  private final Set<Name> enclosedPackageNames;

  private final List<Directive> directives;
  
  private final List<? extends Directive> readOnlyDirectives;

  private DefaultModuleElement(final AnnotatedName fullyQualifiedName) {
    this(fullyQualifiedName, false, List.of());
  }

  private DefaultModuleElement(final AnnotatedName fullyQualifiedName,
                               final boolean open) {
    this(fullyQualifiedName, open, List.of());
  }

  private DefaultModuleElement(final AnnotatedName fullyQualifiedName,
                               final boolean open,
                               final List<? extends Directive> directives) {
    super(fullyQualifiedName,
          ElementKind.MODULE,
          DefaultNoType.MODULE,
          Set.of(),
          null, // enclosingElement
          null); // enclosedElementsSupplier
    this.enclosedPackageNames = ConcurrentHashMap.newKeySet();
    this.simpleName = DefaultName.ofSimple(fullyQualifiedName.getName());
    this.open = open;
    this.directives = new CopyOnWriteArrayList<>();
    this.readOnlyDirectives = Collections.unmodifiableList(this.directives);
    if (directives != null) {
      for (final Directive d : directives) {
        this.addDirective(d);
      }
    }
  }

  @Override // AbstractElement
  public <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
    return v.visitModule(this, p);
  }

  @Override // AbstractElement
  public final NoType asType() {
    return (NoType)super.asType();
  }

  @Override // ModuleElement
  public final boolean isOpen() {
    return this.open;
  }

  final void addDirective(final Directive directive) {
    this.directives.add(directive);
  }
  
  @Override // ModuleElement
  public final List<? extends Directive> getDirectives() {
    return this.readOnlyDirectives;
  }
                              
  @Override // ModuleElement
  public final Name getQualifiedName() {
    return super.getSimpleName();
  }

  @Override // ModuleElement
  public final Name getSimpleName() {
    return this.simpleName;
  }

  final void addEnclosedElement(DefaultPackageElement p) {
    if (this.enclosedPackageNames.add(p.getQualifiedName())) {
      super.addEnclosedElement0(p);
    }
  }
  
  @Override // ModuleElement
  public final Element getEnclosingElement() {
    return null;
  }

  @Override // ModuleElement
  public final boolean isUnnamed() {
    return this.getSimpleName().length() <= 0;
  }


  /*
   * Static methods.
   */


  public static final DefaultModuleElement of(final AnnotatedName fullyQualifiedName) {
    return of(fullyQualifiedName, false, List.of());
  }

  public static final DefaultModuleElement of(final AnnotatedName fullyQualifiedName, final boolean open) {
    return of(fullyQualifiedName, open, List.of());
  }

  public static final DefaultModuleElement of(final AnnotatedName fullyQualifiedName,
                                              final boolean open,
                                              final List<? extends Directive> directives) {
    final DefaultModuleElement e = new DefaultModuleElement(fullyQualifiedName, open, directives);
    cacheLock.lock();
    try {
      final DefaultModuleElement returnValue = cache.putIfAbsent(fullyQualifiedName, e);
      return returnValue == null ? e : returnValue;
    } finally {
      cacheLock.unlock();
    }
  }

  public static final DefaultModuleElement of(final Module m) throws ClassNotFoundException {
    return of(m, Thread.currentThread().getContextClassLoader());
  }
  
  public static final DefaultModuleElement of(final Module m, final ClassLoader cl) throws ClassNotFoundException {
    DefaultModuleElement returnValue;
    final AnnotatedName name = AnnotatedName.of(m.getName());
    cacheLock.lock();
    try {
      returnValue = cache.get(name);
      if (returnValue == null) {
        final ModuleDescriptor md = m.getDescriptor();        
        returnValue = new DefaultModuleElement(name, md == null || md.modifiers().contains(ModuleDescriptor.Modifier.OPEN));
        cache.put(name, returnValue);

        final Set<ModuleDescriptor.Exports> exports;
        if (md == null) {
          exports = Set.of();
        } else {
          exports = new TreeSet<>(md.exports());
        }
        for (final ModuleDescriptor.Exports export : exports) {
          final DefaultPackageElement packageElement = DefaultPackageElement.of(DefaultName.of(export.source())); // INDIRECT RECURSIVE
          returnValue.addEnclosedElement(packageElement);
          final List<DefaultModuleElement> targetModuleElements;
          if (export.isQualified()) {
            final ModuleLayer layer = m.getLayer();
            final Set<String> moduleNames = new TreeSet<>(export.targets());
            targetModuleElements = new ArrayList<>(moduleNames.size());
            for (final String moduleName : moduleNames) {
              final Module tm = layer.findModule(moduleName).orElse(null);
              if (tm != null) {
                targetModuleElements.add(DefaultModuleElement.of(tm, cl)); // RECURSIVE
              }
            }
          } else {
            targetModuleElements = List.of();
          }
          returnValue.addDirective(new DefaultExportsDirective(packageElement, targetModuleElements));
        }

        final Set<ModuleDescriptor.Opens> openses;
        if (md == null) {
          openses = Set.of();
        } else {
          openses = new TreeSet<>(md.opens());
        }
        for (final ModuleDescriptor.Opens opens : openses) {
          final DefaultPackageElement packageElement = DefaultPackageElement.of(DefaultName.of(opens.source())); // INDIRECT RECURSIVE
          returnValue.addEnclosedElement(packageElement);
          final List<DefaultModuleElement> targetModuleElements;
          if (opens.isQualified()) {
            final ModuleLayer layer = m.getLayer();
            final Set<String> moduleNames = new TreeSet<>(opens.targets());
            targetModuleElements = new ArrayList<>(moduleNames.size());
            for (final String moduleName : moduleNames) {
              final Module tm = layer.findModule(moduleName).orElse(null);
              if (tm != null) {
                targetModuleElements.add(DefaultModuleElement.of(tm, cl)); // RECURSIVE
              }
            }
          } else {
            targetModuleElements = List.of();
          }
          returnValue.addDirective(new DefaultOpensDirective(packageElement, targetModuleElements));
        }

        final Set<ModuleDescriptor.Provides> provideses;
        if (md == null) {
          provideses = Set.of();
        } else {
          provideses = new TreeSet<>(md.provides());
        }
        for (final ModuleDescriptor.Provides provides : provideses) {
          final DefaultTypeElement service = DefaultTypeElement.of(Class.forName(provides.service(), false, cl));
          final Collection<? extends String> providers = provides.providers();
          final List<TypeElement> implementations = new ArrayList<>(providers.size());
          for (final String provider : providers) {
            implementations.add(DefaultTypeElement.of(Class.forName(provider, false, cl))); // INDIRECT RECURSIVE
          }
          returnValue.addDirective(new DefaultProvidesDirective(service, implementations));
        }

      }
    } finally {
      cacheLock.unlock();
    }
    return returnValue;
  }

  private static final List<? extends Directive> directivesFor(final Module m,
                                                               final ClassLoader cl,
                                                               final Consumer<? super DefaultPackageElement> moduleElement)
    throws ClassNotFoundException {
    final ArrayList<Directive> returnValue = new ArrayList<>();
    cacheLock.lock();
    try {
      final ModuleDescriptor md = m.getDescriptor();
      final Set<ModuleDescriptor.Exports> exports = new TreeSet<>(md.exports());
      for (final ModuleDescriptor.Exports export : exports) {
        final List<DefaultModuleElement> targetModuleElements;
        if (export.isQualified()) {
          final ModuleLayer layer = m.getLayer();
          final Set<String> moduleNames = new TreeSet<>(export.targets());
          targetModuleElements = new ArrayList<>(moduleNames.size());
          for (final String moduleName : moduleNames) {
            final Module tm = layer.findModule(moduleName).orElse(null);
            if (tm != null) {
              targetModuleElements.add(DefaultModuleElement.of(tm, cl)); // RECURSIVE
            }
          }
        } else {
          targetModuleElements = List.of();
        }
        final DefaultPackageElement packageElement = DefaultPackageElement.of(DefaultName.of(export.source())); // INDIRECT RECURSIVE
        moduleElement.accept(packageElement);
        returnValue.add(new DefaultExportsDirective(packageElement, targetModuleElements));
      }

      final Set<ModuleDescriptor.Opens> openses = new TreeSet<>(md.opens());
      for (final ModuleDescriptor.Opens opens : openses) {
        final List<DefaultModuleElement> targetModuleElements;
        if (opens.isQualified()) {
          final ModuleLayer layer = m.getLayer();
          final Set<String> moduleNames = new TreeSet<>(opens.targets());
          targetModuleElements = new ArrayList<>(moduleNames.size());
          for (final String moduleName : moduleNames) {
            final Module tm = layer.findModule(moduleName).orElse(null);
            if (tm != null) {
              targetModuleElements.add(DefaultModuleElement.of(tm, cl)); // RECURSIVE
            }
          }
        } else {
          targetModuleElements = List.of();
        }
        final DefaultPackageElement packageElement = DefaultPackageElement.of(DefaultName.of(opens.source())); // INDIRECT RECURSIVE
        moduleElement.accept(packageElement);
        returnValue.add(new DefaultOpensDirective(packageElement, targetModuleElements));
      }

      final Set<ModuleDescriptor.Provides> provideses = new TreeSet<>(md.provides());
      for (final ModuleDescriptor.Provides provides : provideses) {
        final Collection<? extends String> providers = provides.providers();
        final List<TypeElement> implementations = new ArrayList<>(providers.size());
        for (final String provider : providers) {
          implementations.add(DefaultTypeElement.of(Class.forName(provider, false, cl))); // INDIRECT RECURSIVE
        }
        final DefaultTypeElement service = DefaultTypeElement.of(Class.forName(provides.service(), false, cl));
        returnValue.add(new DefaultProvidesDirective(service, implementations));
      }      
      
    } finally {
      cacheLock.unlock();
    }
    returnValue.trimToSize();
    return Collections.unmodifiableList(returnValue);
  }

}
