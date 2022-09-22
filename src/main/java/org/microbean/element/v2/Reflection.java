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

import java.lang.annotation.Annotation;

import java.lang.module.ModuleDescriptor;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReadWriteLock;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

public final class Reflection {

  private final ReadWriteLock typeStubsByClassLock;

  private final Map<Class<?>, AbstractTypeMirror> typeStubsByClass;

  private final ReadWriteLock elementStubsByClassLock;

  private final Map<Class<?>, DefaultTypeElement> elementStubsByClass;

  public Reflection() {
    super();
    this.typeStubsByClassLock = new ReentrantReadWriteLock();
    this.typeStubsByClass = new WeakHashMap<>();
    this.elementStubsByClassLock = new ReentrantReadWriteLock();
    this.elementStubsByClass = new WeakHashMap<>();
  }

  public final DefaultAnnotationMirror annotationMirrorFrom(final Annotation a) throws IllegalAccessException, InvocationTargetException {
    final Class<? extends Annotation> t = a.annotationType();
    final Map<DefaultExecutableElement, DefaultAnnotationValue> values = new HashMap<>();
    final Method[] elements = t.getDeclaredMethods();
    for (final Method element : elements) {
      values.put(elementStubFrom(element), annotationValueFrom(element.invoke(a)));
    }
    return new DefaultAnnotationMirror((DeclaredType)typeStubFrom(t), values);
  }

  public final List<? extends DefaultAnnotationMirror> annotationMirrorsFrom(final AnnotatedElement e) throws IllegalAccessException, InvocationTargetException {
    return annotationMirrorsFrom(e.getDeclaredAnnotations());
  }

  public final List<? extends DefaultAnnotationMirror> annotationMirrorsFrom(final Collection<? extends Annotation> annotations) throws IllegalAccessException, InvocationTargetException {
    if (annotations == null || annotations.isEmpty()) {
      return List.of();
    }
    final List<DefaultAnnotationMirror> list = new ArrayList<>(annotations.size());
    for (final Annotation annotation : annotations) {
      list.add(annotationMirrorFrom(annotation));
    }
    return Collections.unmodifiableList(list);
  }

  public final List<? extends DefaultAnnotationMirror> annotationMirrorsFrom(final Annotation[] annotations) throws IllegalAccessException, InvocationTargetException {
    if (annotations == null || annotations.length <= 0) {
      return List.of();
    }
    final List<DefaultAnnotationMirror> list = new ArrayList<>(annotations.length);
    for (final Annotation annotation : annotations) {
      list.add(annotationMirrorFrom(annotation));
    }
    return Collections.unmodifiableList(list);
  }

  public final List<? extends DefaultAnnotationMirror> annotationMirrorsFrom(final Enum<?> e) throws IllegalAccessException, InvocationTargetException {
    final Field[] fields = e.getDeclaringClass().getDeclaredFields();
    final int ordinal = e.ordinal();
    int i = 0;
    for (final Field field : fields) {
      if (field.isEnumConstant() && i++ == ordinal) {
        return annotationMirrorsFrom(field);
      }
    }
    return List.of();
  }

  public final List<? extends DefaultAnnotationValue> annotationValuesFrom(final Collection<?> c) throws IllegalAccessException, InvocationTargetException {
    if (c.isEmpty()) {
      return List.of();
    }
    final List<DefaultAnnotationValue> list = new ArrayList<>(c.size());
    for (final Object value : c) {
      list.add(annotationValueFrom(value));
    }
    return Collections.unmodifiableList(list);
  }

  public final List<? extends DefaultAnnotationValue> annotationValuesFrom(final Object[] array) throws IllegalAccessException, InvocationTargetException {
    if (array == null || array.length <= 0) {
      return List.of();
    }
    final List<DefaultAnnotationValue> list = new ArrayList<>(array.length);
    for (final Object value : array) {
      list.add(annotationValueFrom(value));
    }
    return Collections.unmodifiableList(list);
  }

