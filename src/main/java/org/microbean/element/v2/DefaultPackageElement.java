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
package org.microbean.element.v2;

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

public final class DefaultPackageElement extends AbstractElement implements PackageElement {


  /*
   * Instance fields.
   */


  private final DefaultName simpleName;


  /*
   * Constructors.
   */


  public DefaultPackageElement() {
    this(AnnotatedName.of(DefaultName.of("")), DefaultNoType.PACKAGE, List.of());
  }

  public DefaultPackageElement(final AnnotatedName fullyQualifiedName) {
    this(fullyQualifiedName, DefaultNoType.PACKAGE, List.of());
  }

  public DefaultPackageElement(final AnnotatedName fullyQualifiedName, final NoType packageType) {
    this(fullyQualifiedName, packageType, List.of());
  }

  public
    <T extends TypeElement & Encloseable>
    DefaultPackageElement(final AnnotatedName fullyQualifiedName,
                          final NoType packageType,
                          final List<? extends T> enclosedElements) {
    super(fullyQualifiedName,
          ElementKind.PACKAGE,
          validateType(packageType),
          Set.of(),
          null,
          enclosedElements);
    this.simpleName = DefaultName.ofSimple(fullyQualifiedName.getName());
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

  @Override // PackageElement
  public final ModuleElement getEnclosingElement() {
    return (ModuleElement)super.getEnclosingElement();
  }

  @Override // PackageElement
  public final boolean isUnnamed() {
    return this.getSimpleName().length() <= 0;
  }


  /*
   * Static methods.
   */


  static final NoType validateType(final NoType type) {
    switch (type.getKind()) {
    case PACKAGE:
      return type;
    default:
      throw new IllegalArgumentException("type: " + type);
    }
  }

  public static final DefaultPackageElement of(final PackageElement e) {
    if (e instanceof DefaultPackageElement defaultPackageElement) {
      return defaultPackageElement;
    }
    return
      new DefaultPackageElement(AnnotatedName.of(e.getAnnotationMirrors(), e.getQualifiedName()),
                                (NoType)e.asType(),
                                DefaultTypeElement.encloseableTypeElementsOf(e.getEnclosedElements()));
  }
  

}
