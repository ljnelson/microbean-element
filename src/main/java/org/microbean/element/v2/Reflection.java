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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
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
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReadWriteLock;

import javax.lang.model.AnnotatedConstruct;

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

  private static final EqualityBasedEqualsAndHashCode EHC_NO_ANNOTATIONS = new EqualityBasedEqualsAndHashCode(false);
  
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

  private final DefaultAnnotationMirror annotationMirrorFrom(final Annotation a)
    throws IllegalAccessException, InvocationTargetException {
    final Class<? extends Annotation> annotationType = a.annotationType();
    final DefaultDeclaredType t = (DefaultDeclaredType)typeStubFrom(annotationType);

    // Deliberately used for side effects only.
    elementStubFrom(annotationType, t);
    
    final Map<ExecutableElement, DefaultAnnotationValue> values = new HashMap<>();
    for (final Method annotationElement : annotationType.getDeclaredMethods()) {
      assert annotationElement.getParameterCount() == 0;
      assert annotationElement.getReturnType() != void.class;
      assert !"toString".equals(annotationElement.getName());
      assert !"hashCode()".equals(annotationElement.getName());
      values.put(DefaultElement.of(elementStubFrom(annotationElement), EHC_NO_ANNOTATIONS),
                 annotationValueFrom(annotationElement.invoke(a)));
    }
    return new DefaultAnnotationMirror(t, values);
  }

  private final List<? extends DefaultAnnotationMirror> annotationMirrorsFrom(final AnnotatedElement ae)
    throws IllegalAccessException, InvocationTargetException {
    return annotationMirrorsFrom(ae.getDeclaredAnnotations());
  }

  private final List<? extends DefaultAnnotationMirror> annotationMirrorsFrom(final Collection<? extends Annotation> annotations)
    throws IllegalAccessException, InvocationTargetException {
    if (annotations == null || annotations.isEmpty()) {
      return List.of();
    }
    final List<DefaultAnnotationMirror> list = new ArrayList<>(annotations.size());
    for (final Annotation annotation : annotations) {
      list.add(annotationMirrorFrom(annotation));
    }
    return Collections.unmodifiableList(list);
  }

  private final List<? extends DefaultAnnotationMirror> annotationMirrorsFrom(final Annotation[] annotations)
    throws IllegalAccessException, InvocationTargetException {
    if (annotations == null || annotations.length <= 0) {
      return List.of();
    }
    final List<DefaultAnnotationMirror> list = new ArrayList<>(annotations.length);
    for (final Annotation annotation : annotations) {
      list.add(annotationMirrorFrom(annotation));
    }
    return Collections.unmodifiableList(list);
  }

  private final List<? extends DefaultAnnotationMirror> annotationMirrorsFrom(final Enum<?> e) throws IllegalAccessException, InvocationTargetException {
    final Field[] fields = e.getDeclaringClass().getDeclaredFields();
    final int ordinal = e.ordinal();
    for (int i = 0; i < fields.length; i++) {
      final Field field = fields[i];
      if (i == ordinal && field.isEnumConstant()) {
        return annotationMirrorsFrom(field);
      }
    }
    return List.of();
  }

  private final List<? extends DefaultAnnotationValue> annotationValuesFrom(final Collection<?> c) throws IllegalAccessException, InvocationTargetException {
    if (c.isEmpty()) {
      return List.of();
    }
    final List<DefaultAnnotationValue> list = new ArrayList<>(c.size());
    for (final Object value : c) {
      list.add(annotationValueFrom(value));
    }
    return Collections.unmodifiableList(list);
  }

  private final List<? extends DefaultAnnotationValue> annotationValuesFrom(final Object[] array) throws IllegalAccessException, InvocationTargetException {
    if (array == null || array.length <= 0) {
      return List.of();
    }
    final List<DefaultAnnotationValue> list = new ArrayList<>(array.length);
    for (final Object value : array) {
      list.add(annotationValueFrom(value));
    }
    return Collections.unmodifiableList(list);
  }

  private final DefaultAnnotationValue annotationValueFrom(final Object value) throws IllegalAccessException, InvocationTargetException {
    switch (value) {
    case String s : return annotationValueFrom(s);
    case Boolean b : return annotationValueFrom(b);
    case Integer i : return annotationValueFrom(i);
    case Enum<?> e : return annotationValueFrom(e);
    case Class<?> c : return annotationValueFrom(c);
    case Object[] array : return annotationValueFrom(array);
    case Annotation a : return annotationValueFrom(a);
    case Byte b : return annotationValueFrom(b);
    case Character c : return annotationValueFrom(c);
    case Double d : return annotationValueFrom(d);
    case Float f : return annotationValueFrom(f);
    case Long l : return annotationValueFrom(l);
    case Short s : return annotationValueFrom(s);
    case Method m : return annotationValueFrom(m);
    case TypeMirror t : return annotationValueFrom(t);
    case VariableElement v : return annotationValueFrom(v);
    case AnnotationMirror a : return annotationValueFrom(a);
    case List<?> l : return annotationValueFrom(l);
    default : throw new IllegalArgumentException("value: " + value);
    }
  }

  private final DefaultAnnotationValue annotationValueFrom(final String s) throws IllegalAccessException, InvocationTargetException {
    return new DefaultAnnotationValue(s);
  }

  private final DefaultAnnotationValue annotationValueFrom(final Boolean b) throws IllegalAccessException, InvocationTargetException {
    return new DefaultAnnotationValue(b);
  }

  private final DefaultAnnotationValue annotationValueFrom(final Integer i) throws IllegalAccessException, InvocationTargetException {
    return new DefaultAnnotationValue(i);
  }

  private final DefaultAnnotationValue annotationValueFrom(final Enum<?> e) throws IllegalAccessException, InvocationTargetException {
    return
      new DefaultAnnotationValue(new DefaultVariableElement(DefaultName.of(e.name()),
                                                            annotationMirrorsFrom(e),
                                                            ElementKind.ENUM,
                                                            typeStubFrom(e.getDeclaringClass()),
                                                            Set.of(), // TODO: modifiers
                                                            null, // enclosingElement; not defined by javadocs?!
                                                            null) // no constant value // TODO: check
                                 );
  }

  private final DefaultAnnotationValue annotationValueFrom(final Class<?> c) throws IllegalAccessException, InvocationTargetException {
    return new DefaultAnnotationValue(typeStubFrom(c));
  }

  private final DefaultAnnotationValue annotationValueFrom(final Object[] array) throws IllegalAccessException, InvocationTargetException {
    return new DefaultAnnotationValue(annotationValuesFrom(array));
  }

  private final DefaultAnnotationValue annotationValueFrom(final Annotation a) throws IllegalAccessException, InvocationTargetException {
    return new DefaultAnnotationValue(annotationMirrorFrom(a));
  }

  private final DefaultAnnotationValue annotationValueFrom(final Byte b) throws IllegalAccessException, InvocationTargetException {
    return new DefaultAnnotationValue(b);
  }

  private final DefaultAnnotationValue annotationValueFrom(final Character c) throws IllegalAccessException, InvocationTargetException {
    return new DefaultAnnotationValue(c);
  }

  private final DefaultAnnotationValue annotationValueFrom(final Double d) throws IllegalAccessException, InvocationTargetException {
    return new DefaultAnnotationValue(d);
  }

  private final DefaultAnnotationValue annotationValueFrom(final Float f) throws IllegalAccessException, InvocationTargetException {
    return new DefaultAnnotationValue(f);
  }

  private final DefaultAnnotationValue annotationValueFrom(final Long l) throws IllegalAccessException, InvocationTargetException {
    return new DefaultAnnotationValue(l);
  }

  private final DefaultAnnotationValue annotationValueFrom(final Short s) throws IllegalAccessException, InvocationTargetException {
    return new DefaultAnnotationValue(s);
  }

  private final <T> DefaultAnnotationValue annotationValueFrom(final Method m) throws IllegalAccessException, InvocationTargetException {
    final Object defaultValue = m.getDefaultValue();
    if (defaultValue == null) {
      return null;
    }
    return new DefaultAnnotationValue(defaultValue);
  }

  private final DefaultAnnotationValue annotationValueFrom(final TypeMirror t) throws IllegalAccessException, InvocationTargetException {
    return new DefaultAnnotationValue(t);
  }

  private final DefaultAnnotationValue annotationValueFrom(final VariableElement v) throws IllegalAccessException, InvocationTargetException {
    return new DefaultAnnotationValue(v);
  }

  private final DefaultAnnotationValue annotationValueFrom(final AnnotationMirror a) throws IllegalAccessException, InvocationTargetException {
    return new DefaultAnnotationValue(a);
  }

  private final DefaultAnnotationValue annotationValueFrom(final List<?> list) throws IllegalAccessException, InvocationTargetException {
    return new DefaultAnnotationValue(annotationValuesFrom(list));
  }


  //
  // Element Stubs
  //


  public final DefaultTypeElement elementStubFrom(final Class<?> c)
    throws IllegalAccessException, InvocationTargetException {
    return elementStubFrom(c, (DefaultDeclaredType)typeStubFrom(c));
  }

  private final DefaultTypeElement elementStubFrom(final Class<?> c, final DefaultDeclaredType type)
    throws IllegalAccessException, InvocationTargetException {
    if (c == null) {
      return null;
    } else if (c == void.class || c.isArray() || c.isPrimitive()) {
      throw new IllegalArgumentException("c: " + c);
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
        enclosingElement = elementStubFrom(c.getModule(), c.getPackage());
      }

      final AnnotatedType annotatedSuperclass = c.getAnnotatedSuperclass();
      final DefaultDeclaredType supertype = annotatedSuperclass == null ? null : (DefaultDeclaredType)typeStubFrom(annotatedSuperclass);

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
      final java.lang.reflect.TypeVariable<?>[] typeParameters = c.getTypeParameters();
      if (typeParameters.length <= 0) {
        if (!type.getTypeArguments().isEmpty()) {
          throw new IllegalArgumentException("type: " + type);
        }
        typeParameterElements = List.of();
      } else {
        @SuppressWarnings("unchecked")
        final List<? extends DefaultTypeVariable> typeArguments = (List<? extends DefaultTypeVariable>)type.getTypeArguments();
        assert typeArguments.size() == typeParameters.length;
        typeParameterElements = new ArrayList<>(typeParameters.length);
        for (int i = 0; i < typeParameters.length; i++) {
          final java.lang.reflect.TypeVariable<?> typeParameter = typeParameters[i];
          final DefaultTypeVariable typeArgument = typeArguments.get(i);
          assert !typeArgument.defined();
          final DefaultTypeParameterElement typeParameterElement = elementStubFrom(typeParameter, typeArgument);
          assert typeArgument.defined();
          assert typeArgument.asElement() == typeParameterElement;

          // For every bound, use the elementStubFrom() methods to
          // create an appropriate Element for it (when, obviously,
          // the bound is either a Class or a
          // java.lang.reflect.TypeVariable, as those are the only
          // kinds of types that have Elements in the
          // javax.lang.model.* language model).  This deliberately
          // relies on elementStubFrom()'s caching behavior as a side
          // effect.
          for (final AnnotatedType bound : typeParameter.getAnnotatedBounds()) {
            switch (bound) {
            case AnnotatedTypeVariable tv -> elementStubFrom(tv);
            case AnnotatedType a -> elementStubFrom(a);
            };
          }

          typeParameterElements.add(typeParameterElement);
        }
      }

      final Annotation[] declaredAnnotations = c.getDeclaredAnnotations();

      e =
        new DefaultTypeElement(DefaultName.of(c.getName()),
                               null, // annotations; break potential cycle; see below
                               kind,
                               type,
                               finalModifiers,
                               nestingKind,
                               supertype,
                               permittedSubclassTypeMirrors,
                               interfaceTypeMirrors,
                               enclosingElement,
                               new DeferredList<>(() -> this.memberElementStubsFrom(c)), // enclosedElements
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

      // Now get the annotations and set them. Because annotation
      // classes can annotate themselves (!) there is a cycle that
      // needs to be broken via this mechanism.
      final List<? extends AnnotationMirror> annotationMirrors = annotationMirrorsFrom(declaredAnnotations);
      e.setAnnotationMirrors(annotationMirrors);

    }
    return e;
  }

  private final AbstractElement elementStubFrom(final AccessibleObject a) throws IllegalAccessException, InvocationTargetException {
    return switch (a) {
    case null -> null;
    case GenericDeclaration g -> elementStubFrom(g);
    case Field f -> elementStubFrom(f);
    default -> throw new IllegalArgumentException("a: " + a);
    };
  }

  private final AbstractElement elementStubFrom(final GenericDeclaration g) throws IllegalAccessException, InvocationTargetException {
    return switch (g) {
    case null -> null;
    case Class<?> c -> elementStubFrom(c);
    case Executable e -> elementStubFrom(e);
    default -> throw new IllegalArgumentException("g: " + g);
    };
  }

  private final DefaultExecutableElement elementStubFrom(final Executable e)
    throws IllegalAccessException, InvocationTargetException {
    return this.elementStubFrom(e, typeStubFrom(e));
  }

  final DefaultExecutableElement elementStubFrom(final Executable e,
                                                 final DefaultExecutableType type)
    throws IllegalAccessException, InvocationTargetException {
    if (e == null) {
      return null;
    }
    final int modifiers = e.getModifiers();
    final Collection<Modifier> modifierSet = new HashSet<>();
    final ElementKind kind;
    final DefaultName simpleName;
    final boolean isDefault;
    final AnnotationValue defaultValue;
    if (e instanceof Method m) {
      // TODO: handle static and instance initializers
      kind = ElementKind.METHOD;
      simpleName = DefaultName.of(m.getName());
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
      simpleName = DefaultName.of("<init>");
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
    final java.lang.reflect.TypeVariable<?>[] typeParameters = e.getTypeParameters();
    if (typeParameters.length <= 0) {
      if (!type.getTypeVariables().isEmpty()) {
        throw new IllegalArgumentException("type: " + type);
      }
      typeParameterElements = List.of();
    } else {
      @SuppressWarnings("unchecked")
      final List<? extends DefaultTypeVariable> typeVariables = (List<? extends DefaultTypeVariable>)type.getTypeVariables();
      assert typeVariables.size() == typeParameters.length;
      typeParameterElements = new ArrayList<>(typeParameters.length);
      for (int i = 0; i < typeParameters.length; i++) {
        final java.lang.reflect.TypeVariable<?> typeParameter = typeParameters[i];
        final DefaultTypeVariable typeVariable = typeVariables.get(i);
        assert !typeVariable.defined();
        final DefaultTypeParameterElement typeParameterElement = elementStubFrom(typeParameter, typeVariable);
        assert typeVariable.defined();
        assert typeVariable.asElement() == typeParameterElement;

        // Side effects only below.
        //
        // For every bound, use the elementStubFrom() methods to
        // create an appropriate Element for it (when, obviously, the
        // bound is either a Class or a
        // java.lang.reflect.TypeVariable, as those are the only kinds
        // of types that have Elements in the javax.lang.model.*
        // language model).  This deliberately relies on
        // elementStubFrom()'s caching behavior as a side effect.
        for (final AnnotatedType bound : typeParameter.getAnnotatedBounds()) {
          switch (bound) {
          case AnnotatedTypeVariable tv -> elementStubFrom(tv);
          case AnnotatedType a -> elementStubFrom(a);
          };
        }

        typeParameterElements.add(typeParameterElement);
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
      new DefaultExecutableElement(simpleName,
                                   annotationMirrorsFrom(e),
                                   kind,
                                   type,
                                   modifierSet.isEmpty() ? Set.of() : EnumSet.copyOf(modifierSet),
                                   elementStubFrom(e.getDeclaringClass()), // enclosingElement
                                   typeParameterElements,
                                   parameterElements,
                                   e.isVarArgs(),
                                   isDefault,
                                   defaultValue);
  }

  private final DefaultVariableElement elementStubFrom(final Field f) throws IllegalAccessException, InvocationTargetException {
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
      if (isFinal && annotatedType.getType() instanceof Class<?> c && (c == String.class || c.isPrimitive()) && f.trySetAccessible()) {
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
      new DefaultVariableElement(DefaultName.of(f.getName()),
                                 annotationMirrorsFrom(f),
                                 ElementKind.FIELD,
                                 typeStubFrom(annotatedType),
                                 finalModifiers,
                                 elementStubFrom(f.getDeclaringClass()), // enclosingElement
                                 constantValue);
  }

  private final DefaultModuleElement elementStubFrom(final Module m) throws IllegalAccessException, InvocationTargetException {
    final ModuleDescriptor md = m.getDescriptor();
    return new DefaultModuleElement(DefaultName.of(md.name()),
                                    annotationMirrorsFrom(m),
                                    md.isOpen(),
                                    List.of());
  }

  private final DefaultPackageElement elementStubFrom(final Module m, final Package p)
    throws IllegalAccessException, InvocationTargetException {
    final DefaultPackageElement returnValue =
      new DefaultPackageElement(DefaultName.of(p.getName()), annotationMirrorsFrom(p));
    returnValue.setEnclosingElement(elementStubFrom(m));
    return returnValue;
  }

  private final DefaultVariableElement elementStubFrom(final Parameter p)
    throws IllegalAccessException, InvocationTargetException {
    return
      new DefaultVariableElement(DefaultName.of(p.getName()),
                                 annotationMirrorsFrom(p),
                                 ElementKind.PARAMETER,
                                 typeStubFrom(p.getParameterizedType()),
                                 Set.of(), // modifiers
                                 null, // enclosing element; normally is set by the DefaultExecutableElement that is probably being built
                                 null); // constant value
  }

  private final AbstractElement elementStubFrom(final AnnotatedType t) throws IllegalAccessException, InvocationTargetException {
    return switch (t) {
    case AnnotatedTypeVariable tv -> elementStubFrom(tv);
    case AnnotatedType t2 when t2.getType() instanceof Class<?> c -> elementStubFrom(c);
    default -> throw new IllegalArgumentException("t: " + t);
    };
  }

  private final DefaultTypeParameterElement elementStubFrom(final AnnotatedTypeVariable t)
    throws IllegalAccessException, InvocationTargetException {
    // Discard annotations on purpose.
    return elementStubFrom((java.lang.reflect.TypeVariable<?>)t.getType());
  }

  private final DefaultTypeParameterElement elementStubFrom(final java.lang.reflect.TypeVariable<?> t)
    throws IllegalAccessException, InvocationTargetException {
    // Yes, you need the two-argument form.
    return elementStubFrom(t, typeStubFrom(t));
  }

  private final DefaultTypeParameterElement elementStubFrom(final java.lang.reflect.TypeVariable<?> t,
                                                            final DefaultTypeVariable tv)
    throws IllegalAccessException, InvocationTargetException {
    // Don't get clever; this method is implemented nice and simple
    // and should stay that way.  Yes, you need the two-argument form.
    // If you're looking for various side-effect element-defining
    // shenanigans, look elsewhere.
    assert !tv.defined();
    return
      new DefaultTypeParameterElement(DefaultName.of(t.getName()),
                                      annotationMirrorsFrom(t), // annotations are element annotations; this is appropriate
                                      tv,
                                      Set.of()); // modifiers
  }


  //
  // Type Stubs
  //


  private final AbstractTypeMirror typeStubFrom(final AnnotatedType t) throws IllegalAccessException, InvocationTargetException {
    return switch (t) {
    case null -> null;
    case AnnotatedParameterizedType apt -> typeStubFrom(apt);
    case AnnotatedArrayType aat -> typeStubFrom(aat);
    case AnnotatedTypeVariable atv -> typeStubFrom(atv);
    case AnnotatedWildcardType awt -> typeStubFrom(awt);
    case AnnotatedType t2 when t2.getType() instanceof Class<?> c -> {
      List<? extends AnnotationMirror> annotations = annotationMirrorsFrom(t);
      final TypeMirror enclosingType = typeStubFrom(t2.getAnnotatedOwnerType()); // OK; could be NONE, remember
      final java.lang.reflect.TypeVariable<?>[] tvs = c.getTypeParameters();
      boolean shortcut =
        annotations.isEmpty() &&
        (enclosingType == null || enclosingType.getKind() == TypeKind.NONE || enclosingType.getAnnotationMirrors().isEmpty());
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
          if (!annotated) {
            if (!typeStub.getAnnotationMirrors().isEmpty()) {
              annotated = true;
            }
            AnnotatedConstruct bound = null;
            if (!annotated) {
              bound = typeStub.getUpperBound();
              if (bound != null && !bound.getAnnotationMirrors().isEmpty()) {
                annotated = true;
              }
            }
            if (!annotated) {
              bound = typeStub.getLowerBound();
              if (bound != null && !bound.getAnnotationMirrors().isEmpty()) {
                annotated = true;
              }
            }
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

  private final AbstractTypeMirror typeStubFrom(final Type t) throws IllegalAccessException, InvocationTargetException {
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

  private final DefaultDeclaredType typeStubFrom(final ParameterizedType p)
    throws IllegalAccessException, InvocationTargetException {
    final Type ownerType = p.getOwnerType();
    final TypeMirror enclosingType = ownerType == null ? DefaultNoType.NONE : typeStubFrom(ownerType);
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

  private final DefaultArrayType typeStubFrom(final GenericArrayType t) throws IllegalAccessException, InvocationTargetException {
    return new DefaultArrayType(typeStubFrom(t.getGenericComponentType()), null);
  }

  private final DefaultTypeVariable typeStubFrom(final java.lang.reflect.TypeVariable<?> tv)
    throws IllegalAccessException, InvocationTargetException {
    // Don't get clever.  This method is implemented simply and
    // straightforwardly and that's all on purpose.  If you're looking
    // for element-related side-effect shenanigans, look elsewhere.
    final AnnotatedType[] bounds = tv.getAnnotatedBounds();
    final DefaultTypeVariable dtv;
    switch (bounds.length) {
    case 0:
      throw new AssertionError();
    case 1:
      // Class, interface, or type variable
      final AnnotatedType soleBound = bounds[0];
      dtv = new DefaultTypeVariable(typeStubFrom(soleBound), null, annotationMirrorsFrom(soleBound));
      break;
    default:
      final List<TypeMirror> intersectionTypeBounds = new ArrayList<>(bounds.length);
      for (final AnnotatedType bound : bounds) {
        intersectionTypeBounds.add(typeStubFrom(bound));
      }
      // dtv = new DefaultTypeVariable(DefaultIntersectionType.of(intersectionTypeBounds), null, annotationMirrorsFrom(tv));
      dtv = new DefaultTypeVariable(DefaultIntersectionType.of(intersectionTypeBounds), null, List.of());
      break;
    }
    return dtv;
  }

  private final DefaultWildcardType typeStubFrom(final java.lang.reflect.WildcardType w)
    throws IllegalAccessException, InvocationTargetException {
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

  private final DefaultArrayType typeStubFrom(final AnnotatedArrayType t)
    throws IllegalAccessException, InvocationTargetException {
    return new DefaultArrayType(typeStubFrom(t.getAnnotatedGenericComponentType()), annotationMirrorsFrom(t));
  }

  private final DefaultDeclaredType typeStubFrom(final AnnotatedParameterizedType p)
    throws IllegalAccessException, InvocationTargetException {
    final AnnotatedType annotatedOwnerType = p.getAnnotatedOwnerType();
    final TypeMirror enclosingType = annotatedOwnerType == null ? DefaultNoType.NONE : typeStubFrom(annotatedOwnerType);
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

  private final DefaultTypeVariable typeStubFrom(final AnnotatedTypeVariable tv)
    throws IllegalAccessException, InvocationTargetException {
    final AnnotatedType[] bounds = tv.getAnnotatedBounds();
    // If a java.lang.reflect.TypeVariable has a
    // java.lang.reflect.TypeVariable as its first bound, it is
    // required that this first bound be its only bound.
    switch (bounds.length) {
    case 0:
      throw new AssertionError();
    case 1:
      // Class, interface, or type variable
      return new DefaultTypeVariable(typeStubFrom(bounds[0]), null, annotationMirrorsFrom(tv));
    default:
      final List<TypeMirror> intersectionTypeBounds = new ArrayList<>(bounds.length);
      for (final AnnotatedType bound : bounds) {
        intersectionTypeBounds.add(typeStubFrom(bound));
      }
      return new DefaultTypeVariable(DefaultIntersectionType.of(intersectionTypeBounds), null, annotationMirrorsFrom(tv));
    }
  }

  private final DefaultWildcardType typeStubFrom(final AnnotatedWildcardType w)
    throws IllegalAccessException, InvocationTargetException {
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
        } else if (c == void.class) {
          // void.class.isPrimitive() returns true, even though it's
          // not a primitive class in the JLS.
          t = DefaultNoType.VOID;
        } else {
          throw new AssertionError();
        }
      } else {
        final Class<?> enclosingClass = c.getEnclosingClass();
        final TypeMirror enclosingType = enclosingClass == null ? null : typeStubFrom(enclosingClass); // RECURSIVE

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

        t = new DefaultDeclaredType(enclosingType, typeArguments, List.of());
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

  private final DefaultDeclaredType typeStubFrom(final Annotation a) throws IllegalAccessException, InvocationTargetException {
    return (DefaultDeclaredType)typeStubFrom(a.annotationType());
  }

  private final DefaultExecutableType typeStubFrom(final Executable e) throws IllegalAccessException, InvocationTargetException {
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
                                List.of()); // don't think we want to use annotationMirrorsFrom(e)
  }

  private final List<AbstractElement> memberElementStubsFrom(final Class<?> c) {
    final ArrayList<AbstractElement> list = new ArrayList<>();
    try {
      for (final Class<?> memberClass : c.getDeclaredClasses()) {
        list.add(elementStubFrom(memberClass));
      }

      for (final Field field : c.getDeclaredFields()) {
        list.add(elementStubFrom(field));
      }

      for (final Constructor<?> constructor : c.getDeclaredConstructors()) {
        list.add(elementStubFrom(constructor));
      }

      for (final Method m : c.getDeclaredMethods()) {
        list.add(elementStubFrom(m));
      }
    } catch (final IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
    list.trimToSize();
    return Collections.unmodifiableList(list);
  }

}