  public final DefaultAnnotationValue annotationValueFrom(Object value) throws IllegalAccessException, InvocationTargetException {
    return new DefaultAnnotationValue(switch (value) {
      case String s -> s;
      case Boolean b -> b;
      case Integer i -> i;
      case Enum<?> e -> {
        yield
          new DefaultVariableElement(AnnotatedName.of(annotationMirrorsFrom(e), DefaultName.of(e.name())),
                                     ElementKind.ENUM,
                                     typeStubFrom(e.getDeclaringClass()),
                                     Set.of(), // TODO: modifiers
                                     null, // enclosingElement; not defined by javadocs?!
                                     null); // no constant value // TODO: check
      }
      case Class<?> c -> typeStubFrom(c);
      case Object[] array -> annotationValuesFrom(array);
      case Annotation a -> annotationMirrorFrom(a);
      case Byte b -> b;
      case Character c -> c;
      case Double d -> d;
      case Float f -> f;
      case Long l -> l;
      case Short s -> s;
      // Get the default value:
      case Method m -> m.getDefaultValue() == null ? null : annotationValueFrom(m.getDefaultValue());
      // For completeness:
      case TypeMirror t -> t;
      case VariableElement v -> v;
      case AnnotationMirror a -> a;
      case List<?> list -> annotationValuesFrom(list);
      default -> throw new IllegalArgumentException("value: " + value);
      });
  }

  public final DefaultTypeElement elementStubFrom(final Class<?> c) throws IllegalAccessException, InvocationTargetException {
    return elementStubFrom(c, (DefaultDeclaredType)typeStubFrom(c));
  }

