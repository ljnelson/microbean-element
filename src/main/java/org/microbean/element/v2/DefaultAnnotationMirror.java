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

import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;

import javax.lang.model.type.DeclaredType;

public class DefaultAnnotationMirror implements AnnotationMirror {

  private final DeclaredType annotationType;

  private final Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues;
  
  public DefaultAnnotationMirror(final DeclaredType annotationType,
                                 final Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues) {
    super();
    switch (annotationType.asElement().getKind()) {
    case ANNOTATION_TYPE:
      this.annotationType = annotationType;
      break;
    default:
      throw new IllegalArgumentException("annotationType: " + annotationType);
    }
    if (elementValues == null || elementValues.isEmpty()) {
      this.elementValues = Map.of();
    } else {
      this.elementValues = Map.copyOf(elementValues);
    }
  }

  @Override // AnnotationMirror
  public final DeclaredType getAnnotationType() {
    return this.annotationType;
  }

  @Override // AnnotationMirror
  public final Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValues() {
    return this.elementValues;
  }

  @Override // Object
  public int hashCode() {
    return org.microbean.element.Equality.hashCode(this, true);
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    } else if (other instanceof AnnotationMirror her) { // instanceof is on purpose
      return org.microbean.element.Equality.equals(this, her, true);
    } else {
      return false;
    }
  }

  public static final DefaultAnnotationMirror of(final AnnotationMirror am) {
    if (am instanceof DefaultAnnotationMirror dam) {
      return dam;
    }
    return of(am.getAnnotationType(), am.getElementValues());
  }

  public static final DefaultAnnotationMirror of(final DeclaredType annotationType,
                                                 final Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues) {
    return new DefaultAnnotationMirror(annotationType, elementValues);
  }
  
}
