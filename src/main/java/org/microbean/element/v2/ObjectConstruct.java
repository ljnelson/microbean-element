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
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;

final class ObjectConstruct {

  static final DefaultDeclaredType JAVA_LANG_OBJECT_TYPE = new DefaultDeclaredType(null, null, null);

  // kind of a stub; we don't include enclosed objects like methods and fields and so on
  static final DefaultTypeElement JAVA_LANG_OBJECT_ELEMENT =
    new DefaultTypeElement(AnnotatedName.of(DefaultName.of("java.lang.Object")),
                           ElementKind.CLASS,
                           JAVA_LANG_OBJECT_TYPE,
                           Set.of(Modifier.PUBLIC),
                           NestingKind.TOP_LEVEL,
                           null, // superclass
                           List.of(), // permitted subclasses
                           List.of(), // interfaces
                           null, // enclosingElement; we don't bother to supply the java.lang package
                           List.of(), // enclosedElements; we don't bother to supply these here
                           List.of()); // typeParameters

  static final DefaultDeclaredType JAVA_IO_SERIALIZABLE_TYPE = new DefaultDeclaredType(null, null, null);
  
  public static final DefaultTypeElement JAVA_IO_SERIALIZABLE_ELEMENT =
    new DefaultTypeElement(AnnotatedName.of(DefaultName.of("java.io.Serializable")),
                           ElementKind.INTERFACE,
                           JAVA_IO_SERIALIZABLE_TYPE,
                           Set.of(Modifier.PUBLIC),
                           NestingKind.TOP_LEVEL,
                           null,
                           List.of(),
                           List.of(),
                           null,
                           List.of(),
                           List.of());

  static final DefaultDeclaredType JAVA_LANG_CLONEABLE_TYPE = new DefaultDeclaredType(null, null, null);

  // kind of a stub; we don't include enclosed objects like methods and fields and so on
  public static final DefaultTypeElement JAVA_LANG_CLONEABLE_ELEMENT =
    new DefaultTypeElement(AnnotatedName.of(DefaultName.of("java.lang.Cloneable")),
                           ElementKind.INTERFACE,
                           JAVA_LANG_CLONEABLE_TYPE,
                           Set.of(Modifier.PUBLIC),
                           NestingKind.TOP_LEVEL,
                           null,
                           List.of(),
                           List.of(),
                           null,
                           List.of(),
                           List.of());

}
