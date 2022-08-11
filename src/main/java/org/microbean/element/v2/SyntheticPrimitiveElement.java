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
import java.util.Set;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;

import javax.lang.model.type.PrimitiveType;

final class SyntheticPrimitiveElement extends AbstractElement {


  /*
   * Static fields.
   */


  static final SyntheticPrimitiveElement BOOLEAN = new SyntheticPrimitiveElement(DefaultName.of("boolean"), DefaultPrimitiveType.BOOLEAN);

  static final SyntheticPrimitiveElement BYTE = new SyntheticPrimitiveElement(DefaultName.of("byte"), DefaultPrimitiveType.BYTE);

  static final SyntheticPrimitiveElement CHAR = new SyntheticPrimitiveElement(DefaultName.of("char"), DefaultPrimitiveType.CHAR);

  static final SyntheticPrimitiveElement DOUBLE = new SyntheticPrimitiveElement(DefaultName.of("double"), DefaultPrimitiveType.DOUBLE);

  static final SyntheticPrimitiveElement FLOAT = new SyntheticPrimitiveElement(DefaultName.of("float"), DefaultPrimitiveType.FLOAT);

  static final SyntheticPrimitiveElement INT = new SyntheticPrimitiveElement(DefaultName.of("int"), DefaultPrimitiveType.INT);

  static final SyntheticPrimitiveElement LONG = new SyntheticPrimitiveElement(DefaultName.of("long"), DefaultPrimitiveType.LONG);

  static final SyntheticPrimitiveElement SHORT = new SyntheticPrimitiveElement(DefaultName.of("short"), DefaultPrimitiveType.SHORT);


  /*
   * Constructors.
   */


  private SyntheticPrimitiveElement(final Name name, final PrimitiveType type) {
    super(AnnotatedName.of(name),
          ElementKind.OTHER, // not really right, but this whole thing isn't really right, because javac isn't really right
          validateType(type),
          Set.of(),
          null,
          List.of());
  }


  /*
   * Static methods.
   */


  private static final PrimitiveType validateType(final PrimitiveType t) {
    if (!t.getKind().isPrimitive()) {
      throw new IllegalArgumentException("t: " + t);
    }
    return t;
  }

}
