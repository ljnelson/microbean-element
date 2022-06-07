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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.lang.model.element.AnnotationMirror;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;

public class DefaultIntersectionType extends AbstractTypeMirror implements IntersectionType {

  private final List<? extends TypeMirror> bounds;
  
  protected DefaultIntersectionType(final List<? extends TypeMirror> bounds) {
    super(TypeKind.INTERSECTION, List.of());
    if (bounds.isEmpty()) {
      // (Technically I think we could use Object...)
      throw new IllegalArgumentException("bounds.isEmpty()");
    }    
    final TypeMirror firstBound = bounds.get(0);
    switch (firstBound.getKind()) {
    case DECLARED:
      if (((DeclaredType)firstBound).asElement().getKind().isInterface()) {
        final List<TypeMirror> newBounds = new ArrayList<>(bounds.size() + 1);
        newBounds.add(0, DefaultDeclaredType.JAVA_LANG_OBJECT);
        newBounds.addAll(bounds);
        this.bounds = Collections.unmodifiableList(newBounds);
      } else {
        this.bounds = List.copyOf(bounds);
      }
      break;
    default:
      this.bounds = List.copyOf(bounds);
    }
  }

  @Override // TypeMirror
  public <R, P> R accept(final TypeVisitor<R, P> v, P p) {
    return v.visitIntersection(this, p);
  }
  
  @Override // IntersectionType
  public final List<? extends TypeMirror> getBounds() {
    return this.bounds;
  }

  public static DefaultIntersectionType of(final List<? extends TypeMirror> bounds) {
    return new DefaultIntersectionType(bounds);
  }
  
}
