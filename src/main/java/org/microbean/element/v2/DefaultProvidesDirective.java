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
import java.util.Objects;

import javax.lang.model.element.ModuleElement.DirectiveKind;
import javax.lang.model.element.ModuleElement.DirectiveVisitor;
import javax.lang.model.element.ModuleElement.ProvidesDirective;
import javax.lang.model.element.TypeElement;

public final class DefaultProvidesDirective extends AbstractDirective implements ProvidesDirective {

  private final TypeElement service;

  private final List<? extends TypeElement> implementations;
  
  public DefaultProvidesDirective(final TypeElement service,
                                 final List<? extends TypeElement> implementations) {
    super(DirectiveKind.PROVIDES);
    this.service = Objects.requireNonNull(service, "service");
    this.implementations = implementations == null || implementations.isEmpty() ? List.of() : List.copyOf(implementations);
  }

  @Override // Directive
  public final <R, P> R accept(final DirectiveVisitor<R, P> v, final P p) {
    return v.visitProvides(this, p);
  }

  @Override // ProvidesDirective
  public final TypeElement getService() {
    return this.service;
  }

  @Override // ProvidesDirective
  public final List<? extends TypeElement> getImplementations() {
    return this.implementations;
  }
  
}