  public final DefaultTypeElement elementStubFrom(final Class<?> c,
                                                  final DefaultDeclaredType type)
    throws IllegalAccessException, InvocationTargetException {
    if (c == null) {
      return null;
    } else if (c == void.class || c.isArray() || c.isPrimitive()) {
      throw new IllegalArgumentException("c: " + c);
    } else if (type.asElement() != null) {
      throw new IllegalArgumentException("type: " + type);
    }

    DefaultTypeElement e = null;

    Lock lock = this.elementStubsByClassLock.readLock();
    lock.lock();
    try {
      e = this.elementStubsByClass.get(c);
    } finally {
      lock.unlock();
    }

    if (e == null) {

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

      final Collection<Modifier> modifierSet = new HashSet<>();
      final int modifiers = c.getModifiers();

      if (java.lang.reflect.Modifier.isAbstract(modifiers)) {
        modifierSet.add(Modifier.ABSTRACT);
      } else if (java.lang.reflect.Modifier.isFinal(modifiers)) {
        modifierSet.add(Modifier.FINAL);
      }

      if (java.lang.reflect.Modifier.isPrivate(modifiers)) {
        modifierSet.add(Modifier.PRIVATE);
      } else if (java.lang.reflect.Modifier.isProtected(modifiers)) {
        modifierSet.add(Modifier.PROTECTED);
      } else if (java.lang.reflect.Modifier.isPublic(modifiers)) {
        modifierSet.add(Modifier.PUBLIC);
      }

      if (c.isSealed()) {
        modifierSet.add(Modifier.SEALED);
      }
      if (java.lang.reflect.Modifier.isStatic(modifiers)) {
        modifierSet.add(Modifier.STATIC);
      }
      final EnumSet<Modifier> finalModifiers = EnumSet.copyOf(modifierSet);

      final Element enclosingElement;
      final NestingKind nestingKind;
      if (c.isAnonymousClass()) {
        nestingKind = NestingKind.ANONYMOUS;
        Executable enclosingObject = c.getEnclosingMethod();
        if (enclosingObject == null) {
          enclosingObject = c.getEnclosingConstructor();
        }
        // Note: we don't want to call c.getEnclosingClass(), because
        // that will jump the hierarchy. An anonymous class is
        // lexically enclosed only by an non-static executable.  (I
        // think. :-) (What about private Gorp gorp = new Gorp()
        // {};?))
        assert enclosingObject != null;
        enclosingElement = elementStubFrom(enclosingObject);
      } else if (c.isLocalClass()) {
        nestingKind = NestingKind.LOCAL;
        Executable enclosingExecutable = c.getEnclosingMethod();
        if (enclosingExecutable == null) {
          enclosingExecutable = c.getEnclosingConstructor();
        }
        assert enclosingExecutable != null;
        enclosingElement = elementStubFrom(enclosingExecutable);
      } else if (c.isMemberClass()) {
        nestingKind = NestingKind.MEMBER;
        enclosingElement = elementStubFrom(c.getDeclaringClass());
      } else {
        nestingKind = NestingKind.TOP_LEVEL;
        enclosingElement = null;
      }

      final List<DeclaredType> permittedSubclassTypeMirrors;
      if (c.isSealed()) {
        final Class<?>[] permittedSubclasses = c.getPermittedSubclasses();
        if (permittedSubclasses.length <= 0) {
          permittedSubclassTypeMirrors = List.of();
        } else {
          permittedSubclassTypeMirrors = new ArrayList<>(permittedSubclasses.length);
          for (final Class<?> psc : permittedSubclasses) {
            permittedSubclassTypeMirrors.add((DeclaredType)typeStubFrom(psc));
          }
        }
      } else {
        permittedSubclassTypeMirrors = List.of();
      }

      final List<DeclaredType> interfaceTypeMirrors;
      final AnnotatedType[] annotatedInterfaces = c.getAnnotatedInterfaces();
      if (annotatedInterfaces.length <= 0) {
        interfaceTypeMirrors = List.of();
      } else {
        interfaceTypeMirrors = new ArrayList<>(annotatedInterfaces.length);
        for (final AnnotatedType t : annotatedInterfaces) {
          interfaceTypeMirrors.add((DeclaredType)typeStubFrom(t));
        }
      }
      
      final List<DefaultTypeParameterElement> typeParameterElements;
      // A Class can be seen as representing both a type and an
      // element.  Viewed as an element, it has type parameters.
      // Viewed as a type, it has type arguments, which are always
      // type variables.  java.lang.reflect.TypeVariable represents
      // both type parameters and type arguments.
      final java.lang.reflect.TypeVariable<?>[] tvs = c.getTypeParameters();
      if (tvs.length <= 0) {
        assert type.getTypeArguments().isEmpty();
        typeParameterElements = List.of();
      } else {
        final List<? extends DefaultTypeVariable> typeArguments = (List<? extends DefaultTypeVariable>)type.getTypeArguments();
        assert typeArguments.size() == tvs.length;
        typeParameterElements = new ArrayList<>(tvs.length);
        for (int i = 0; i < tvs.length; i++) {
          final DefaultTypeVariable dtv = typeArguments.get(i);
          // final java.lang.reflect.TypeVariable<?> typeParameter = tvs[i];
          // assert !dtv.defined();
          // typeParameterElements.add(elementStubFrom(typeParameter, dtv));
          assert dtv.defined();
          typeParameterElements.add(DefaultTypeParameterElement.of(dtv.asElement()));

        }
      }

      e =
        new DefaultTypeElement(AnnotatedName.of(annotationMirrorsFrom(c), DefaultName.of(c.getName())),
                               kind,
                               type,
                               finalModifiers,
                               nestingKind,
                               typeStubFrom(c.getAnnotatedSuperclass()),
                               permittedSubclassTypeMirrors,
                               interfaceTypeMirrors,
                               enclosingElement,
                               List.of(), // enclosedElements; this is a stub; we don't inflate them
                               typeParameterElements);

      lock = this.elementStubsByClassLock.writeLock();
      lock.lock();
      try {
        final DefaultTypeElement e2 = this.elementStubsByClass.get(c);
        if (e2 == null) {
          this.elementStubsByClass.put(c, e);
        } else {
          e = e2;
        }
      } finally {
        lock.unlock();
      }

    }
    return e;
  }

