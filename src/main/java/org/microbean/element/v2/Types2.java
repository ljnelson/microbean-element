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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public final class Types2 {

  public Types2() {
    super();
  }

  // Not visitor-based in javac
  private static final List<? extends TypeMirror> allTypeArguments(final TypeMirror t) {
    switch (t.getKind()) {
    case ARRAY:
      return allTypeArguments(((ArrayType)t).getComponentType()); // RECURSIVE
    case DECLARED:
      return allTypeArguments((DeclaredType)t);
    case INTERSECTION:
      // Verified; see
      // https://github.com/openjdk/jdk/blob/jdk-19+25/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Type.java#L1265
    default:
      return List.of();
    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t: " + t);
    }
  }

  private static final List<? extends TypeMirror> allTypeArguments(final DeclaredType t) {
    assert t.getKind() == TypeKind.DECLARED;
    final List<? extends TypeMirror> enclosingTypeTypeArguments = allTypeArguments(t.getEnclosingType());
    final List<? extends TypeMirror> typeArguments = t.getTypeArguments();
    if (enclosingTypeTypeArguments.isEmpty()) {
      return typeArguments;
    } else if (typeArguments.isEmpty()) {
      return enclosingTypeTypeArguments;
    } else {
      final List<TypeMirror> list = new ArrayList<>(enclosingTypeTypeArguments.size() + typeArguments.size());
      list.addAll(enclosingTypeTypeArguments);
      list.addAll(typeArguments);
      return Collections.unmodifiableList(list);
    }
  }
  
}
