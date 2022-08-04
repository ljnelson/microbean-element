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

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeMirror;

public final class ArrayTypeBuilder extends AbstractAnnotatedConstructBuilder<ArrayType, ArrayTypeBuilder> {

  private TypeMirror componentType;
  
  public ArrayTypeBuilder(final TypeMirror componentType) {
    super();
    this.componentType = validateComponentType(componentType);
  }

  public final TypeMirror componentType() {
    return this.componentType;
  }

  public final ArrayTypeBuilder withComponentType(final TypeMirror componentType) {
    this.componentType = validateComponentType(componentType);
    return self();
  }

  @Override // AbstractBuilder
  public final ArrayType build() {
    return DefaultArrayType.of(this.componentType(), this.annotations());
  }

  public static final TypeMirror validateComponentType(final TypeMirror componentType) {
    switch (componentType.getKind()) {
    case ERROR:
    case EXECUTABLE:
    case MODULE:
    case NONE:
    case NULL:
    case OTHER:
    case PACKAGE:
    case UNION:
      throw new IllegalArgumentException("componentType: " + componentType);
    default:
      return componentType;
    }
  }
  
}
