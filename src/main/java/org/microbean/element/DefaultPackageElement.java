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
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.ModuleElement;

import javax.lang.model.type.NoType;

public class DefaultPackageElement extends AbstractElement implements PackageElement {

  private final DefaultName simpleName;

  public DefaultPackageElement(final Name fullyQualifiedName,
                               final NoType packageType,
                               final Set<? extends Modifier> modifiers,
                               final DefaultModuleElement enclosingElement,
                               final List<? extends AnnotationMirror> annotationMirrors) {
    super(fullyQualifiedName,
          ElementKind.PACKAGE,
          packageType,
          modifiers,
          enclosingElement,
          annotationMirrors);
    this.simpleName = DefaultName.ofSimple(fullyQualifiedName);
  }

  @Override // AbstractElement
  public <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
    return v.visitPackage(this, p);
  }
  
  @Override // PackageElement
  public final NoType asType() {
    return (NoType)super.asType();
  }

  @Override // PackageElement
  public final DefaultName getQualifiedName() {
    return super.getSimpleName();
  }

  @Override // PackageElement
  public final DefaultName getSimpleName() {
    return this.simpleName;
  }

  @Override // PackageElement
  public final DefaultModuleElement getEnclosingElement() {
    return (DefaultModuleElement)super.getEnclosingElement();
  }

  @Override // PackageElement
  public final boolean isUnnamed() {
    return this.getSimpleName().length() <= 0;
  }
  
}
