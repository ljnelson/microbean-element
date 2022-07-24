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

import java.lang.module.ModuleDescriptor;

import java.util.List;
import java.util.Objects;

import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.DirectiveKind;
import javax.lang.model.element.ModuleElement.DirectiveVisitor;
import javax.lang.model.element.ModuleElement.ExportsDirective;
import javax.lang.model.element.PackageElement;

public final class DefaultExportsDirective extends AbstractDirective implements ExportsDirective {

  private final PackageElement pkg;

  private final List<? extends ModuleElement> targetModules;
  
  public DefaultExportsDirective(final PackageElement pkg,
                                 final List<? extends ModuleElement> targetModules) {
    super(DirectiveKind.EXPORTS);
    this.pkg = Objects.requireNonNull(pkg, "pkg");
    this.targetModules = targetModules == null || targetModules.isEmpty() ? List.of() : List.copyOf(targetModules);
  }

  @Override // Directive
  public final <R, P> R accept(final DirectiveVisitor<R, P> v, final P p) {
    return v.visitExports(this, p);
  }

  @Override // ExportsDirective
  public final PackageElement getPackage() {
    return this.pkg;
  }

  @Override // ExportsDirective
  public final List<? extends ModuleElement> getTargetModules() {
    return this.targetModules;
  }

}
