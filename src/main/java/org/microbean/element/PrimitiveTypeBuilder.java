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

import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;

public final class PrimitiveTypeBuilder
  extends AbstractAnnotatedConstructBuilder<PrimitiveType, PrimitiveTypeBuilder>
  implements TypeMirrorBuilder<PrimitiveType, PrimitiveTypeBuilder> {

  private TypeKind kind;
  
  public PrimitiveTypeBuilder(final TypeKind kind) {
    super();
    this.kind = validateTypeKind(kind);
  }

  @Override // TypeMirrorBuilder
  public final TypeKind kind() {
    return this.kind;
  }

  @Override // TypeMirrorBuilder
  public final PrimitiveTypeBuilder withKind(final TypeKind kind) {
    this.kind = validateTypeKind(kind);
    return self();
  }

  @Override // AbstractBuilder
  public final PrimitiveType build() {
    if (!this.annotations().isEmpty()) {
      return new DefaultPrimitiveType(this.kind(), this.annotations());
    }
    switch (this.kind()) {
    case BOOLEAN:
      return DefaultPrimitiveType.BOOLEAN;
    case BYTE:
      return DefaultPrimitiveType.BYTE;
    case CHAR:
      return DefaultPrimitiveType.CHAR;
    case DOUBLE:
      return DefaultPrimitiveType.DOUBLE;
    case FLOAT:
      return DefaultPrimitiveType.FLOAT;
    case INT:
      return DefaultPrimitiveType.INT;
    case LONG:
      return DefaultPrimitiveType.LONG;
    case SHORT:
      return DefaultPrimitiveType.SHORT;
    default:
      throw new AssertionError();
    }
  }

  public static final TypeKind validateTypeKind(final TypeKind kind) {
    if (!kind.isPrimitive()) {
      throw new IllegalArgumentException("kind: " + kind);
    }
    return kind;
  }
  
}
