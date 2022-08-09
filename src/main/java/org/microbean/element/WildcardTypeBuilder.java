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

import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;

public final class WildcardTypeBuilder
  extends AbstractAnnotatedConstructBuilder<WildcardType, WildcardTypeBuilder> {

  private TypeMirror extendsBound;

  private TypeMirror superBound;

  WildcardTypeBuilder() {
    super();
  }

  public final TypeMirror extendsBound() {
    return this.extendsBound;
  }

  public final WildcardTypeBuilder withExtendsBound(final TypeMirror extendsBound) {
    this.extendsBound = validateExtendsBound(extendsBound);
    return self();
  }

  public final TypeMirror superBound() {
    return this.superBound;
  }

  public final WildcardTypeBuilder withSuperBound(final TypeMirror superBound) {
    this.superBound = validateSuperBound(superBound);
    return self();
  }

  public final WildcardTypeBuilder unbounded() {
    return this.withExtendsBound(null).withSuperBound(null);
  }

  @Override // AbstractBuilder
  public final WildcardType build() {
    return
      DefaultWildcardType.of(this.extendsBound(), this.superBound(), this.annotations());
  }

  private static final TypeMirror validateExtendsBound(final TypeMirror extendsBound) {
    if (extendsBound == null) {
      return null;
    }
    switch (extendsBound.getKind()) {
    // See
    // https://docs.oracle.com/javase/specs/jls/se18/html/jls-4.html#jls-WildcardBounds
    // and
    // https://docs.oracle.com/javase/specs/jls/se18/html/jls-4.html#jls-ReferenceType
    case ARRAY:
    case DECLARED:
    case TYPEVAR:
      return extendsBound;
    default:
      throw new IllegalArgumentException("extendsBound: " + extendsBound);
    }
  }

  private static final TypeMirror validateSuperBound(final TypeMirror superBound) {
    if (superBound == null) {
      return null;
    }
    switch (superBound.getKind()) {
      // See
      // https://docs.oracle.com/javase/specs/jls/se18/html/jls-4.html#jls-WildcardBounds
      // and
      // https://docs.oracle.com/javase/specs/jls/se18/html/jls-4.html#jls-ReferenceType
      case ARRAY:
      case DECLARED:
      case TYPEVAR:
        return superBound;
      default:
        throw new IllegalArgumentException("superBound: " + superBound);
    }
  }
  
}
