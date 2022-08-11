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
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import javax.lang.model.util.ElementFilter;

public class DefaultTypeElement extends AbstractParameterizableElement implements TypeElement {


  /*
   * Instance fields.
   */


  private final DefaultName simpleName;

  private final NestingKind nestingKind;

  private final TypeMirror superclass;

  private final List<? extends TypeMirror> interfaces;

  private final List<? extends TypeMirror> permittedSubclasses;


  /*
   * Constructors.
   */


  public
    <T extends Element & Encloseable, P extends TypeParameterElement & Encloseable>
    DefaultTypeElement(final AnnotatedName qualifiedName,
                       final ElementKind kind,
                       final TypeMirror type,
                       final Set<? extends Modifier> modifiers,
                       final NestingKind nestingKind,
                       final TypeMirror superclass,
                       final List<? extends TypeMirror> permittedSubclasses,
                       final List<? extends TypeMirror> interfaces,
                       final Element enclosingElement,
                       final List<? extends T> enclosedElements,
                       final List<? extends P> typeParameters) {
    super(qualifiedName,
          validateKind(kind),
          validateType(type),
          modifiers,
          enclosingElement,
          enclosedElements,
          typeParameters);
    this.simpleName = DefaultName.ofSimple(qualifiedName.getName());
    this.nestingKind = nestingKind == null ? NestingKind.TOP_LEVEL : nestingKind;
    this.superclass = superclass == null ? DefaultNoType.NONE : superclass;
    this.interfaces = interfaces == null || interfaces.isEmpty() ? List.of() : List.copyOf(interfaces);
    if (!modifiers.contains(Modifier.SEALED) && permittedSubclasses != null && !permittedSubclasses.isEmpty()) {
      throw new IllegalArgumentException("permittedSubclasses: " + permittedSubclasses);
    }
    this.permittedSubclasses = permittedSubclasses == null || permittedSubclasses.isEmpty() ? List.of() : List.copyOf(permittedSubclasses);
    if (type instanceof DefaultDeclaredType ddt) {
      ddt.definingElement(this);
    }
  }


  /*
   * Instance methods.
   */


  @Override // TypeElement
  public <R, P> R accept(final ElementVisitor<R, P> v, final P p) {
    return v.visitType(this, p);
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
    switch (this.getKind()) {
    case RECORD:
      return ElementFilter.recordComponentsIn(this.getEnclosedElements());
    default:
      throw new IllegalStateException();
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


  /*
   * Static methods.
   */


  private static final ElementKind validateKind(final ElementKind kind) {
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

  private static final TypeMirror validateType(final TypeMirror type) {
    switch (type.getKind()) {
    case DECLARED:
      return type;
    default:
      throw new IllegalArgumentException("type: " + type);
    }
  }

  public static final DefaultTypeElement of(final TypeElement t) {
    if (t instanceof DefaultTypeElement defaultTypeElement) {
      return defaultTypeElement;
    }
    return
      new DefaultTypeElement(AnnotatedName.of(t.getAnnotationMirrors(), t.getSimpleName()),
                             t.getKind(),
                             DefaultExecutableType.of((ExecutableType)t.asType()),
                             t.getModifiers(),
                             t.getNestingKind(),
                             t.getSuperclass(),
                             t.getPermittedSubclasses(),
                             t.getInterfaces(),
                             t.getEnclosingElement(),
                             AbstractElement.encloseablesOf(t.getEnclosedElements()),
                             DefaultTypeParameterElement.encloseableTypeParametersOf(t.getTypeParameters()));
  }

  @SuppressWarnings("unchecked")
  public static final <E extends TypeElement & Encloseable> List<? extends E> encloseableTypeElementsOf(final List<? extends Element> list) {
    if (list == null || list.isEmpty()) {
      return List.of();
    }
    final List<E> newList = new ArrayList<>(list.size());
    for (final Element e : list) {
      if (e instanceof TypeElement te) {
        if (te instanceof Encloseable enc) {
          newList.add((E)enc);
        } else {
          newList.add((E)DefaultTypeElement.of(te));
        }
      } else {
        throw new IllegalArgumentException("list: " + list);
      }
    }
    return Collections.unmodifiableList(newList);
  }

}
