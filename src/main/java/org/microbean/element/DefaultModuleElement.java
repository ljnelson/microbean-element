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
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.Directive;
import javax.lang.model.element.Name;

import javax.lang.model.type.NoType;

public class DefaultModuleElement extends AbstractElement implements ModuleElement {

  private final DefaultName simpleName;

  private final boolean open;

  private final List<? extends Directive> directives;
  
  public DefaultModuleElement(final Name fullyQualifiedName,
                              final NoType moduleType,
                              final Set<? extends Modifier> modifiers,
                              final boolean open,
                              final List<? extends Directive> directives,
                              final List<? extends AnnotationMirror> annotationMirrors) {
    super(fullyQualifiedName,
          ElementKind.MODULE,
          moduleType == null ? DefaultNoType.MODULE : moduleType,
          modifiers,
          null,
          annotationMirrors);
    this.simpleName = DefaultName.ofSimple(fullyQualifiedName);
    this.open = open;
    this.directives = directives == null || directives.isEmpty() ? List.of() : List.copyOf(directives);
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

  @Override // ModuleElement
  public final List<? extends Directive> getDirectives() {
    return this.directives;
  }
                              
  @Override // ModuleElement
  public final DefaultName getQualifiedName() {
    return super.getSimpleName();
  }

  @Override // ModuleElement
  public final DefaultName getSimpleName() {
    return this.simpleName;
  }

  @Override // ModuleElement
  public final AbstractElement getEnclosingElement() {
    return null;
  }

  @Override // ModuleElement
  public final boolean isUnnamed() {
    return this.getSimpleName().length() <= 0;
  }
  
}
