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

  // java.base ModuleElement

  static final DefaultModuleElement JAVA_BASE_ELEMENT =
    new DefaultModuleElement(DefaultName.of("java.base"),
                             List.of(),
                             false, // not open
                             List.of()); // enclosedElements/directives; we don't bother to supply these here

  // java.lang PackageElement

  static final DefaultPackageElement JAVA_LANG_ELEMENT =
    new DefaultPackageElement(DefaultName.of("java.lang"),
                              List.of(),
                              DefaultNoType.PACKAGE,
                              JAVA_BASE_ELEMENT,
                              List.of());

  // java.lang.constant PackageElement

  static final DefaultPackageElement JAVA_LANG_CONSTANT_ELEMENT =
    new DefaultPackageElement(DefaultName.of("java.lang.constant"),
                              List.of(),
                              DefaultNoType.PACKAGE,
                              JAVA_BASE_ELEMENT,
                              List.of());


  // java.lang.invoke PackageElement

  static final DefaultPackageElement JAVA_LANG_INVOKE_ELEMENT =
    new DefaultPackageElement(DefaultName.of("java.lang.invoke"),
                              List.of(),
                              DefaultNoType.PACKAGE,
                              JAVA_BASE_ELEMENT,
                              List.of());

  // java.lang.Object

  static final DefaultDeclaredType JAVA_LANG_OBJECT_TYPE = new DefaultDeclaredType();

  static final DefaultTypeElement JAVA_LANG_OBJECT_ELEMENT =
    new DefaultTypeElement(DefaultName.of("java.lang.Object"),
                           List.of(),
                           ElementKind.CLASS,
                           JAVA_LANG_OBJECT_TYPE,
                           Set.of(Modifier.PUBLIC),
                           NestingKind.TOP_LEVEL,
                           null, // superclass
                           List.of(), // permitted subclasses
                           List.of(), // interfaces
                           JAVA_LANG_ELEMENT, // enclosingElement
                           List.of(), // enclosedElements; we don't bother to supply these here
                           List.of()); // typeParameters

  // java.io.Serializable

  static final DefaultDeclaredType JAVA_IO_SERIALIZABLE_TYPE = new DefaultDeclaredType(null, null, null);

  static final DefaultTypeElement JAVA_IO_SERIALIZABLE_ELEMENT =
    new DefaultTypeElement(DefaultName.of("java.io.Serializable"),
                           List.of(),
                           ElementKind.INTERFACE,
                           JAVA_IO_SERIALIZABLE_TYPE,
                           Set.of(Modifier.PUBLIC, Modifier.ABSTRACT),
                           NestingKind.TOP_LEVEL,
                           null, // superclass
                           List.of(),
                           List.of(),
                           JAVA_LANG_ELEMENT, // enclosingElement
                           List.of(),
                           List.of());

  // java.lang.Cloneable

  static final DefaultDeclaredType JAVA_LANG_CLONEABLE_TYPE = new DefaultDeclaredType(null, null, null);

  static final DefaultTypeElement JAVA_LANG_CLONEABLE_ELEMENT =
    new DefaultTypeElement(DefaultName.of("java.lang.Cloneable"),
                           List.of(),
                           ElementKind.INTERFACE,
                           JAVA_LANG_CLONEABLE_TYPE,
                           Set.of(Modifier.PUBLIC, Modifier.ABSTRACT),
                           NestingKind.TOP_LEVEL,
                           null, // superclass
                           List.of(), // permitted subclasses
                           List.of(), // interfaces
                           JAVA_LANG_ELEMENT, // enclosingElement
                           List.of(), // enclosedElements; we don't bother to supply these here
                           List.of()); // typeParameters

  // java.lang.Comparable<T>

  static final DefaultDeclaredType JAVA_LANG_COMPARABLE_TYPE =
    new DefaultDeclaredType(List.of(new DefaultTypeVariable(JAVA_LANG_OBJECT_TYPE)));

  static final DefaultTypeElement JAVA_LANG_COMPARABLE_ELEMENT =
    new DefaultTypeElement(DefaultName.of("java.lang.Comparable"),
                           List.of(),
                           ElementKind.INTERFACE,
                           JAVA_LANG_COMPARABLE_TYPE,
                           Set.of(Modifier.PUBLIC, Modifier.ABSTRACT),
                           NestingKind.TOP_LEVEL,
                           null, // superclass
                           List.of(),
                           List.of(),
                           JAVA_LANG_ELEMENT,
                           List.of(),
                           List.of(new DefaultTypeParameterElement(DefaultName.of("T"),
                                                                   List.of(),
                                                                   (DefaultTypeVariable)JAVA_LANG_COMPARABLE_TYPE.getTypeArguments().get(0))));

  // java.lang.constant.Constable

  static final DefaultDeclaredType JAVA_LANG_CONSTANT_CONSTABLE_TYPE = new DefaultDeclaredType();

  static final DefaultTypeElement JAVA_LANG_CONSTANT_CONSTABLE_ELEMENT =
    new DefaultTypeElement(DefaultName.of("java.lang.constant.Constable"),
                           List.of(),
                           ElementKind.INTERFACE,
                           JAVA_LANG_CONSTANT_CONSTABLE_TYPE,
                           Set.of(Modifier.PUBLIC, Modifier.ABSTRACT),
                           NestingKind.TOP_LEVEL,
                           null, // superclass
                           List.of(),
                           List.of(),
                           JAVA_LANG_CONSTANT_ELEMENT,
                           List.of(),
                           List.of());

  // java.lang.invoke.TypeDescriptor

  static final DefaultDeclaredType JAVA_LANG_INVOKE_TYPEDESCRIPTOR_TYPE = new DefaultDeclaredType();

  static final DefaultTypeElement JAVA_LANG_INVOKE_TYPEDESCRIPTOR_ELEMENT =
    new DefaultTypeElement(DefaultName.of("java.lang.invoke.TypeDescriptor"),
                           List.of(),
                           ElementKind.INTERFACE,
                           JAVA_LANG_INVOKE_TYPEDESCRIPTOR_TYPE,
                           Set.of(Modifier.PUBLIC, Modifier.ABSTRACT),
                           NestingKind.TOP_LEVEL,
                           null, // superclass
                           List.of(),
                           List.of(),
                           JAVA_LANG_INVOKE_ELEMENT,
                           List.of(),
                           List.of());

  // java.lang.invoke.TypeDescriptor.OfField

  static final DefaultDeclaredType JAVA_LANG_INVOKE_TYPEDESCRIPTOR_OFFIELD_TYPE;
  static {
    // Self-referential type arguments.
    final DefaultDeclaredType t = new DefaultDeclaredType(JAVA_LANG_INVOKE_TYPEDESCRIPTOR_TYPE);
    JAVA_LANG_INVOKE_TYPEDESCRIPTOR_OFFIELD_TYPE = t.withTypeArguments(List.of(new DefaultTypeVariable(t)));
  }

  static final DefaultTypeElement JAVA_LANG_INVOKE_TYPEDESCRIPTOR_OFFIELD_ELEMENT =
    new DefaultTypeElement(DefaultName.of("java.lang.invoke.TypeDescriptor.OfField"),
                           List.of(),
                           ElementKind.INTERFACE,
                           JAVA_LANG_INVOKE_TYPEDESCRIPTOR_OFFIELD_TYPE,
                           Set.of(Modifier.PUBLIC, Modifier.STATIC, Modifier.ABSTRACT),
                           NestingKind.MEMBER,
                           null,
                           List.of(),
                           List.of(JAVA_LANG_INVOKE_TYPEDESCRIPTOR_TYPE), // interfaces
                           JAVA_LANG_INVOKE_TYPEDESCRIPTOR_ELEMENT,
                           List.of(),
                           List.of(new DefaultTypeParameterElement(DefaultName.of("F"),
                                                                   List.of(),
                                                                   (DefaultTypeVariable)JAVA_LANG_INVOKE_TYPEDESCRIPTOR_OFFIELD_TYPE.getTypeArguments().get(0))));

  // java.lang.invoke.TypeDescriptor.OfMethod

  static final DefaultDeclaredType JAVA_LANG_INVOKE_TYPEDESCRIPTOR_OFMETHOD_TYPE;
  static {
    // Self-referential type arguments.
    final DefaultDeclaredType t = new DefaultDeclaredType(JAVA_LANG_INVOKE_TYPEDESCRIPTOR_TYPE);
    JAVA_LANG_INVOKE_TYPEDESCRIPTOR_OFMETHOD_TYPE = t.withTypeArguments(List.of(new DefaultTypeVariable(JAVA_LANG_INVOKE_TYPEDESCRIPTOR_OFFIELD_TYPE),
                                                                                new DefaultTypeVariable(t)));
  }

  static final DefaultTypeElement JAVA_LANG_INVOKE_TYPEDESCRIPTOR_OFMETHOD_ELEMENT =
    new DefaultTypeElement(DefaultName.of("java.lang.invoke.TypeDescriptor.OfMethod"),
                           List.of(),
                           ElementKind.INTERFACE,
                           JAVA_LANG_INVOKE_TYPEDESCRIPTOR_OFMETHOD_TYPE,
                           Set.of(Modifier.PUBLIC, Modifier.STATIC, Modifier.ABSTRACT),
                           NestingKind.MEMBER,
                           null, // supertype
                           List.of(),
                           List.of(JAVA_LANG_INVOKE_TYPEDESCRIPTOR_TYPE), // interfaces
                           JAVA_LANG_INVOKE_TYPEDESCRIPTOR_ELEMENT,
                           List.of(),
                           List.of(new DefaultTypeParameterElement(DefaultName.of("F"),
                                                                   List.of(),
                                                                   (DefaultTypeVariable)JAVA_LANG_INVOKE_TYPEDESCRIPTOR_OFMETHOD_TYPE.getTypeArguments().get(0)),
                                   new DefaultTypeParameterElement(DefaultName.of("M"),
                                                                   List.of(),
                                                                   (DefaultTypeVariable)JAVA_LANG_INVOKE_TYPEDESCRIPTOR_OFMETHOD_TYPE.getTypeArguments().get(1))));

  // Working our way toward defining ConstantDesc, which is sealed
  // with permitted subtypes, so we have to list those first, and they
  // extend the TypeDescriptor stuff above.

  static final DefaultDeclaredType JAVA_LANG_CONSTANT_CONSTANTDESC_TYPE = new DefaultDeclaredType();

  static final DefaultDeclaredType JAVA_LANG_CONSTANT_CLASSDESC_TYPE = new DefaultDeclaredType();

  static final DefaultDeclaredType JAVA_LANG_CONSTANT_DIRECTMETHODHANDLEDESC_TYPE = new DefaultDeclaredType();

  static final DefaultDeclaredType JAVA_LANG_CONSTANT_DYNAMICCONSTANTDESC_TYPE =
    new DefaultDeclaredType(List.of(new DefaultTypeVariable(JAVA_LANG_OBJECT_TYPE)));

  static final DefaultDeclaredType JAVA_LANG_CONSTANT_METHODHANDLEDESC_TYPE = new DefaultDeclaredType();

  static final DefaultDeclaredType JAVA_LANG_CONSTANT_METHODTYPEDESC_TYPE = new DefaultDeclaredType();

  static final DefaultDeclaredType JAVA_LANG_DOUBLE_TYPE = new DefaultDeclaredType();

  static final DefaultDeclaredType JAVA_LANG_FLOAT_TYPE = new DefaultDeclaredType();

  static final DefaultDeclaredType JAVA_LANG_INTEGER_TYPE = new DefaultDeclaredType();

  static final DefaultDeclaredType JAVA_LANG_LONG_TYPE = new DefaultDeclaredType();

  static final DefaultDeclaredType JAVA_LANG_STRING_TYPE = new DefaultDeclaredType();

  // Whew! Now we can make the java.lang.constant.ConstantDesc element!

  // java.lang.constant.ConstantDesc

  static final DefaultTypeElement JAVA_LANG_CONSTANT_CONSTANTDESC_ELEMENT =
    new DefaultTypeElement(DefaultName.of("java.lang.constant.ConstantDesc"),
                           List.of(),
                           ElementKind.INTERFACE,
                           JAVA_LANG_CONSTANT_CONSTANTDESC_TYPE,
                           Set.of(Modifier.PUBLIC, Modifier.ABSTRACT, Modifier.SEALED),
                           NestingKind.TOP_LEVEL,
                           null, // superclass
                           List.of(JAVA_LANG_CONSTANT_CLASSDESC_TYPE,
                                   JAVA_LANG_CONSTANT_METHODHANDLEDESC_TYPE,
                                   JAVA_LANG_CONSTANT_METHODTYPEDESC_TYPE,
                                   JAVA_LANG_DOUBLE_TYPE,
                                   JAVA_LANG_CONSTANT_DYNAMICCONSTANTDESC_TYPE,
                                   JAVA_LANG_FLOAT_TYPE,
                                   JAVA_LANG_INTEGER_TYPE,
                                   JAVA_LANG_LONG_TYPE,
                                   JAVA_LANG_STRING_TYPE),
                           List.of(), // interfaces
                           JAVA_LANG_CONSTANT_ELEMENT,
                           List.of(),
                           List.of());

  // Now the permitted sub-elements, etc.

  // java.lang.constant.ClassDesc

  static final DefaultTypeElement JAVA_LANG_CONSTANT_CLASSDESC_ELEMENT =
    new DefaultTypeElement(DefaultName.of("java.lang.constant.ClassDesc"),
                           List.of(),
                           ElementKind.INTERFACE,
                           JAVA_LANG_CONSTANT_CLASSDESC_TYPE,
                           Set.of(Modifier.PUBLIC, Modifier.ABSTRACT, Modifier.SEALED),
                           NestingKind.TOP_LEVEL,
                           null,
                           List.of(),
                           List.of(JAVA_LANG_CONSTANT_CONSTANTDESC_TYPE,
                                   new DefaultDeclaredType(JAVA_LANG_INVOKE_TYPEDESCRIPTOR_OFFIELD_ELEMENT,
                                                           List.of(JAVA_LANG_CONSTANT_CONSTANTDESC_TYPE))),
                           JAVA_LANG_CONSTANT_ELEMENT,
                           List.of(),
                           List.of());

  // java.lang.constant.MethodHandleDesc

  static final DefaultTypeElement JAVA_LANG_CONSTANT_METHODHANDLEDESC_ELEMENT =
    new DefaultTypeElement(DefaultName.of("java.lang.constant.MethodHandleDesc"),
                           List.of(),
                           ElementKind.INTERFACE,
                           JAVA_LANG_CONSTANT_METHODHANDLEDESC_TYPE,
                           Set.of(Modifier.PUBLIC, Modifier.ABSTRACT, Modifier.SEALED),
                           NestingKind.TOP_LEVEL,
                           null,
                           List.of(JAVA_LANG_CONSTANT_DIRECTMETHODHANDLEDESC_TYPE),
                           List.of(JAVA_LANG_CONSTANT_CONSTANTDESC_TYPE),
                           JAVA_LANG_CONSTANT_ELEMENT,
                           List.of(),
                           List.of());

  // java.lang.constant.MethodTypeDesc

  static final DefaultTypeElement JAVA_LANG_CONSTANT_METHODTYPEDESC_ELEMENT =
    new DefaultTypeElement(DefaultName.of("java.lang.constant.MethodTypeDesc"),
                           List.of(),
                           ElementKind.INTERFACE,
                           JAVA_LANG_CONSTANT_METHODTYPEDESC_TYPE,
                           Set.of(Modifier.PUBLIC, Modifier.ABSTRACT, Modifier.SEALED),
                           NestingKind.TOP_LEVEL,
                           null,
                           List.of(),
                           List.of(JAVA_LANG_CONSTANT_CONSTANTDESC_TYPE,
                                   new DefaultDeclaredType(JAVA_LANG_INVOKE_TYPEDESCRIPTOR_OFMETHOD_TYPE,
                                                           List.of(JAVA_LANG_CONSTANT_CLASSDESC_TYPE,
                                                                   JAVA_LANG_CONSTANT_METHODTYPEDESC_TYPE))),
                           JAVA_LANG_CONSTANT_ELEMENT,
                           List.of(),
                           List.of());

  // java.lang.constant.DirectMethodHandleDesc

  static final DefaultTypeElement JAVA_LANG_CONSTANT_DIRECTMETHODHANDLEDESC_ELEMENT =
    new DefaultTypeElement(DefaultName.of("java.lang.constant.DirectMethodHandleDesc"),
                           List.of(),
                           ElementKind.INTERFACE,
                           JAVA_LANG_CONSTANT_DIRECTMETHODHANDLEDESC_TYPE,
                           Set.of(Modifier.PUBLIC, Modifier.ABSTRACT, Modifier.SEALED),
                           NestingKind.TOP_LEVEL,
                           null,
                           List.of(),
                           List.of(JAVA_LANG_CONSTANT_METHODHANDLEDESC_TYPE),
                           JAVA_LANG_CONSTANT_ELEMENT,
                           List.of(),
                           List.of());

  // java.lang.constant.DynamicConstantDesc

  static final DefaultTypeElement JAVA_LANG_CONSTANT_DYNAMICCONSTANTDESC_ELEMENT =
    new DefaultTypeElement(DefaultName.of("java.lang.constant.DynamicConstantDesc"),
                           List.of(),
                           ElementKind.INTERFACE,
                           JAVA_LANG_CONSTANT_DYNAMICCONSTANTDESC_TYPE,
                           Set.of(Modifier.PUBLIC, Modifier.ABSTRACT, Modifier.SEALED),
                           NestingKind.TOP_LEVEL,
                           null,
                           List.of(),
                           List.of(JAVA_LANG_CONSTANT_CONSTANTDESC_TYPE),
                           JAVA_LANG_CONSTANT_ELEMENT,
                           List.of(),
                           List.of(new DefaultTypeParameterElement(DefaultName.of("T"),
                                                                   List.of(),
                                                                   (DefaultTypeVariable)JAVA_LANG_CONSTANT_DYNAMICCONSTANTDESC_TYPE
                                                                     .getTypeArguments()
                                                                     .get(0))));

  // java.lang.Number

  static final DefaultDeclaredType JAVA_LANG_NUMBER_TYPE = new DefaultDeclaredType();

  static final DefaultTypeElement JAVA_LANG_NUMBER_ELEMENT =
    new DefaultTypeElement(DefaultName.of("java.lang.Number"),
                           List.of(),
                           ElementKind.CLASS,
                           JAVA_LANG_NUMBER_TYPE,
                           Set.of(Modifier.PUBLIC, Modifier.ABSTRACT),
                           NestingKind.TOP_LEVEL,
                           JAVA_LANG_OBJECT_TYPE,
                           List.of(),
                           List.of(JAVA_IO_SERIALIZABLE_TYPE),
                           JAVA_LANG_ELEMENT,
                           List.of(),
                           List.of());

  // java.lang.Byte

  static final DefaultDeclaredType JAVA_LANG_BYTE_TYPE = new DefaultDeclaredType();

  static final DefaultTypeElement JAVA_LANG_BYTE_ELEMENT =
    new DefaultTypeElement(DefaultName.of("java.lang.Byte"),
                           List.of(),
                           ElementKind.CLASS,
                           JAVA_LANG_BYTE_TYPE,
                           Set.of(Modifier.PUBLIC, Modifier.FINAL),
                           NestingKind.TOP_LEVEL,
                           JAVA_LANG_NUMBER_TYPE,
                           List.of(),
                           List.of(new DefaultDeclaredType(JAVA_LANG_COMPARABLE_ELEMENT, List.of(JAVA_LANG_BYTE_TYPE)),
                                   JAVA_LANG_CONSTANT_CONSTABLE_TYPE),
                           JAVA_LANG_ELEMENT,
                           List.of(),
                           List.of());

  // java.lang.Character

  static final DefaultDeclaredType JAVA_LANG_CHARACTER_TYPE = new DefaultDeclaredType();

  static final DefaultTypeElement JAVA_LANG_CHARACTER_ELEMENT =
    new DefaultTypeElement(DefaultName.of("java.lang.Character"),
                           List.of(),
                           ElementKind.CLASS,
                           JAVA_LANG_CHARACTER_TYPE,
                           Set.of(Modifier.PUBLIC, Modifier.FINAL),
                           NestingKind.TOP_LEVEL,
                           JAVA_LANG_OBJECT_TYPE,
                           List.of(),
                           List.of(JAVA_IO_SERIALIZABLE_TYPE,
                                   new DefaultDeclaredType(JAVA_LANG_COMPARABLE_ELEMENT, List.of(JAVA_LANG_CHARACTER_TYPE)),
                                   JAVA_LANG_CONSTANT_CONSTABLE_TYPE),
                           JAVA_LANG_ELEMENT,
                           List.of(),
                           List.of());

  // java.lang.Double

  static final DefaultTypeElement JAVA_LANG_DOUBLE_ELEMENT =
    new DefaultTypeElement(DefaultName.of("java.lang.Double"),
                           List.of(),
                           ElementKind.CLASS,
                           JAVA_LANG_DOUBLE_TYPE,
                           Set.of(Modifier.PUBLIC, Modifier.FINAL),
                           NestingKind.TOP_LEVEL,
                           JAVA_LANG_NUMBER_TYPE,
                           List.of(),
                           List.of(new DefaultDeclaredType(JAVA_LANG_COMPARABLE_ELEMENT, List.of(JAVA_LANG_DOUBLE_TYPE)),
                                   JAVA_LANG_CONSTANT_CONSTABLE_TYPE,
                                   JAVA_LANG_CONSTANT_CONSTANTDESC_TYPE),
                           JAVA_LANG_ELEMENT,
                           List.of(),
                           List.of());

  // java.lang.Float

  static final DefaultTypeElement JAVA_LANG_FLOAT_ELEMENT =
    new DefaultTypeElement(DefaultName.of("java.lang.Float"),
                           List.of(),
                           ElementKind.CLASS,
                           JAVA_LANG_FLOAT_TYPE,
                           Set.of(Modifier.PUBLIC, Modifier.FINAL),
                           NestingKind.TOP_LEVEL,
                           JAVA_LANG_NUMBER_TYPE,
                           List.of(),
                           List.of(new DefaultDeclaredType(JAVA_LANG_COMPARABLE_ELEMENT, List.of(JAVA_LANG_FLOAT_TYPE)),
                                   JAVA_LANG_CONSTANT_CONSTABLE_TYPE,
                                   JAVA_LANG_CONSTANT_CONSTANTDESC_TYPE),
                           JAVA_LANG_ELEMENT,
                           List.of(),
                           List.of());

  // java.lang.Integer

  static final DefaultTypeElement JAVA_LANG_INTEGER_ELEMENT =
    new DefaultTypeElement(DefaultName.of("java.lang.Integer"),
                           List.of(),
                           ElementKind.CLASS,
                           JAVA_LANG_INTEGER_TYPE,
                           Set.of(Modifier.PUBLIC, Modifier.FINAL),
                           NestingKind.TOP_LEVEL,
                           JAVA_LANG_NUMBER_TYPE,
                           List.of(),
                           List.of(new DefaultDeclaredType(JAVA_LANG_COMPARABLE_ELEMENT, List.of(JAVA_LANG_INTEGER_TYPE)),
                                   JAVA_LANG_CONSTANT_CONSTABLE_TYPE,
                                   JAVA_LANG_CONSTANT_CONSTANTDESC_TYPE),
                           JAVA_LANG_ELEMENT,
                           List.of(),
                           List.of());

  // java.lang.Long

  static final DefaultTypeElement JAVA_LANG_LONG_ELEMENT =
    new DefaultTypeElement(DefaultName.of("java.lang.Long"),
                           List.of(),
                           ElementKind.CLASS,
                           JAVA_LANG_LONG_TYPE,
                           Set.of(Modifier.PUBLIC, Modifier.FINAL),
                           NestingKind.TOP_LEVEL,
                           JAVA_LANG_NUMBER_TYPE,
                           List.of(),
                           List.of(new DefaultDeclaredType(JAVA_LANG_COMPARABLE_ELEMENT, List.of(JAVA_LANG_LONG_TYPE)),
                                   JAVA_LANG_CONSTANT_CONSTABLE_TYPE,
                                   JAVA_LANG_CONSTANT_CONSTANTDESC_TYPE),
                           JAVA_LANG_ELEMENT,
                           List.of(),
                           List.of());

  // java.lang.Short

  static final DefaultDeclaredType JAVA_LANG_SHORT_TYPE = new DefaultDeclaredType();

  static final DefaultTypeElement JAVA_LANG_SHORT_ELEMENT =
    new DefaultTypeElement(DefaultName.of("java.lang.Short"),
                           List.of(),
                           ElementKind.CLASS,
                           JAVA_LANG_SHORT_TYPE,
                           Set.of(Modifier.PUBLIC, Modifier.FINAL),
                           NestingKind.TOP_LEVEL,
                           JAVA_LANG_NUMBER_TYPE,
                           List.of(),
                           List.of(new DefaultDeclaredType(JAVA_LANG_COMPARABLE_ELEMENT, List.of(JAVA_LANG_SHORT_TYPE)),
                                   JAVA_LANG_CONSTANT_CONSTABLE_TYPE),
                           JAVA_LANG_ELEMENT,
                           List.of(),
                           List.of());

  // java.lang.Void

  static final DefaultTypeElement JAVA_LANG_VOID_ELEMENT =
    new DefaultTypeElement(DefaultName.of("java.lang.Void"),
                           List.of(),
                           ElementKind.CLASS,
                           new DefaultDeclaredType(),
                           Set.of(Modifier.PUBLIC, Modifier.FINAL),
                           NestingKind.TOP_LEVEL,
                           JAVA_LANG_OBJECT_TYPE,
                           List.of(),
                           List.of(),
                           JAVA_LANG_ELEMENT,
                           List.of(),
                           List.of());

}
