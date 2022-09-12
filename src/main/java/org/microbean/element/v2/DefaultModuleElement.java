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

public final class DefaultModuleElement extends AbstractElement implements ModuleElement {


  /*
   * Instance fields.
   */


  private final DefaultName simpleName;

  private final boolean open;

  private final List<Directive> directives;


  /*
   * Constructors.
   */


  public DefaultModuleElement(final AnnotatedName fullyQualifiedName,
                              final boolean open,
                              final List<? extends Directive> directives) {
    super(fullyQualifiedName,
          ElementKind.MODULE,
          DefaultNoType.MODULE,
          Set.of(),
          null, // enclosingElement
          List.of());
    this.simpleName = DefaultName.ofSimple(fullyQualifiedName.getName());
    this.open = open;
    this.directives = directives == null || directives.isEmpty() ? List.of() : List.copyOf(directives);
  }


  /*
   * Instance methods.
   */


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

  
  public static final DefaultModuleElement of(final ModuleElement e) {
    if (e instanceof DefaultModuleElement defaultModuleElement) {
      return defaultModuleElement;
    }
    return new DefaultModuleElement(AnnotatedName.of(e.getAnnotationMirrors(), e.getSimpleName()),
                                    e.isOpen(),
                                    e.getDirectives());
  }
  
}
