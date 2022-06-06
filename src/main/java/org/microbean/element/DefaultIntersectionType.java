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
import java.util.Objects;

import javax.lang.model.element.AnnotationMirror;

import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;

public final class DefaultIntersectionType extends AbstractTypeMirror implements IntersectionType {

  private final List<? extends TypeMirror> bounds;
  
  private DefaultIntersectionType(final List<? extends TypeMirror> bounds) {
    super(TypeKind.INTERSECTION, List.of());
    this.bounds = bounds == null || bounds.isEmpty() ? List.of() : List.copyOf(bounds);
  }

  @Override // TypeMirror
  public final <R, P> R accept(final TypeVisitor<R, P> v, P p) {
    return v.visitIntersection(this, p);
  }
  
  @Override // IntersectionType
  public final List<? extends TypeMirror> getBounds() {
    return this.bounds;
  }

  public static final DefaultIntersectionType of(final List<? extends TypeMirror> bounds) {
    return new DefaultIntersectionType(bounds);
  }
  
}
