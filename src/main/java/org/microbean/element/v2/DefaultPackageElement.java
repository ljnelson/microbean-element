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
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
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

  private final List<? extends AnnotationMirror> annotationMirrors;


  /*
   * Constructors.
   */


  public DefaultPackageElement() {
    this(DefaultName.of(""),
         List.of(),
         DefaultNoType.PACKAGE,
         null,
         List.of());
  }
  
  public DefaultPackageElement(final Name fullyQualifiedName) {
    this(fullyQualifiedName,
         List.of(),
         DefaultNoType.PACKAGE,
         null,
         List.of());
  }

  public DefaultPackageElement(final Name fullyQualifiedName,
                               final List<? extends AnnotationMirror> annotationMirrors) {
    this(fullyQualifiedName,
         annotationMirrors,
         DefaultNoType.PACKAGE,
         null,
         List.of());
  }

  public DefaultPackageElement(final Name fullyQualifiedName,
                               final NoType packageType) {
    this(fullyQualifiedName,
         List.of(),
         packageType,
         null,
         List.of());
  }

  public
    <T extends TypeElement & Encloseable>
    DefaultPackageElement(final Name fullyQualifiedName,
                          final List<? extends AnnotationMirror> annotationMirrors,
                          final NoType packageType,
                          final ModuleElement enclosingElement,
                          final List<? extends T> enclosedElements) {
    super(fullyQualifiedName,
          ElementKind.PACKAGE,
          validateType(packageType),
          Set.of(),
          enclosingElement,
          enclosedElements);
    this.simpleName = DefaultName.ofSimple(fullyQualifiedName);
    if (annotationMirrors == null) {
      this.annotationMirrors = List.of();
    } else if (annotationMirrors instanceof DeferredList<? extends AnnotationMirror>) {
      this.annotationMirrors = annotationMirrors;
    } else {
      this.annotationMirrors = List.copyOf(annotationMirrors);
    }
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

  @Override
  public final List<? extends AnnotationMirror> getAnnotationMirrors() {
    return this.annotationMirrors;
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
      new DefaultPackageElement(e.getQualifiedName(),
                                e.getAnnotationMirrors(),
                                (NoType)e.asType(),
                                (ModuleElement)e.getEnclosingElement(),
                                DefaultTypeElement.encloseableTypeElementsOf(e.getEnclosedElements()));
  }
  

}
