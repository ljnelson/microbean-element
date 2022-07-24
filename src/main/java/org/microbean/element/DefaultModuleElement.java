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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

import javax.lang.model.type.NoType;

public class DefaultModuleElement extends AbstractElement implements ModuleElement {

  private final DefaultName simpleName;

  private final boolean open;

  private final List<Directive> directives;
  
  private final List<? extends Directive> readOnlyDirectives;

  public DefaultModuleElement(final Name fullyQualifiedName) {
    this(fullyQualifiedName, false, null);
  }

  public DefaultModuleElement(final Name fullyQualifiedName, final boolean open) {
    this(fullyQualifiedName, open, null);
  }

  private DefaultModuleElement(final Name fullyQualifiedName,
                               final boolean open,
                               final List<? extends AnnotationMirror> annotationMirrors) {
    super(fullyQualifiedName,
          ElementKind.MODULE,
          DefaultNoType.MODULE,
          Set.of(),
          null, // enclosingElement
          null, // enclosedElementsSupplier
          annotationMirrors);
    this.simpleName = DefaultName.ofSimple(fullyQualifiedName);
    this.open = open;
    this.directives = new CopyOnWriteArrayList<>();
    this.readOnlyDirectives = Collections.unmodifiableList(this.directives);
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


  public static final DefaultModuleElement of(final Module m) {
    return DefaultModuleElement.of(m, new ConcurrentHashMap<>(), null);
  }

  static final <P extends PackageElement & Encloseable> DefaultModuleElement of(final Module m, final P enclosedElement) {
    return DefaultModuleElement.of(m, new ConcurrentHashMap<>(), enclosedElement);
  }

  @SuppressWarnings("unchecked")
  private static final <P extends PackageElement & Encloseable> DefaultModuleElement of(final Module m, final ConcurrentMap<String, DefaultModuleElement> seen, final P enclosedElement) {
    if (enclosedElement != null && enclosedElement.getKind() != ElementKind.PACKAGE) {
      throw new IllegalArgumentException("enclosedElement: " + enclosedElement);
    }
    final String name = m.getName();
    DefaultModuleElement returnValue = seen.get(name);
    if (returnValue == null) {

      final DefaultName defaultName = DefaultName.of(name);
      final ModuleDescriptor md = m.getDescriptor();
      final boolean open = md.modifiers().contains(ModuleDescriptor.Modifier.OPEN);
      returnValue = new DefaultModuleElement(defaultName, open);
      seen.putIfAbsent(name, returnValue);
      
      final Set<ModuleDescriptor.Exports> exports = new TreeSet<>(md.exports());
      for (final ModuleDescriptor.Exports export : exports) {

        final String source = export.source();
        final P packageElement;
        if (enclosedElement != null && enclosedElement.getQualifiedName().contentEquals(source)) {
          packageElement = enclosedElement;
          returnValue.addEnclosedElement(enclosedElement);
        } else {
          packageElement = (P)DefaultPackageElement.of(DefaultName.of(source));
          returnValue.addEnclosedElement(packageElement);
        }

        final List<DefaultModuleElement> targetModuleElements;
        if (export.isQualified()) {
          final ModuleLayer layer = m.getLayer();
          final Set<String> moduleNames = new TreeSet<>(export.targets());
          targetModuleElements = new ArrayList<>(moduleNames.size());
          for (final String moduleName : moduleNames) {
            final Module targetModule = layer.findModule(moduleName).orElse(null);
            if (targetModule != null) {
              targetModuleElements.add(DefaultModuleElement.of(targetModule, seen, null));
            }
          }
        } else {
          targetModuleElements = List.of();
        }
        returnValue.addDirective(new DefaultExportsDirective(packageElement, targetModuleElements));
      }
    }
    return returnValue;
  }

  private static final List<? extends Directive> directivesOf(final Module m, final ConcurrentMap<String, DefaultModuleElement> seen, final PackageElement enclosedElement) {
    if (enclosedElement != null && enclosedElement.getKind() != ElementKind.PACKAGE) {
      throw new IllegalArgumentException("enclosedElement: " + enclosedElement);
    }
    ArrayList<Directive> returnValue = new ArrayList<>();
    final ModuleDescriptor md = m.getDescriptor();
    final Set<ModuleDescriptor.Exports> exports = new TreeSet<>(md.exports());
    for (final ModuleDescriptor.Exports export : exports) {
      
      final String source = export.source();
      final PackageElement packageElement;
      if (enclosedElement != null && enclosedElement.getQualifiedName().contentEquals(source)) {
        packageElement = enclosedElement;
      } else {
        packageElement = DefaultPackageElement.of(DefaultName.of(source));
      }

      final List<DefaultModuleElement> targetModuleElements;
      if (export.isQualified()) {
        final ModuleLayer layer = m.getLayer();
        final Set<String> moduleNames = new TreeSet<>(export.targets());
        targetModuleElements = new ArrayList<>(moduleNames.size());
        for (final String moduleName : moduleNames) {
          final Module targetModule = layer.findModule(moduleName).orElse(null);
          if (targetModule != null) {
            targetModuleElements.add(DefaultModuleElement.of(targetModule, seen, null));
          }
        }
      } else {
        targetModuleElements = List.of();
      }
      returnValue.add(new DefaultExportsDirective(packageElement, targetModuleElements));
    }
    returnValue.trimToSize();
    return returnValue;
  }
  
}
