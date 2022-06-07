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
import java.util.Set;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;

import javax.lang.model.type.PrimitiveType;

final class PrimitiveElement extends AbstractElement {

  static final PrimitiveElement BOOLEAN = new PrimitiveElement(DefaultName.of("boolean"), DefaultPrimitiveType.BOOLEAN);

  static final PrimitiveElement BYTE = new PrimitiveElement(DefaultName.of("byte"), DefaultPrimitiveType.BYTE);

  static final PrimitiveElement CHAR = new PrimitiveElement(DefaultName.of("char"), DefaultPrimitiveType.CHAR);

  static final PrimitiveElement DOUBLE = new PrimitiveElement(DefaultName.of("double"), DefaultPrimitiveType.DOUBLE);
  
  static final PrimitiveElement FLOAT = new PrimitiveElement(DefaultName.of("float"), DefaultPrimitiveType.FLOAT);
  
  static final PrimitiveElement INT = new PrimitiveElement(DefaultName.of("int"), DefaultPrimitiveType.INT);

  static final PrimitiveElement LONG = new PrimitiveElement(DefaultName.of("long"), DefaultPrimitiveType.LONG);

  static final PrimitiveElement SHORT = new PrimitiveElement(DefaultName.of("short"), DefaultPrimitiveType.SHORT);
  
  private PrimitiveElement(final Name name, final PrimitiveType type) {
    super(name,
          ElementKind.OTHER, // not really right, but this whole thing isn't really right, because javac isn't really right
          validate(type),
          Set.of(),
          null, // enclosing element
          List.of());
  }

  private static final PrimitiveType validate(final PrimitiveType t) {
    if (t.getKind().isPrimitive()) {
      return t;
    }
    throw new IllegalArgumentException("t: " + t);
  }
  
}