  public final DefaultExecutableElement elementStubFrom(final Executable e) throws IllegalAccessException, InvocationTargetException {
    if (e == null) {
      return null;
    }
    final int modifiers = e.getModifiers();
    final Collection<Modifier> modifierSet = new HashSet<>();
    final ElementKind kind;
    final boolean isDefault;
    final AnnotationValue defaultValue;
    if (e instanceof Method m) {
      // TODO: handle static and instance initializers
      kind = ElementKind.METHOD;
      isDefault = m.isDefault();
      if (isDefault) {
        modifierSet.add(Modifier.DEFAULT);
      } else {
        if (java.lang.reflect.Modifier.isAbstract(modifiers)) {
          modifierSet.add(Modifier.ABSTRACT);
        } else if (java.lang.reflect.Modifier.isFinal(modifiers)) {
          modifierSet.add(Modifier.FINAL);
        } else if (java.lang.reflect.Modifier.isNative(modifiers)) {
          modifierSet.add(Modifier.NATIVE);
        }

        if (java.lang.reflect.Modifier.isStatic(modifiers)) {
          modifierSet.add(Modifier.STATIC);
        }
        if (java.lang.reflect.Modifier.isSynchronized(modifiers)) {
          modifierSet.add(Modifier.SYNCHRONIZED);
        }
      }
      defaultValue = annotationValueFrom(e);
    } else {
      kind = ElementKind.CONSTRUCTOR;
      isDefault = false;
      defaultValue = null;
    }

    if (java.lang.reflect.Modifier.isPrivate(modifiers)) {
      modifierSet.add(Modifier.PRIVATE);
    } else if (java.lang.reflect.Modifier.isProtected(modifiers)) {
      modifierSet.add(Modifier.PROTECTED);
    } else if (java.lang.reflect.Modifier.isPublic(modifiers)) {
      modifierSet.add(Modifier.PUBLIC);
    }

    final List<DefaultTypeParameterElement> typeParameterElements;
    // An Executable can be seen as representing both a type and an
    // element.  Viewed as an element, it has type parameters.
    // Viewed as a type, it has type arguments.
    // java.lang.reflect.TypeVariable represents both.
    final java.lang.reflect.TypeVariable<?>[] tvs = e.getTypeParameters();
    if (tvs.length <= 0) {
      typeParameterElements = List.of();
    } else {
      typeParameterElements = new ArrayList<>(tvs.length);
      for (final java.lang.reflect.TypeVariable<?> typeParameter : tvs) {
        typeParameterElements.add(elementStubFrom(typeParameter));
      }
    }

    final List<DefaultVariableElement> parameterElements;
    final int parameterCount = e.getParameterCount();
    if (parameterCount <= 0) {
      parameterElements = List.of();
    } else {
      final Parameter[] parameters = e.getParameters();
      final List<DefaultVariableElement> list = new ArrayList<>(parameterCount);
      for (final Parameter parameter : parameters) {
        list.add(elementStubFrom(parameter));
      }
      parameterElements = Collections.unmodifiableList(list);
    }

    return
      new DefaultExecutableElement(AnnotatedName.of(annotationMirrorsFrom(e), DefaultName.of(e.getName())),
                                   kind,
                                   typeStubFrom(e),
                                   EnumSet.copyOf(modifierSet),
                                   elementStubFrom(e.getDeclaringClass()),
                                   typeParameterElements,
                                   parameterElements,
                                   e.isVarArgs(),
                                   isDefault,
                                   defaultValue);
  }

  public final DefaultVariableElement elementStubFrom(final Field f) throws IllegalAccessException, InvocationTargetException {
    final AnnotatedType annotatedType = f.getAnnotatedType();
    final Collection<Modifier> modifierSet = new HashSet<>();
    final int modifiers = f.getModifiers();

    final boolean isFinal;
    if (java.lang.reflect.Modifier.isAbstract(modifiers)) {
      modifierSet.add(Modifier.ABSTRACT);
      isFinal = false;
    } else if (java.lang.reflect.Modifier.isFinal(modifiers)) {
      modifierSet.add(Modifier.FINAL);
      isFinal = true;
    } else {
      isFinal = false;
    }

    if (java.lang.reflect.Modifier.isPrivate(modifiers)) {
      modifierSet.add(Modifier.PRIVATE);
    } else if (java.lang.reflect.Modifier.isProtected(modifiers)) {
      modifierSet.add(Modifier.PROTECTED);
    } else if (java.lang.reflect.Modifier.isPublic(modifiers)) {
      modifierSet.add(Modifier.PUBLIC);
    }

    final Object constantValue;
    if (java.lang.reflect.Modifier.isStatic(modifiers)) {
      modifierSet.add(Modifier.STATIC);
      if (isFinal && annotatedType.getType() instanceof Class<?> c && (c == String.class || c.isPrimitive())) {
        constantValue = f.get(null);
      } else {
        constantValue = null;
      }
    } else {
      constantValue = null;
    }

    if (java.lang.reflect.Modifier.isStrict(modifiers)) {
      modifierSet.add(Modifier.STRICTFP);
    }
    if (java.lang.reflect.Modifier.isSynchronized(modifiers)) {
      modifierSet.add(Modifier.SYNCHRONIZED);
    }
    if (java.lang.reflect.Modifier.isTransient(modifiers)) {
      modifierSet.add(Modifier.TRANSIENT);
    }
    if (java.lang.reflect.Modifier.isVolatile(modifiers)) {
      modifierSet.add(Modifier.VOLATILE);
    }
    final EnumSet<Modifier> finalModifiers = EnumSet.copyOf(modifierSet);
    return
      new DefaultVariableElement(AnnotatedName.of(annotationMirrorsFrom(f), DefaultName.of(f.getName())),
                                 ElementKind.FIELD,
                                 typeStubFrom(annotatedType),
                                 finalModifiers,
                                 null,
                                 constantValue);
  }

