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

import java.util.function.Supplier;

import javax.lang.model.element.AnnotationMirror;

import javax.lang.model.type.ReferenceType;
import javax.lang.model.type.TypeKind;

public class AbstractReferenceType extends AbstractTypeMirror implements ReferenceType {

  public AbstractReferenceType(final TypeKind kind, final Supplier<List<? extends AnnotationMirror>> annotationMirrorsSupplier) {
    super(validate(kind), annotationMirrorsSupplier);
  }

  private static final TypeKind validate(final TypeKind kind) {
    switch (kind) {
    case ARRAY:
    case DECLARED:
    case NULL:
    case TYPEVAR:
      return kind;
    default:
      throw new IllegalArgumentException("Not a reference kind: " + kind);
    }
  }
  
}
