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

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public abstract class AbstractTypeMirrorBuilder<T extends TypeMirror, B extends AbstractTypeMirrorBuilder<T, B>>
  extends AbstractBuilder<T, B> implements TypeMirrorBuilder<T, B> {

  private TypeKind kind;
  
  protected AbstractTypeMirrorBuilder() {
    super();
    this.kind = TypeKind.NONE;
  }

  public final TypeKind kind() {
    return this.kind;
  }
  
  public B withKind(final TypeKind kind) {
    this.kind = Objects.requireNonNull(kind);
    return self();
  }
  
}