  public final DefaultModuleElement elementStubFrom(final Module m) throws IllegalAccessException, InvocationTargetException {
    final ModuleDescriptor md = m.getDescriptor();
    return new DefaultModuleElement(AnnotatedName.of(annotationMirrorsFrom(m), DefaultName.of(md.name())), md.isOpen(), List.of());
  }

  public final DefaultPackageElement elementStubFrom(final Package p) throws IllegalAccessException, InvocationTargetException {
    return new DefaultPackageElement(AnnotatedName.of(annotationMirrorsFrom(p), DefaultName.of(p.getName())));
  }

  public final DefaultVariableElement elementStubFrom(final Parameter p) throws IllegalAccessException, InvocationTargetException {
    return
      new DefaultVariableElement(AnnotatedName.of(annotationMirrorsFrom(p), DefaultName.of(p.getName())),
                                 ElementKind.PARAMETER,
                                 typeStubFrom(p.getParameterizedType()),
                                 Set.of(),
                                 null,
                                 null);
  }

  public final DefaultTypeParameterElement elementStubFrom(final java.lang.reflect.TypeVariable<?> t)
    throws IllegalAccessException, InvocationTargetException {
    return elementStubFrom(t, typeStubFrom(t));
  }

  public final DefaultTypeParameterElement elementStubFrom(final java.lang.reflect.TypeVariable<?> t,
                                                           final TypeVariable tv)
    throws IllegalAccessException, InvocationTargetException {
    return
      new DefaultTypeParameterElement(AnnotatedName.of(annotationMirrorsFrom(t), DefaultName.of(t.getName())),
                                      DefaultTypeVariable.of(tv),
                                      Set.of());
  }

  public final AbstractTypeMirror typeStubFrom(final AnnotatedType t) throws IllegalAccessException, InvocationTargetException {
    return switch (t) {
    case null -> null;
    case AnnotatedParameterizedType apt -> typeStubFrom(apt);
    case AnnotatedArrayType aat -> typeStubFrom(aat);
    case AnnotatedTypeVariable atv -> typeStubFrom(atv);
    case AnnotatedWildcardType awt -> typeStubFrom(awt);
    case AnnotatedType t2 when t2.getType() instanceof Class<?> c -> {
      List<? extends AnnotationMirror> annotations = annotationMirrorsFrom(t);
      final TypeMirror enclosingType = typeStubFrom(t2.getAnnotatedOwnerType());
      // A Class can be seen as representing both a type and an
      // element.  Viewed as an element, it has type parameters.
      // Viewed as a type, it has type arguments.
      // java.lang.reflect.TypeVariable represents both.
      final java.lang.reflect.TypeVariable<?>[] tvs = c.getTypeParameters();
      boolean shortcut = annotations.isEmpty() && (enclosingType == null || enclosingType.getKind() == TypeKind.NONE || enclosingType.getAnnotationMirrors().isEmpty());
      final List<DefaultTypeVariable> typeArguments;
      if (tvs.length <= 0) {
        typeArguments = List.of();
        if (shortcut) {
          // Uses caching
          yield typeStubFrom(c);
        }
      } else {
        boolean annotated = false;
        typeArguments = new ArrayList<>(tvs.length);
        for (final java.lang.reflect.TypeVariable<?> tv : tvs) {
          final DefaultTypeVariable typeStub = typeStubFrom(tv);
          typeArguments.add(typeStub);
          if (!annotated && !typeStub.getAnnotationMirrors().isEmpty()) {
            annotated = true;
          }
        }
        if (shortcut && !annotated) {
          // Discard typeArguments; use caching instead
          yield typeStubFrom(c);
        }
      }
      yield new DefaultDeclaredType(enclosingType, typeArguments, annotations);
    }
    default -> throw new IllegalArgumentException("t: " + t);
    };
  }

