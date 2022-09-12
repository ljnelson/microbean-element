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
import java.util.Objects;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.TypeMirror;

public final class DefaultAnnotationValue implements AnnotationValue {

  private final Object value;

  public DefaultAnnotationValue(final Object value) {
    super();
    this.value = Objects.requireNonNull(value, "value");
  }

  @Override // AnnotationValue
  @SuppressWarnings("unchecked")
  public <R, P> R accept(final AnnotationValueVisitor<R, P> v, final P p) {
    final Object value = this.getValue();
    if (value instanceof String s) {
      return v.visitString(s, p);
    } else if (value instanceof Boolean b) {
      return v.visitBoolean(b, p);
    } else if (value instanceof Integer i) {
      return v.visitInt(i, p);
    } else if (value instanceof VariableElement e) {
      return v.visitEnumConstant(e, p);
    } else if (value instanceof AnnotationValue a) {
      return v.visit(a, p);
    } else if (value instanceof AnnotationMirror a) {
      return v.visitAnnotation(a, p);
    } else if (value instanceof List<?> list) {
      return v.visitArray((List<? extends AnnotationValue>)list, p);
    } else if (value instanceof TypeMirror t) {
      return v.visitType(t, p);
    } else if (value instanceof Byte b) {
      return v.visitByte(b, p);
    } else if (value instanceof Character c) {
      return v.visitChar(c, p);
    } else if (value instanceof Double d) {
      return v.visitDouble(d, p);
    } else if (value instanceof Float f) {
      return v.visitFloat(f, p);
    } else if (value instanceof Long l) {
      return v.visitLong(l, p);
    } else if (value instanceof Short s) {
      return v.visitShort(s, p);
    } else {
      return v.visitUnknown(this, p);
    }
  }

  @Override // AnnotationValue
  public final Object getValue() {
    return this.value;
  }

  @Override // Object
  public int hashCode() {
    return Equality.hashCode(this, true);
  }

  @Override // Object
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    } else if (other instanceof AnnotationValue her) { // instanceof is on purpose
      return Equality.equals(this, her, true);
    } else {
      return false;
    }
  }

  public static final DefaultAnnotationValue of(final Object value) {
    return new DefaultAnnotationValue(value);
  }

}
