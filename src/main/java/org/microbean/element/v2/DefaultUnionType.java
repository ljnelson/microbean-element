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

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;

public final class DefaultUnionType extends AbstractTypeMirror implements UnionType {

  // I don't even know if this is legal.
  public static final DefaultUnionType EMPTY = new DefaultUnionType(List.of());
  
  private final List<? extends TypeMirror> alternatives;

  protected DefaultUnionType(final List<? extends TypeMirror> alternatives) {
    super(TypeKind.UNION, List.of());
    this.alternatives = alternatives == null || alternatives.isEmpty() ? List.of() : List.copyOf(alternatives);
  }

  @Override // TypeMirror
  public <R, P> R accept(final TypeVisitor<R, P> v, P p) {
    return v.visitUnion(this, p);
  }
  
  @Override
  public final List<? extends TypeMirror> getAlternatives() {
    return this.alternatives;
  }


  /*
   * Static methods.
   */
  

  public static DefaultUnionType of() {
    return EMPTY;
  }

  public static DefaultUnionType of(final TypeMirror alternative) {
    return of(List.of(alternative));
  }

  public static DefaultUnionType of(final List<? extends TypeMirror> alternatives) {
    return new DefaultUnionType(alternatives);
  }
  
}
