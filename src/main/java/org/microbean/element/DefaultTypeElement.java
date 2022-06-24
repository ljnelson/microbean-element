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

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Executable;
import java.lang.reflect.RecordComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import java.util.concurrent.CopyOnWriteArrayList;

import java.util.function.Supplier;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
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

  // Hazy on all this.
  //
  // I think my? the JDK's? confusion around types and names is
  // because a Java identifier sometimes denotes an Element (in lang
  // model parlance) and sometimes just stands in for a TypeMirror
  // backing that element.
  //
  // So in:
  //
  //   public class Frob implements Comparable<Frob> ...
  //
  // ...the first occurrence of Frob defines the canonical
  // TypeElement, and the second occurrence of Frob (inside the angle
  // brackets) is some text that refers to it.
  //
  // For example, I think that in the following declarations:
  //
  //   public interface java.lang.Comparable<T> ...
  //
  //   public class Frob implements Comparable<Frob> ...
  //
  // ...there are the following things, working from the top down:
  //
  // * an element, T.  This is represented in the lang model as a
  //   TypeParameterElement.  Its getSimpleName() method will return a
  //   Name representing the String "T".  Its getGenericElement() and
  //   its getEnclosingElement() will return the Element it is a type
  //   parameter for.  (We'll get to that next.)  Its asType() method
  //   will return a (definitionally nameless) TypeVariable whose
  //   upper bound is the DeclaredType denoted by a TypeElement, not
  //   present here, whose qualified Name is "java.lang.Object".
  //
  // * a TypeElement named "java.lang.Comparable" that defines a (definitionally nameless) DeclaredType
  //
  // * an element, Frob.  This is fundamentally what is being
  //   declared.  It "has" an associated (definitionally nameless; in
  //   the lang model types don't have names) type "backing" it
  //   featuring zero type arguments, because none were supplied,
  //   because the Frob element declares zero type parameters.

  // * an element, declared elsewhere, Comparable<T>.  (If you were to
  // * call asType() on this element, what would be the value of
  // * type.getTypeArguments()? Probably List.of().)
  
  // * an element, Comparable<Frob>.  It "has" an associated
  // * (definitionally nameless) type "backing" it whose
  // * getTypeArguments() will return exactly one type, namely the
  // * type backing the Frob element.
  // 
  public static final DefaultTypeElement JAVA_LANG_COMPARABLE_BOOLEAN =
    new DefaultTypeElement(DefaultName.of("java.lang.Comparable"),
                           ElementKind.INTERFACE,
                           DefaultDeclaredType.JAVA_LANG_COMPARABLE_BOOLEAN,
                           PUBLIC);
  
  public static final DefaultTypeElement JAVA_LANG_BOOLEAN =
    new DefaultTypeElement(DefaultName.of("java.lang.Boolean"),
                           DefaultDeclaredType.JAVA_LANG_BOOLEAN,
                           PUBLIC,
                           DefaultDeclaredType.JAVA_LANG_OBJECT,
                           List.of(DefaultDeclaredType.JAVA_IO_SERIALIZABLE,
                                   DefaultDeclaredType.JAVA_LANG_COMPARABLE_BOOLEAN,
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

  private final List<TypeParameterElement> mutableTypeParameters;
  
  private final List<? extends TypeParameterElement> typeParameters;

  private final TypeMirror superclass;

  private final List<? extends TypeMirror> interfaces;

  private final List<? extends TypeMirror> permittedSubclasses;

  private final List<RecordComponentElement> mutableRecordComponents;

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
         superclass,
         List.of(),
         List.of(),
         null);
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
         superclass,
         List.of(),
         interfaces,
         null);
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
         null,
         List.of(),
         List.of(),
         null);
  }

  public DefaultTypeElement(final Name qualifiedName,
                            final ElementKind kind,
                            final TypeMirror type,
                            final Set<? extends Modifier> modifiers,
                            final Element enclosingElement,
                            final NestingKind nestingKind,
                            final TypeMirror superclass,
                            final List<? extends TypeMirror> permittedSubclasses,
                            final List<? extends TypeMirror> interfaces,
                            final Supplier<List<? extends AnnotationMirror>> annotationMirrorsSupplier) {
    super(qualifiedName,
          validate(kind),
          type,
          modifiers,
          enclosingElement,
          annotationMirrorsSupplier);
    this.simpleName = DefaultName.ofSimple(qualifiedName);
    this.nestingKind = nestingKind == null ? NestingKind.TOP_LEVEL : nestingKind;
    this.mutableTypeParameters = new CopyOnWriteArrayList<>();
    this.typeParameters = Collections.unmodifiableList(this.mutableTypeParameters);
    this.superclass = superclass == null ? DefaultNoType.NONE : superclass;
    this.interfaces = interfaces == null || interfaces.isEmpty() ? List.of() : List.copyOf(interfaces);
    this.permittedSubclasses = permittedSubclasses == null || permittedSubclasses.isEmpty() ? List.of() : List.copyOf(permittedSubclasses);
    this.mutableRecordComponents = new CopyOnWriteArrayList<>();
    this.recordComponents = Collections.unmodifiableList(this.mutableRecordComponents);
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

  final void addTypeParameter(final TypeParameterElement tp) {
    switch (tp.getKind()) {
    case TYPE_PARAMETER:
      this.mutableTypeParameters.add(tp);
      break;
    default:
      throw new IllegalArgumentException();
    }
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

  final void addRecordComponent(final RecordComponentElement r) {
    switch (r.getKind()) {
    case RECORD_COMPONENT:
      this.mutableRecordComponents.add(r);
      break;
    default:
      throw new IllegalArgumentException();
    }
  }

  @Override // TypeElement
  public List<? extends TypeMirror> getPermittedSubclasses() {
    return this.permittedSubclasses;
  }

  @Override // TypeElement
  public final Name getSimpleName() {
    return this.simpleName;
  }

  @Override // TypeElement
  public final Name getQualifiedName() {
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

  public static final DefaultTypeElement of(final Element enclosingElement, final Class<?> c) {
    if (c == void.class || c.isArray() || c.isPrimitive()) {
      throw new IllegalArgumentException("c: " + c);
    }
    final Name qualifiedName = DefaultName.of(c.getName());
    final ElementKind kind;
    if (c.isAnnotation()) {
      kind = ElementKind.ANNOTATION_TYPE;
    } else if (c.isInterface()) {
      kind = ElementKind.INTERFACE;
    } else if (c.isEnum()) {
      kind = ElementKind.ENUM;
    } else if (c.isRecord()) {
      kind = ElementKind.RECORD;
    } else {
      kind = ElementKind.CLASS;
    }
    final TypeMirror type = DefaultDeclaredType.of(c);
    final Collection<Modifier> modifierSet = new HashSet<>();
    final int modifiers = c.getModifiers();
    if (java.lang.reflect.Modifier.isAbstract(modifiers)) {
      modifierSet.add(Modifier.ABSTRACT);
    }
    if (java.lang.reflect.Modifier.isFinal(modifiers)) {
      modifierSet.add(Modifier.FINAL);
    }
    if (java.lang.reflect.Modifier.isPrivate(modifiers)) {
      modifierSet.add(Modifier.PRIVATE);
    }
    if (java.lang.reflect.Modifier.isProtected(modifiers)) {
      modifierSet.add(Modifier.PROTECTED);
    }
    if (java.lang.reflect.Modifier.isPublic(modifiers)) {
      modifierSet.add(Modifier.PUBLIC);
    }
    if (c.isSealed()) {
      modifierSet.add(Modifier.SEALED);
    }
    if (java.lang.reflect.Modifier.isStatic(modifiers)) {
      modifierSet.add(Modifier.STATIC);
    }
    final EnumSet<Modifier> finalModifiers = EnumSet.copyOf(modifierSet);
    final NestingKind nestingKind;
    if (enclosingElement == null) {
      nestingKind = NestingKind.TOP_LEVEL;
    } else if (c.isAnonymousClass()) {
      nestingKind = NestingKind.ANONYMOUS;
    } else if (c.isLocalClass()) {
      nestingKind = NestingKind.LOCAL;
    } else if (c.isMemberClass()) {
      nestingKind = NestingKind.MEMBER;
    } else {
      throw new AssertionError();
    }
    final AnnotatedType annotatedSuperclass = c.getAnnotatedSuperclass();
    final DeclaredType superclass = annotatedSuperclass == null ? null : DefaultDeclaredType.of(annotatedSuperclass);
    final List<TypeMirror> permittedSubclassTypeMirrors;
    if (c.isSealed()) {
      final Class<?>[] permittedSubclasses = c.getPermittedSubclasses();
      if (permittedSubclasses.length <= 0) {
        permittedSubclassTypeMirrors = List.of();
      } else {
        permittedSubclassTypeMirrors = new ArrayList<>(permittedSubclasses.length);
        for (final Class<?> psc : permittedSubclasses) {
          permittedSubclassTypeMirrors.add(DefaultDeclaredType.of(psc));
        }
      }
    } else {
      permittedSubclassTypeMirrors = List.of();
    }
    final List<TypeMirror> interfaceTypeMirrors;
    final AnnotatedType[] annotatedInterfaces = c.getAnnotatedInterfaces();
    if (annotatedInterfaces.length <= 0) {
      interfaceTypeMirrors = List.of();
    } else {
      interfaceTypeMirrors = new ArrayList<>(annotatedInterfaces.length);
      for (final AnnotatedType t : annotatedInterfaces) {
        interfaceTypeMirrors.add(DefaultDeclaredType.of(t));
      }
    }
    final DefaultTypeElement returnValue =
      new DefaultTypeElement(qualifiedName,
                             kind,
                             type,
                             finalModifiers,
                             enclosingElement,
                             nestingKind,
                             superclass,
                             permittedSubclassTypeMirrors,
                             interfaceTypeMirrors,
                             null);
    final RecordComponent[] recordComponents = c.getRecordComponents();
    if (recordComponents != null) {
      for (final RecordComponent rc : recordComponents) {
        DefaultRecordComponentElement.of(returnValue, rc); // side effect: will get added
      }
    }
    return returnValue;
  }

}
