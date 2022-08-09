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

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeMirror;

public final class IntersectionTypeBuilder
  extends AbstractAnnotatedConstructBuilder<IntersectionType, IntersectionTypeBuilder> {

  private List<? extends TypeMirror> bounds;
  
  public IntersectionTypeBuilder() {
    super();
  }

  public final List<? extends TypeMirror> bounds() {
    return this.bounds;
  }

  public final IntersectionTypeBuilder withBounds(final List<? extends TypeMirror> bounds) {
    this.bounds = validateBounds(bounds);
    return self();
  }

  @Override // AbstractBuilder
  public final IntersectionType build() {
    return DefaultIntersectionType.of(this.bounds());
  }

  private static final List<? extends TypeMirror> validateBounds(final List<? extends TypeMirror> bounds) {
    if (bounds == null || bounds.isEmpty()) {
      throw new IllegalArgumentException("bounds: " + bounds);
    }
    final TypeMirror firstBound = bounds.get(0);
    switch (firstBound.getKind()) {
    case DECLARED:
      if (((DeclaredType)firstBound).asElement().getKind().isInterface()) {
        final List<TypeMirror> newBounds = new ArrayList<>(bounds.size() + 1);
        newBounds.add(0, DeclaredTypeBuilder.object());
        newBounds.addAll(bounds);
        return Collections.unmodifiableList(newBounds);
      }
      return List.copyOf(bounds);
    default:
      return List.copyOf(bounds);
    }
  }
  
}