  public final AbstractTypeMirror typeStubFrom(final Type t) throws IllegalAccessException, InvocationTargetException {
    return switch (t) {
    case null -> null;
    case Class<?> c -> typeStubFrom(c);
    case ParameterizedType p -> typeStubFrom(p);
    case GenericArrayType g -> typeStubFrom(g);
    case java.lang.reflect.TypeVariable<?> tv -> typeStubFrom(tv);
    case java.lang.reflect.WildcardType w -> typeStubFrom(w);
    default -> throw new IllegalArgumentException("t: " + t);
    };
  }

  public final DefaultDeclaredType typeStubFrom(final ParameterizedType p) throws IllegalAccessException, InvocationTargetException {
    final TypeMirror enclosingType = typeStubFrom(p.getOwnerType());
    final List<TypeMirror> typeArguments;
    final Type[] actualTypeArguments = p.getActualTypeArguments();
    if (actualTypeArguments.length <= 0) {
      typeArguments = List.of();
    } else {
      typeArguments = new ArrayList<>(actualTypeArguments.length);
      for (final Type t : actualTypeArguments) {
        typeArguments.add(typeStubFrom(t));
      }
    }
    return new DefaultDeclaredType(enclosingType, typeArguments, null);
  }

  public final DefaultArrayType typeStubFrom(final GenericArrayType t) throws IllegalAccessException, InvocationTargetException {
    return new DefaultArrayType(typeStubFrom(t.getGenericComponentType()), null);
  }

  public final DefaultTypeVariable typeStubFrom(final java.lang.reflect.TypeVariable<?> tv) throws IllegalAccessException, InvocationTargetException {
    final AnnotatedType[] bounds = tv.getAnnotatedBounds();
    // If a java.lang.reflect.TypeVariable has a
    // java.lang.reflect.TypeVariable as its first bound, it is
    // required that this first bound be its only bound.
    final DefaultTypeVariable dtv;
    switch (bounds.length) {
    case 0:
      throw new AssertionError();
    case 1:
      // Class, interface, or type variable
      final AnnotatedType soleBound = bounds[0];
      if (soleBound instanceof AnnotatedTypeVariable tvBound) {
        dtv = new DefaultTypeVariable(typeStubFrom(tvBound), null, annotationMirrorsFrom(tv));
      } else {
        dtv = new DefaultTypeVariable(typeStubFrom(soleBound), null, annotationMirrorsFrom(tv));
      }
      break;
    default:
      final List<TypeMirror> intersectionTypeBounds = new ArrayList<>(bounds.length);
      for (final AnnotatedType bound : bounds) {
        intersectionTypeBounds.add(typeStubFrom(bound));
      }
      dtv = new DefaultTypeVariable(DefaultIntersectionType.of(intersectionTypeBounds), null, annotationMirrorsFrom(tv));
      break;
    }
    final DefaultTypeParameterElement e = elementStubFrom(tv, dtv);
    return dtv;
  }

  public final DefaultWildcardType typeStubFrom(final java.lang.reflect.WildcardType w) throws IllegalAccessException, InvocationTargetException {
    final Type[] lowerBounds = w.getLowerBounds();
    if (lowerBounds.length > 0) {
      return DefaultWildcardType.lowerBoundedWildcardType(typeStubFrom(lowerBounds[0]));
    } else {
      final Type[] upperBounds = w.getUpperBounds();
      final Type soleUpperBound = upperBounds[0];
      if (soleUpperBound == Object.class) {
        // Unbounded.
        return DefaultWildcardType.unboundedWildcardType();
      } else {
        return DefaultWildcardType.upperBoundedWildcardType(typeStubFrom(soleUpperBound));
      }
    }
  }

