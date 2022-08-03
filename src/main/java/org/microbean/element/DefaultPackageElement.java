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

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import java.util.function.Supplier;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.NoType;

public class DefaultPackageElement extends AbstractElement implements PackageElement {


  /*
   * Static fields.
   */


  static final Map<AnnotatedName, DefaultPackageElement> cache = new ConcurrentHashMap<>();


  /*
   * Instance fields.
   */


  private final Set<Name> enclosedTypeNames;
  
  private final DefaultName simpleName;


  /*
   * Constructors.
   */


  private DefaultPackageElement(final AnnotatedName fullyQualifiedName) {
    this(fullyQualifiedName, DefaultNoType.PACKAGE);
  }

  private DefaultPackageElement(final AnnotatedName fullyQualifiedName, final NoType packageType) {
    super(fullyQualifiedName,
          ElementKind.PACKAGE,
          packageType,
          Set.of(),
          null,
          null);
    this.simpleName = DefaultName.ofSimple(fullyQualifiedName.getName());
    this.enclosedTypeNames = ConcurrentHashMap.newKeySet();
  }


  /*
   * Instance methods.
   */


  @Override // AbstractElement
  public <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
    return v.visitPackage(this, p);
  }

  @Override // PackageElement
  public final NoType asType() {
    return (NoType)super.asType();
  }

  @Override // PackageElement
  public final Name getQualifiedName() {
    return super.getSimpleName();
  }

  @Override // PackageElement
  public final DefaultName getSimpleName() {
    return this.simpleName;
  }

  final void addEnclosedElement(DefaultTypeElement t) {
    if (this.enclosedTypeNames.add(t.getQualifiedName())) {
      super.addEnclosedElement0(t);
    }
  }

  @Override // PackageElement
  public final ModuleElement getEnclosingElement() {
    return (ModuleElement)super.getEnclosingElement();
  }

  @Override // AbstractElement
  public final void setEnclosingElement(final Element moduleElement) {
    super.setEnclosingElement(validate(moduleElement));
  }

  @Override // PackageElement
  public final boolean isUnnamed() {
    return this.getSimpleName().length() <= 0;
  }


  /*
   * Static methods.
   */


  private static final ModuleElement validate(final Element enclosingElement) {
    if (enclosingElement == null) {
      return null;
    }
    switch (enclosingElement.getKind()) {
    case MODULE:
      return (ModuleElement)enclosingElement;
    default:
      throw new IllegalArgumentException("enclosingElement: " + enclosingElement);
    }
  }

  public static final DefaultPackageElement of(final PackageElement p) {
    if (p instanceof DefaultPackageElement dpe) {
      return dpe;
    } else {
      return
        DefaultPackageElement.of(AnnotatedName.of(p.getAnnotationMirrors(),
                                                  p.getQualifiedName()),
                                 (NoType)p.asType());
    }
  }

  public static final DefaultPackageElement of(final Package p) {
    return DefaultPackageElement.of(DefaultName.of(p.getName()), null);
  }

  public static final DefaultPackageElement of(final Name fullyQualifiedName) {
    return DefaultPackageElement.of(fullyQualifiedName, null);
  }

  public static final DefaultPackageElement of(final Name fullyQualifiedName, final List<? extends AnnotationMirror> annotationMirrors) {
    return DefaultPackageElement.of(AnnotatedName.of(annotationMirrors, fullyQualifiedName));
  }

  public static final DefaultPackageElement of(final AnnotatedName name) {
    return DefaultPackageElement.of(name, null);
  }

  public static final DefaultPackageElement of(final AnnotatedName name, final NoType packageType) {
    return cache.computeIfAbsent(name, n -> new DefaultPackageElement(n, packageType));
  }

}
