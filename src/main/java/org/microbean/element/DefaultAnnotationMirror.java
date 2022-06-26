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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;

import javax.lang.model.type.DeclaredType;

public class DefaultAnnotationMirror implements AnnotationMirror {

  private final DefaultDeclaredType annotationType;

  private final Map<? extends DefaultExecutableElement, ? extends DefaultAnnotationValue> elementValues;
  
  public DefaultAnnotationMirror(final DeclaredType annotationType,
                                 final Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues) {
    super();
    switch (annotationType.asElement().getKind()) {
    case ANNOTATION_TYPE:
      this.annotationType = DefaultDeclaredType.of(annotationType);
      break;
    default:
      throw new IllegalArgumentException("annotationType: " + annotationType);
    }
    if (elementValues == null || elementValues.isEmpty()) {
      this.elementValues = Map.of();
    } else {
      final Map<DefaultExecutableElement, DefaultAnnotationValue> map = new HashMap<>();
      for (final Entry<? extends ExecutableElement, ? extends AnnotationValue> e : elementValues.entrySet()) {
        map.put(DefaultExecutableElement.of(e.getKey()), DefaultAnnotationValue.of(e.getValue()));
      }
      this.elementValues = Collections.unmodifiableMap(map);
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
    return Equality.hashCode(this, true);
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    } else if (other instanceof AnnotationMirror her) { // instanceof is on purpose
      return Equality.equals(this, her, true);
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
