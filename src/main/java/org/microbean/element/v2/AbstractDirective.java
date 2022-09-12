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

import javax.lang.model.element.ModuleElement.Directive;
import javax.lang.model.element.ModuleElement.DirectiveKind;
import javax.lang.model.element.ModuleElement.DirectiveVisitor;
import javax.lang.model.element.ModuleElement.ExportsDirective;
import javax.lang.model.element.ModuleElement.OpensDirective;
import javax.lang.model.element.ModuleElement.ProvidesDirective;
import javax.lang.model.element.ModuleElement.RequiresDirective;
import javax.lang.model.element.ModuleElement.UsesDirective;

public sealed class AbstractDirective implements Directive permits DefaultExportsDirective, DefaultOpensDirective, DefaultProvidesDirective, DefaultRequiresDirective, DefaultUsesDirective {

  private final DirectiveKind kind;
  
  protected AbstractDirective(final DirectiveKind kind) {
    super();
    this.kind = Objects.requireNonNull(kind);
  }

  @Override // Directive
  public <R, P> R accept(final DirectiveVisitor<R, P> v, final P p) {
    switch (this.getKind()) {
    case EXPORTS:
      return v.visitExports((ExportsDirective)this, p);
    case OPENS:
      return v.visitOpens((OpensDirective)this, p);
    case PROVIDES:
      return v.visitProvides((ProvidesDirective)this, p);
    case REQUIRES:
      return v.visitRequires((RequiresDirective)this, p);
    case USES:
      return v.visitUses((UsesDirective)this, p);
    default:
      return v.visitUnknown(this, p);
    }
  }
  
  @Override // Directive
  public final DirectiveKind getKind() {
    return this.kind;
  }  
  
}
