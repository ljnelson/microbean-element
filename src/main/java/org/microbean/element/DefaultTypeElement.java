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

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import java.util.function.Supplier;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public class DefaultTypeElement extends AbstractElement implements TypeElement {

  // TODO: clean these up

  public static final Set<Modifier> PUBLIC = EnumSet.of(Modifier.PUBLIC);

  public static final DefaultTypeElement JAVA_LANG_OBJECT =
    new DefaultTypeElement(DefaultName.of("java.lang.Object"),
                           DefaultDeclaredType.JAVA_LANG_OBJECT,
                           PUBLIC);
  
  public static final DefaultTypeElement JAVA_IO_SERIALIZABLE =
    new DefaultTypeElement(DefaultName.of("java.io.Serializable"),
                           ElementKind.INTERFACE,
                           DefaultDeclaredType.JAVA_IO_SERIALIZABLE,
                           PUBLIC);

  public static final DefaultTypeElement JAVA_LANG_CLONEABLE =
    new DefaultTypeElement(DefaultName.of("java.lang.Cloneable"),
                           ElementKind.INTERFACE,
                           DefaultDeclaredType.JAVA_LANG_CLONEABLE,
                           PUBLIC);

  public static final DefaultTypeElement JAVA_LANG_CONSTANT_CONSTABLE =
    new DefaultTypeElement(DefaultName.of("java.lang.constant.Constable"),
                           ElementKind.INTERFACE,
                           DefaultDeclaredType.JAVA_LANG_CONSTANT_CONSTABLE,
                           PUBLIC);
  
  public static final DefaultTypeElement JAVA_LANG_BOOLEAN =
    new DefaultTypeElement(DefaultName.of("java.lang.Boolean"),
                           DefaultDeclaredType.JAVA_LANG_BOOLEAN,
                           PUBLIC,
                           DefaultDeclaredType.JAVA_LANG_OBJECT,
                           List.of(DefaultDeclaredType.JAVA_IO_SERIALIZABLE,
                                   // TODO: Comparable<Boolean>
                                   DefaultDeclaredType.JAVA_LANG_CONSTANT_CONSTABLE));

  public static final DefaultTypeElement JAVA_LANG_NUMBER =
    new DefaultTypeElement(DefaultName.of("java.lang.Number"),
                           DefaultDeclaredType.JAVA_LANG_NUMBER,
                           PUBLIC);
  
  public static final DefaultTypeElement JAVA_LANG_BYTE =
    new DefaultTypeElement(DefaultName.of("java.lang.Byte"),
                           DefaultDeclaredType.JAVA_LANG_BYTE,
                           PUBLIC,
                           DefaultDeclaredType.JAVA_LANG_NUMBER,
                           List.of(DefaultDeclaredType.JAVA_IO_SERIALIZABLE,
                                   // TODO: Comparable<Byte>
                                   DefaultDeclaredType.JAVA_LANG_CONSTANT_CONSTABLE));

  public static final DefaultTypeElement JAVA_LANG_CHARACTER =
    new DefaultTypeElement(DefaultName.of("java.lang.Character"),
                           DefaultDeclaredType.JAVA_LANG_CHARACTER,
                           PUBLIC,
                           DefaultDeclaredType.JAVA_LANG_NUMBER,
                           List.of(DefaultDeclaredType.JAVA_IO_SERIALIZABLE,
                                   // TODO: Comparable<Character>
                                   DefaultDeclaredType.JAVA_LANG_CONSTANT_CONSTABLE));

  public static final DefaultTypeElement JAVA_LANG_DOUBLE =
    new DefaultTypeElement(DefaultName.of("java.lang.Double"),
                           DefaultDeclaredType.JAVA_LANG_DOUBLE,
                           PUBLIC,
                           DefaultDeclaredType.JAVA_LANG_NUMBER,
                           List.of(DefaultDeclaredType.JAVA_IO_SERIALIZABLE,
                                   // TODO: Comparable<Double>
                                   DefaultDeclaredType.JAVA_LANG_CONSTANT_CONSTABLE));

  public static final DefaultTypeElement JAVA_LANG_FLOAT =
    new DefaultTypeElement(DefaultName.of("java.lang.Float"),
                           DefaultDeclaredType.JAVA_LANG_FLOAT,
                           PUBLIC,
                           DefaultDeclaredType.JAVA_LANG_NUMBER,
                           List.of(DefaultDeclaredType.JAVA_IO_SERIALIZABLE,
                                   // TODO: Comparable<Float>
                                   DefaultDeclaredType.JAVA_LANG_CONSTANT_CONSTABLE));

  public static final DefaultTypeElement JAVA_LANG_INTEGER =
    new DefaultTypeElement(DefaultName.of("java.lang.Integer"),
                           DefaultDeclaredType.JAVA_LANG_INTEGER,
                           PUBLIC,
                           DefaultDeclaredType.JAVA_LANG_NUMBER,
                           List.of(DefaultDeclaredType.JAVA_IO_SERIALIZABLE,
                                   // TODO: Comparable<Integer>
                                   DefaultDeclaredType.JAVA_LANG_CONSTANT_CONSTABLE));

  public static final DefaultTypeElement JAVA_LANG_LONG =
    new DefaultTypeElement(DefaultName.of("java.lang.Long"),
                           DefaultDeclaredType.JAVA_LANG_LONG,
                           PUBLIC,
                           DefaultDeclaredType.JAVA_LANG_NUMBER,
                           List.of(DefaultDeclaredType.JAVA_IO_SERIALIZABLE,
                                   // TODO: Comparable<Long>
                                   DefaultDeclaredType.JAVA_LANG_CONSTANT_CONSTABLE));

  public static final DefaultTypeElement JAVA_LANG_SHORT =
    new DefaultTypeElement(DefaultName.of("java.lang.Short"),
                           DefaultDeclaredType.JAVA_LANG_SHORT,
                           PUBLIC,
                           DefaultDeclaredType.JAVA_LANG_NUMBER,
                           List.of(DefaultDeclaredType.JAVA_IO_SERIALIZABLE,
                                   // TODO: Comparable<Short>
                                   DefaultDeclaredType.JAVA_LANG_CONSTANT_CONSTABLE));

  public static final DefaultTypeElement JAVA_LANG_VOID =
    new DefaultTypeElement(DefaultName.of("java.lang.Void"),
                           DefaultDeclaredType.JAVA_LANG_VOID,
                           PUBLIC,
                           DefaultDeclaredType.JAVA_LANG_OBJECT);

  static {
    assert JAVA_LANG_OBJECT.asType() == DefaultDeclaredType.JAVA_LANG_OBJECT;
    assert DefaultDeclaredType.JAVA_LANG_OBJECT.asElement() == JAVA_LANG_OBJECT;
  }

  private final DefaultName simpleName;

  private final NestingKind nestingKind;

  private final List<? extends TypeParameterElement> typeParameters;

  private final TypeMirror superclass;

  private final List<? extends TypeMirror> interfaces;

  private final List<? extends TypeMirror> permittedSubclasses;

  private final List<? extends RecordComponentElement> recordComponents;

  @Deprecated // you can use this, but you better know what you're doing.
  DefaultTypeElement() {
    this(DefaultName.EMPTY, ElementKind.CLASS, new DefaultDeclaredType(), Set.of());
  }

  @Deprecated // you can use this, but you better know what you're doing.
  DefaultTypeElement(final Name qualifiedName) {
    this(qualifiedName, ElementKind.CLASS, new DefaultDeclaredType(), Set.of());
  }

  DefaultTypeElement(final Name qualifiedName,
                     final TypeMirror type,
                     final Set<? extends Modifier> modifiers) {
    this(qualifiedName, ElementKind.CLASS, type, modifiers);
  }

  DefaultTypeElement(final Name qualifiedName,
                     final TypeMirror type,
                     final Set<? extends Modifier> modifiers,
                     final TypeMirror superclass) {
    this(qualifiedName,
         ElementKind.CLASS,
         type,
         modifiers,
         null,
         null,
         List.of(),
         superclass,
         List.of(),
         List.of(),
         List.of(),
         List.of());
  }
  
  DefaultTypeElement(final Name qualifiedName,
                     final TypeMirror type,
                     final Set<? extends Modifier> modifiers,
                     final TypeMirror superclass,
                     final List<? extends TypeMirror> interfaces) {
    this(qualifiedName,
         ElementKind.CLASS,
         type,
         modifiers,
         null,
         null,
         List.of(),
         superclass,
         List.of(),
         interfaces,
         List.of(),
         List.of());
  }

  DefaultTypeElement(final Name qualifiedName,
                     final ElementKind kind,
                     final TypeMirror type,
                     final Set<? extends Modifier> modifiers) {
    this(qualifiedName,
         kind,
         type,
         modifiers,
         null,
         null,
         List.of(),
         null,
         List.of(),
         List.of(),
         List.of(),
         List.of());
  }

  public DefaultTypeElement(final Name qualifiedName,
                            final ElementKind kind,
                            final TypeMirror type,
                            final Set<? extends Modifier> modifiers,
                            final AbstractElement enclosingElement,
                            final NestingKind nestingKind,
                            final List<? extends TypeParameterElement> typeParameters,
                            final TypeMirror superclass,
                            final List<? extends TypeMirror> permittedSubclasses,
                            final List<? extends TypeMirror> interfaces,
                            final List<? extends RecordComponentElement> recordComponents,
                            final List<? extends AnnotationMirror> annotationMirrors) {
    super(qualifiedName,
          validate(kind),
          type,
          modifiers,
          enclosingElement,
          annotationMirrors);
    this.simpleName = DefaultName.ofSimple(qualifiedName);
    this.nestingKind = nestingKind == null ? NestingKind.TOP_LEVEL : nestingKind;
    this.typeParameters = typeParameters == null || typeParameters.isEmpty() ? List.of() : List.copyOf(typeParameters);
    this.superclass = superclass == null ? DefaultNoType.NONE : superclass;
    this.interfaces = interfaces == null || interfaces.isEmpty() ? List.of() : List.copyOf(interfaces);
    this.permittedSubclasses = permittedSubclasses == null || permittedSubclasses.isEmpty() ? List.of() : List.copyOf(permittedSubclasses);
    this.recordComponents = recordComponents == null || recordComponents.isEmpty() ? List.of() : List.copyOf(recordComponents);
    if (type instanceof DefaultDeclaredType ddt) {
      ddt.element(this);
    }
  }

  @Override // TypeElement
  public <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
    return v.visitType(this, p);
  }

  @Override // TypeElement
  public List<? extends TypeParameterElement> getTypeParameters() {
    return this.typeParameters;
  }

  @Override // TypeElement
  public TypeMirror getSuperclass() {
    return this.superclass;
  }

  @Override // TypeElement
  public List<? extends TypeMirror> getInterfaces() {
    return this.interfaces;
  }

  @Override // TypeElement
  public List<? extends RecordComponentElement> getRecordComponents() {
    return this.recordComponents;
  }

  @Override // TypeElement
  public List<? extends TypeMirror> getPermittedSubclasses() {
    return this.permittedSubclasses;
  }

  @Override // TypeElement
  public final DefaultName getSimpleName() {
    return this.simpleName;
  }

  @Override // TypeElement
  public final DefaultName getQualifiedName() {
    return super.getSimpleName();
  }

  @Override // TypeElement
  public final NestingKind getNestingKind() {
    return this.nestingKind;
  }

  private static final ElementKind validate(final ElementKind kind) {
    switch (kind) {
    case ANNOTATION_TYPE:
    case CLASS:
    case ENUM:
    case INTERFACE:
    case RECORD:
      return kind;
    default:
      throw new IllegalArgumentException("Not a type element kind: " + kind);
    }
  }

}
