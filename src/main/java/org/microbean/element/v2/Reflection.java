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

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Executable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReadWriteLock;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

public final class Reflection {

  private final ReadWriteLock typeStubsByClassLock;

  private final Map<Class<?>, TypeMirror> typeStubsByClass;

  private final ReadWriteLock elementStubsByClassLock;

  private final Map<Class<?>, TypeElement> elementStubsByClass;

  public Reflection() {
    super();
    this.typeStubsByClassLock = new ReentrantReadWriteLock();
    this.typeStubsByClass = new WeakHashMap<>();
    this.elementStubsByClassLock = new ReentrantReadWriteLock();
    this.elementStubsByClass = new WeakHashMap<>();
  }

  public TypeElement elementStubFrom(final Class<?> c) {
    if (c == null) {
      return null;
    } else if (c == void.class || c.isArray() || c.isPrimitive()) {
      throw new IllegalArgumentException("c: " + c);
    }
    TypeElement e = null;
    Lock lock = this.elementStubsByClassLock.readLock();
    lock.lock();
    try {
      e = this.elementStubsByClass.get(c);
    } finally {
      lock.unlock();
    }
    if (e == null) {
      // TODO: grab annotations
      final AnnotatedName qualifiedName = AnnotatedName.of(DefaultName.of(c.getName()));
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
      final TypeMirror type = typeStubFrom(c);
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
      Object enclosingObject;
      final NestingKind nestingKind;
      if (c.isAnonymousClass()) {
        nestingKind = NestingKind.ANONYMOUS;
        enclosingObject = c.getEnclosingMethod();
        if (enclosingObject == null) {
          enclosingObject = c.getEnclosingConstructor();
        }
        // Note: we don't want to call c.getEnclosingClass(), because
        // that will jump the hierarchy. An anonymous class is lexically
        // enclosed only by an non-static executable.  (I think. :-))
        assert enclosingObject != null;
      } else if (c.isLocalClass()) {
        nestingKind = NestingKind.LOCAL;
        enclosingObject = c.getEnclosingMethod();
        if (enclosingObject == null) {
          enclosingObject = c.getEnclosingConstructor();
        }
        assert enclosingObject != null;
      } else if (c.isMemberClass()) {
        nestingKind = NestingKind.MEMBER;
        enclosingObject = c.getDeclaringClass();
        assert enclosingObject != null;
      } else {
        nestingKind = NestingKind.TOP_LEVEL;
        enclosingObject = null;
      }
      final Element enclosingElement = enclosingObject instanceof Executable ex ? elementStubFrom(ex) : null;
      final AnnotatedType annotatedSuperclass = c.getAnnotatedSuperclass();
      final TypeMirror superclass = typeStubFrom(annotatedSuperclass);
      final List<TypeMirror> permittedSubclassTypeMirrors;
      if (c.isSealed()) {
        final Class<?>[] permittedSubclasses = c.getPermittedSubclasses();
        if (permittedSubclasses.length <= 0) {
          permittedSubclassTypeMirrors = List.of();
        } else {
          permittedSubclassTypeMirrors = new ArrayList<>(permittedSubclasses.length);
          for (final Class<?> psc : permittedSubclasses) {
            permittedSubclassTypeMirrors.add(typeStubFrom(psc));
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
          interfaceTypeMirrors.add(typeStubFrom(t));
        }
      }
      final List<DefaultTypeParameterElement> typeParameterElements;
      final java.lang.reflect.TypeVariable<?>[] typeParameters = c.getTypeParameters();
      if (typeParameters.length <= 0) {
        typeParameterElements = List.of();
      } else {
        typeParameterElements = new ArrayList<>(typeParameters.length);
        for (final java.lang.reflect.TypeVariable<?> typeParameter : typeParameters) {
          typeParameterElements.add(elementStubFrom(typeParameter));
        }
      }
      e =
        new DefaultTypeElement(qualifiedName,
                               kind,
                               type,
                               finalModifiers,
                               nestingKind,
                               superclass,
                               permittedSubclassTypeMirrors,
                               interfaceTypeMirrors,
                               enclosingElement,
                               List.of(), // enclosedElements; this is a stub; we don't inflate them
                               typeParameterElements);
      lock = this.elementStubsByClassLock.writeLock();
      lock.lock();
      try {
        final TypeElement e2 = this.elementStubsByClass.get(c);
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

  public ExecutableElement elementStubFrom(final Executable e) {
    if (e == null) {
      return null;
    }
    final AnnotatedName simpleName = AnnotatedName.of(DefaultName.of(e.getName()));
    final int modifiers = e.getModifiers();
    final Collection<Modifier> modifierSet = new HashSet<>();
    final ElementKind kind;
    final boolean isDefault;
    if (e instanceof Method m) {
      kind = ElementKind.METHOD;
      isDefault = m.isDefault();
      if (isDefault) {
        modifierSet.add(Modifier.DEFAULT);
      } else {
        if (java.lang.reflect.Modifier.isAbstract(modifiers)) {
          modifierSet.add(Modifier.ABSTRACT);
        }
        if (java.lang.reflect.Modifier.isFinal(modifiers)) {
          modifierSet.add(Modifier.FINAL);
        }
        if (java.lang.reflect.Modifier.isNative(modifiers)) {
          modifierSet.add(Modifier.NATIVE);
        }
        if (java.lang.reflect.Modifier.isStatic(modifiers)) {
          modifierSet.add(Modifier.STATIC);
        }
        if (java.lang.reflect.Modifier.isSynchronized(modifiers)) {
          modifierSet.add(Modifier.SYNCHRONIZED);
        }
      }
    } else {
      kind = ElementKind.CONSTRUCTOR;
      isDefault = false;
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
    final ExecutableType type = typeStubFrom(e);
    final EnumSet<Modifier> finalModifiers = EnumSet.copyOf(modifierSet);
    final TypeElement enclosingElement = elementStubFrom(e.getDeclaringClass());

    final List<DefaultTypeParameterElement> typeParameterElements;
    final java.lang.reflect.TypeVariable<?>[] typeParameters = e.getTypeParameters();
    if (typeParameters.length <= 0) {
      typeParameterElements = List.of();
    } else {
      typeParameterElements = new ArrayList<>(typeParameters.length);
      for (final java.lang.reflect.TypeVariable<?> typeParameter : typeParameters) {
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

    // TODO: no real way to handle default value

    final DefaultExecutableElement returnValue =
      new DefaultExecutableElement(simpleName,
                                   kind,
                                   type,
                                   finalModifiers,
                                   enclosingElement,
                                   typeParameterElements,
                                   parameterElements,
                                   e.isVarArgs(),
                                   isDefault,
                                   null);

    return returnValue;
  }

  public DefaultVariableElement elementStubFrom(final Parameter p) {
    final AnnotatedName simpleName = AnnotatedName.of(DefaultName.of(p.getName()));
    final TypeMirror type = typeStubFrom(p.getParameterizedType());
    return new DefaultVariableElement(simpleName, ElementKind.PARAMETER, type, Set.of(), null, null);
  }

  public DefaultTypeParameterElement elementStubFrom(final java.lang.reflect.TypeVariable<?> t) {
    return
      new DefaultTypeParameterElement(AnnotatedName.of(DefaultName.of(t.getName())),
                                      typeStubFrom(t),
                                      Set.of()); // TODO: verify: what modifiers could possibly exist on a TypeParameterElement?
  }

  public TypeMirror typeStubFrom(final AnnotatedType t) {
    return switch (t) {
    case null -> null;
    case AnnotatedParameterizedType apt -> typeStubFrom(apt);
    case AnnotatedArrayType aat -> typeStubFrom(aat);
    case AnnotatedTypeVariable atv -> typeStubFrom(atv);
    case AnnotatedWildcardType awt -> typeStubFrom(awt);
    case AnnotatedType t2 when t2.getType() instanceof Class<?> c -> {
      final TypeMirror enclosingType = typeStubFrom(t2.getAnnotatedOwnerType());
      final java.lang.reflect.TypeVariable<?>[] typeParameters = c.getTypeParameters();
      final List<TypeVariable> typeArguments;
      if (typeParameters.length <= 0) {
        typeArguments = List.of();
      } else {
        typeArguments = new ArrayList<>(typeParameters.length);
        for (final java.lang.reflect.TypeVariable<?> typeParameter : typeParameters) {
          typeArguments.add(typeStubFrom(typeParameter));
        }
      }
      yield new DefaultDeclaredType(enclosingType, typeArguments, null);
    }
    default -> throw new IllegalArgumentException();
    };
  }

  public TypeMirror typeStubFrom(final Type t) {
    return switch (t) {
    case null -> null;
    case Class<?> c -> typeStubFrom(c);
    case ParameterizedType p -> typeStubFrom(p);
    case GenericArrayType g -> typeStubFrom(g);
    case java.lang.reflect.TypeVariable<?> tv -> typeStubFrom(tv);
    case java.lang.reflect.WildcardType w -> typeStubFrom(w);
    default -> throw new IllegalArgumentException();
    };
  }

  public DeclaredType typeStubFrom(final ParameterizedType p) {
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

  public ArrayType typeStubFrom(final GenericArrayType t) {
    return new DefaultArrayType(typeStubFrom(t.getGenericComponentType()), null);
  }

  public TypeVariable typeStubFrom(final java.lang.reflect.TypeVariable<?> tv) {
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
        return new DefaultTypeVariable(typeStubFrom(tvBound), null, null);
      }
      return new DefaultTypeVariable(typeStubFrom(soleBound), null, null);
    default:
      final List<TypeMirror> intersectionTypeBounds = new ArrayList<>(bounds.length);
      for (final AnnotatedType bound : bounds) {
        intersectionTypeBounds.add(typeStubFrom(bound));
      }
      return new DefaultTypeVariable(DefaultIntersectionType.of(intersectionTypeBounds), null, null);
    }
  }
  
  public WildcardType typeStubFrom(final java.lang.reflect.WildcardType w) {
    final Type[] lowerBounds = w.getLowerBounds();
    if (lowerBounds.length > 0) {
      return DefaultWildcardType.lowerBoundedWildcardType(typeStubFrom(lowerBounds[0]));
    } else {
      final Type[] upperBounds = w.getUpperBounds();
      final Type soleUpperBound = upperBounds[0];
      if (soleUpperBound == Object.class) {
        // Unbounded.
        return DefaultWildcardType.unboundedWildcardType(); // TODO: annotations
      } else {
        return DefaultWildcardType.upperBoundedWildcardType(typeStubFrom(soleUpperBound));
      }
    }
  }
  
  public ArrayType typeStubFrom(final AnnotatedArrayType t) {
    return new DefaultArrayType(typeStubFrom(t.getAnnotatedGenericComponentType()), null);
  }

  public DeclaredType typeStubFrom(final AnnotatedParameterizedType p) {
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
    return new DefaultDeclaredType(enclosingType, typeArguments, null);
  }

  public DefaultTypeVariable typeStubFrom(final AnnotatedTypeVariable tv) {
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
        return new DefaultTypeVariable(typeStubFrom(tvBound), null, null);
      }
      return new DefaultTypeVariable(typeStubFrom(soleBound), null, null);
    default:
      final List<TypeMirror> intersectionTypeBounds = new ArrayList<>(bounds.length);
      for (final AnnotatedType bound : bounds) {
        intersectionTypeBounds.add(typeStubFrom(bound));
      }
      return new DefaultTypeVariable(DefaultIntersectionType.of(intersectionTypeBounds), null, null);
    }
  }

  public WildcardType typeStubFrom(final AnnotatedWildcardType w) {
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

  public <T> TypeMirror typeStubFrom(final Class<T> c) {
    if (c == null) {
      return null;
    } else if (c == void.class) {
      return DefaultNoType.VOID;
    }
    TypeMirror t = null;
    Lock lock = this.typeStubsByClassLock.readLock();
    lock.lock();
    try {
      t = this.typeStubsByClass.get(c);
    } finally {
      lock.unlock();
    }
    if (t == null) {
      if (c.isArray()) {
        t = new DefaultArrayType(typeStubFrom(c.getComponentType()), null); // RECURSIVE // TODO: annotations
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
        final java.lang.reflect.TypeVariable<?>[] typeParameters = c.getTypeParameters();
        final List<TypeVariable> typeArguments;
        if (typeParameters.length <= 0) {
          typeArguments = List.of();
        } else {
          typeArguments = new ArrayList<>(typeParameters.length);
          for (final java.lang.reflect.TypeVariable<?> tp : typeParameters) {
            typeArguments.add(typeStubFrom(tp));
          }
        }
        t = new DefaultDeclaredType(enclosingType, typeArguments, null);
      }
      lock = this.typeStubsByClassLock.writeLock();
      lock.lock();
      try {
        final TypeMirror t2 = this.typeStubsByClass.get(c);
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

  public ExecutableType typeStubFrom(final Executable e) {
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
    final java.lang.reflect.TypeVariable<?>[] typeParameters = e.getTypeParameters();
    if (typeParameters.length <= 0) {
      typeVariables = List.of();
    } else {
      typeVariables = new ArrayList<>(typeParameters.length);
      for (final java.lang.reflect.TypeVariable<?> t : typeParameters) {
        typeVariables.add(typeStubFrom(t));
      }
    }
    return new DefaultExecutableType(parameterTypeMirrors, receiverType, returnType, thrownTypes, typeVariables, null);
  }

}