  public final DefaultArrayType typeStubFrom(final AnnotatedArrayType t) throws IllegalAccessException, InvocationTargetException {
    return new DefaultArrayType(typeStubFrom(t.getAnnotatedGenericComponentType()), annotationMirrorsFrom(t));
  }

  public final DefaultDeclaredType typeStubFrom(final AnnotatedParameterizedType p) throws IllegalAccessException, InvocationTargetException {
    final TypeMirror enclosingType = typeStubFrom(p.getAnnotatedOwnerType());
    final List<TypeMirror> typeArguments;
    final AnnotatedType[] actualTypeArguments = p.getAnnotatedActualTypeArguments();
    if (actualTypeArguments.length <= 0) {
      typeArguments = List.of();
    } else {
      typeArguments = new ArrayList<>(actualTypeArguments.length);
      for (final AnnotatedType t : actualTypeArguments) {
        typeArguments.add(typeStubFrom(t));
      }
    }
    return new DefaultDeclaredType(enclosingType, typeArguments, annotationMirrorsFrom(p));
  }

  public final DefaultTypeVariable typeStubFrom(final AnnotatedTypeVariable tv) throws IllegalAccessException, InvocationTargetException {
    final AnnotatedType[] bounds = tv.getAnnotatedBounds();
    // If a java.lang.reflect.TypeVariable has a
    // java.lang.reflect.TypeVariable as its first bound, it is
    // required that this first bound be its only bound.
    switch (bounds.length) {
    case 0:
      throw new AssertionError();
    case 1:
      // Class, interface, or type variable
      final AnnotatedType soleBound = bounds[0];
      if (soleBound instanceof AnnotatedTypeVariable tvBound) {
        return new DefaultTypeVariable(typeStubFrom(tvBound), null, annotationMirrorsFrom(tv));
      }
      return new DefaultTypeVariable(typeStubFrom(soleBound), null, annotationMirrorsFrom(tv));
    default:
      final List<TypeMirror> intersectionTypeBounds = new ArrayList<>(bounds.length);
      for (final AnnotatedType bound : bounds) {
        intersectionTypeBounds.add(typeStubFrom(bound));
      }
      return new DefaultTypeVariable(DefaultIntersectionType.of(intersectionTypeBounds), null, annotationMirrorsFrom(tv));
    }
  }

  public final DefaultWildcardType typeStubFrom(final AnnotatedWildcardType w) throws IllegalAccessException, InvocationTargetException {
    final AnnotatedType[] lowerBounds = w.getAnnotatedLowerBounds();
    if (lowerBounds.length > 0) {
      return DefaultWildcardType.lowerBoundedWildcardType(typeStubFrom(lowerBounds[0]));
    } else {
      final AnnotatedType[] upperBounds = w.getAnnotatedUpperBounds();
      final AnnotatedType soleUpperBound = upperBounds[0];
      if (soleUpperBound.getType() == Object.class) {
        // Unbounded.
        return DefaultWildcardType.unboundedWildcardType(); // TODO: annotations
      } else {
        return DefaultWildcardType.upperBoundedWildcardType(typeStubFrom(soleUpperBound));
      }
    }
  }

