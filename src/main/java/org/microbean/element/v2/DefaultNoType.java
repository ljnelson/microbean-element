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

import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeKind;

public final class DefaultNoType extends AbstractTypeMirror implements NoType {


  /*
   * Static fields.
   */


  public static final DefaultNoType NONE = new DefaultNoType(TypeKind.NONE);

  public static final DefaultNoType MODULE = new DefaultNoType(TypeKind.MODULE);

  public static final DefaultNoType PACKAGE = new DefaultNoType(TypeKind.PACKAGE);

  public static final DefaultNoType VOID = new DefaultNoType(TypeKind.VOID);


  /*
   * Constructors.
   */


  private DefaultNoType(final TypeKind kind) {
    super(validateKind(kind), List.of());
  }


  /*
   * Static methods.
   */


  private static final TypeKind validateKind(final TypeKind kind) {
    switch (kind) {
    case MODULE:
    case NONE:
    case PACKAGE:
    case VOID:
      return kind;
    default:
      throw new IllegalArgumentException("Not a NoType kind: " + kind);
    }
  }

  public static final DefaultNoType of(final NoType t) {
    if (t instanceof DefaultNoType defaultNoType) {
      return defaultNoType;
    }
    switch (t.getKind()) {
    case MODULE:
      return MODULE;
    case NONE:
      return NONE;
    case PACKAGE:
      return PACKAGE;
    case VOID:
      return VOID;
    default:
      throw new IllegalArgumentException("t: " + t);
    }
  }

}
