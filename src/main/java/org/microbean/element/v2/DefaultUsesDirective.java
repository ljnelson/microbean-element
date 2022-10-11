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

import javax.lang.model.element.ModuleElement.DirectiveKind;
import javax.lang.model.element.ModuleElement.DirectiveVisitor;
import javax.lang.model.element.ModuleElement.UsesDirective;
import javax.lang.model.element.TypeElement;

public final class DefaultUsesDirective extends AbstractDirective implements UsesDirective {

  private final TypeElement service;
  
  public DefaultUsesDirective(final TypeElement service) {
    super(DirectiveKind.USES);
    this.service = Objects.requireNonNull(service, "service");
  }

  @Override // Directive
  public final <R, P> R accept(final DirectiveVisitor<R, P> v, final P p) {
    return v.visitUses(this, p);
  }

  @Override // UsesDirective
  public final TypeElement getService() {
    return this.service;
  }
  
}
