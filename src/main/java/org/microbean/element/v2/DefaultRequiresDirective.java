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

import java.util.Objects;

import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.DirectiveKind;
import javax.lang.model.element.ModuleElement.DirectiveVisitor;
import javax.lang.model.element.ModuleElement.RequiresDirective;

public final class DefaultRequiresDirective extends AbstractDirective implements RequiresDirective {

  private final ModuleElement dependency;

  private final boolean isStatic;

  private final boolean transitive;
  
  public DefaultRequiresDirective(final ModuleElement dependency,
                                  final boolean isStatic,
                                  final boolean transitive) {
    super(DirectiveKind.REQUIRES);
    this.dependency = Objects.requireNonNull(dependency, "dependency");
    this.isStatic = isStatic;
    this.transitive = transitive;
  }

  @Override // Directive
  public final <R, P> R accept(final DirectiveVisitor<R, P> v, final P p) {
    return v.visitRequires(this, p);
  }

  @Override // RequiresDirective
  public final ModuleElement getDependency() {
    return this.dependency;
  }

  @Override // RequiresDirective
  public final boolean isStatic() {
    return this.isStatic;
  }

  @Override // RequiresDirective
  public final boolean isTransitive() {
    return this.transitive;
  }
  
}