  public final <T> AbstractTypeMirror typeStubFrom(final Class<T> c) throws IllegalAccessException, InvocationTargetException {
    if (c == null) {
      return null;
    } else if (c == void.class) {
      return DefaultNoType.VOID;
    }
    AbstractTypeMirror t = null;
    Lock lock = this.typeStubsByClassLock.readLock();
    lock.lock();
    try {
      t = this.typeStubsByClass.get(c);
    } finally {
      lock.unlock();
    }
    if (t == null) {
      if (c.isArray()) {
        t = new DefaultArrayType(typeStubFrom(c.getComponentType()), annotationMirrorsFrom(c)); // RECURSIVE
      } else if (c.isPrimitive()) {
        if (c == boolean.class) {
          t = DefaultPrimitiveType.BOOLEAN;
        } else if (c == byte.class) {
          t = DefaultPrimitiveType.BYTE;
        } else if (c == char.class) {
          t = DefaultPrimitiveType.CHAR;
        } else if (c == double.class) {
          t = DefaultPrimitiveType.DOUBLE;
        } else if (c == float.class) {
          t = DefaultPrimitiveType.FLOAT;
        } else if (c == int.class) {
          t = DefaultPrimitiveType.INT;
        } else if (c == long.class) {
          t = DefaultPrimitiveType.LONG;
        } else if (c == short.class) {
          t = DefaultPrimitiveType.SHORT;
        } else {
          throw new AssertionError();
        }
      } else {
        final TypeMirror enclosingType = typeStubFrom(c.getEnclosingClass()); // RECURSIVE
        // A Class can be seen as representing both a type and an
        // element.  Viewed as an element, it has type parameters.
        // Viewed as a type, it has type arguments.
        // java.lang.reflect.TypeVariable represents both.
        final java.lang.reflect.TypeVariable<?>[] tvs = c.getTypeParameters();
        final List<TypeVariable> typeArguments;
        if (tvs.length <= 0) {
          typeArguments = List.of();
        } else {
          typeArguments = new ArrayList<>(tvs.length);
          for (final java.lang.reflect.TypeVariable<?> tv : tvs) {
            typeArguments.add(typeStubFrom(tv));
          }
        }
        t = new DefaultDeclaredType(enclosingType, typeArguments, annotationMirrorsFrom(c));
      }
      lock = this.typeStubsByClassLock.writeLock();
      lock.lock();
      try {
        final AbstractTypeMirror t2 = this.typeStubsByClass.get(c);
        if (t2 == null) {
          this.typeStubsByClass.put(c, t);
        } else {
          t = t2;
        }
      } finally {
        lock.unlock();
      }
    }
    return t;
  }

  public final DefaultDeclaredType typeStubFrom(final Annotation a) throws IllegalAccessException, InvocationTargetException {
    return (DefaultDeclaredType)typeStubFrom(a.annotationType());
  }

  public final DefaultExecutableType typeStubFrom(final Executable e) throws IllegalAccessException, InvocationTargetException {
    final AnnotatedType[] parameterTypes = e.getAnnotatedParameterTypes();
    final List<TypeMirror> parameterTypeMirrors;
    if (parameterTypes.length <= 0) {
      parameterTypeMirrors = List.of();
    } else {
      parameterTypeMirrors = new ArrayList<>(parameterTypes.length);
      for (final AnnotatedType t : parameterTypes) {
        parameterTypeMirrors.add(typeStubFrom(t));
      }
    }
    final TypeMirror receiverType = typeStubFrom(e.getAnnotatedReceiverType());
    final TypeMirror returnType = typeStubFrom(e.getAnnotatedReturnType());
    final AnnotatedType[] exceptionTypes = e.getAnnotatedExceptionTypes();
    final List<TypeMirror> thrownTypes;
    if (exceptionTypes.length <= 0) {
      thrownTypes = List.of();
    } else {
      thrownTypes = new ArrayList<>(exceptionTypes.length);
      for (final AnnotatedType t : exceptionTypes) {
        thrownTypes.add(typeStubFrom(t));
      }
    }
    final List<TypeVariable> typeVariables;
    // An Executable can be seen as representing both a type and an
    // element.  Viewed as an element, it has type parameters.
    // Viewed as a type, it has type arguments.
    // java.lang.reflect.TypeVariable represents both.
    final java.lang.reflect.TypeVariable<?>[] tvs = e.getTypeParameters();
    if (tvs.length <= 0) {
      typeVariables = List.of();
    } else {
      typeVariables = new ArrayList<>(tvs.length);
      for (final java.lang.reflect.TypeVariable<?> tv : tvs) {
        typeVariables.add(typeStubFrom(tv));
      }
    }
    return
      new DefaultExecutableType(parameterTypeMirrors,
                                receiverType,
                                returnType,
                                thrownTypes,
                                typeVariables,
                                annotationMirrorsFrom(e));
  }

}
