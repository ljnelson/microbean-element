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
import java.util.Objects;

import javax.lang.model.AnnotatedConstruct;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.Directive;
import javax.lang.model.element.ModuleElement.DirectiveKind;
import javax.lang.model.element.ModuleElement.ExportsDirective;
import javax.lang.model.element.ModuleElement.OpensDirective;
import javax.lang.model.element.ModuleElement.ProvidesDirective;
import javax.lang.model.element.ModuleElement.RequiresDirective;
import javax.lang.model.element.ModuleElement.UsesDirective;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;

@Deprecated
public final class Identity {

  private Identity() {
    super();
  }

  public static final int hashCode(final Object o, final boolean includeAnnotations) {
    if (o == null) {
      return 0;
    } else if (o instanceof AnnotationMirror am) {
      return hashCode(am, includeAnnotations);
    } else if (o instanceof AnnotationValue av) {
      return hashCode(av, includeAnnotations);
    } else if (o instanceof AnnotatedConstruct ac) {
      return hashCode(ac, includeAnnotations);
    } else if (o instanceof List<?> list) {
      return hashCode(list, includeAnnotations);
    } else if (o instanceof int[] hashCodes) {
      return hashCode(includeAnnotations, hashCodes);
    } else if (o instanceof Object[] array) {
      return hashCode(array, includeAnnotations);
    } else if (o instanceof Directive directive) {
      return hashCode(directive, includeAnnotations);
    } else {
      return o.hashCode();
    }
  }

  private static final int hashCode(final boolean includeAnnotations, final int... hashCodes) {
    if (hashCodes == null) {
      return 0;
    } else if (hashCodes.length <= 0) {
      return 1;
    }
    int result = 1;
    for (final int hashCode : hashCodes) {
      result = 31 * result + hashCode;
    }
    return result;
  }

  private static final int hashCode(final Object[] os, final boolean includeAnnotations) {
    if (os == null) {
      return 0;
    } else if (os.length <= 0) {
      return 1;
    }
    int result = 1;
    for (final Object o : os) {
      result = 31 * result + (o == null ? 0 : hashCode(o, includeAnnotations));
    }
    return result;
  }

  private static final int hashCode(final List<?> list, final boolean includeAnnotations) {
    if (list == null) {
      return 0;
    } else if (list.isEmpty()) {
      return 1;
    } else {
      // This calculation is mandated by java.util.List#hashCode().
      int hashCode = 1;
      for (final Object o : list) {
        hashCode = 31 * hashCode + (o == null ? 0 : hashCode(o, includeAnnotations));
      }
      return hashCode;
    }
  }
  
  static final int hashCode(final AnnotationMirror am,
                            final boolean includeAnnotations) {
    return am == null ? 0 :
      hashCode(am.getAnnotationType(), includeAnnotations);
  }

  static final int hashCode(final AnnotationValue av,
                            final boolean includeAnnotations) {
    if (av == null) {
      return 0;
    } else if (av instanceof AnnotationMirror am) {
      return hashCode(am, includeAnnotations);
    } else if (av instanceof List<?> list) {
      return hashCode(list, includeAnnotations);
    } else if (av instanceof TypeMirror tm) {
      return hashCode(tm, includeAnnotations);
    } else if (av instanceof VariableElement e) {
      return hashCode(e, includeAnnotations);
    } else {
      return av.hashCode();
    }
  }


  static final int hashCode(final AnnotatedConstruct ac, final boolean includeAnnotations) {
    if (ac == null) {
      return 0;
    } else if (ac instanceof Element e) {
      return hashCode(e, includeAnnotations);
    } else if (ac instanceof TypeMirror tm) {
      return hashCode(tm, includeAnnotations);
    } else {
      return ac.hashCode();
    }
  }

  static final int hashCode(final Element e, final boolean includeAnnotations) {
    if (e == null) {
      return 0;
    }
    final int hc;
    switch (e.getKind()) {
    case ANNOTATION_TYPE:
    case CLASS:
    case ENUM:
    case INTERFACE:
    case RECORD:
      hc = hashCode((TypeElement)e, includeAnnotations);
      break;
    case TYPE_PARAMETER:
      hc = hashCode((TypeParameterElement)e, includeAnnotations);
      break;
    case BINDING_VARIABLE:
    case ENUM_CONSTANT:
    case EXCEPTION_PARAMETER:
    case FIELD:
    case LOCAL_VARIABLE:
    case PARAMETER:
    case RESOURCE_VARIABLE:
      hc = hashCode((VariableElement)e, includeAnnotations);
      break;
    case RECORD_COMPONENT:
      hc = hashCode((RecordComponentElement)e, includeAnnotations);
      break;
    case CONSTRUCTOR:
    case INSTANCE_INIT:
    case METHOD:
    case STATIC_INIT:
      hc = hashCode((ExecutableElement)e, includeAnnotations);
      break;
    case PACKAGE:
      hc = hashCode((PackageElement)e, includeAnnotations);
      break;
    case MODULE:
      hc = hashCode((ModuleElement)e, includeAnnotations);
      break;
    case OTHER:
    default:
      hc = System.identityHashCode(e);
    }
    return
      hashCode(includeAnnotations,
               hashCode(e.asType(), includeAnnotations),
               includeAnnotations ? hashCode(e.getAnnotationMirrors(), includeAnnotations) : 0,
               hashCode(e.getEnclosingElement(), includeAnnotations),
               e.getKind().hashCode(),
               e.getModifiers().hashCode(),
               hashCode(e.getSimpleName()),
               hc);
  }

  static final int hashCode(final TypeMirror t, final boolean includeAnnotations) {
    if (t == null) {
      return 0;
    }
    final int hc;
    switch(t.getKind()) {
    case ARRAY:
      hc = hashCode((ArrayType)t, includeAnnotations);
      break;
    case DECLARED:
    case ERROR:
      hc = hashCode((DeclaredType)t, includeAnnotations);
      break;
    case EXECUTABLE:
      hc = hashCode((ExecutableType)t, includeAnnotations);
      break;
    case INTERSECTION:
      hc = hashCode((IntersectionType)t, includeAnnotations);
      break;
    case MODULE:
    case NONE:
    case PACKAGE:
    case VOID:
      hc = hashCode((NoType)t, includeAnnotations);
      break;
    case NULL:
      hc = hashCode((NullType)t, includeAnnotations);
      break;
    case BOOLEAN:
    case BYTE:
    case CHAR:
    case DOUBLE:
    case FLOAT:
    case INT:
    case LONG:
    case SHORT:
      hc = hashCode((PrimitiveType)t, includeAnnotations);
      break;
    case TYPEVAR:
      hc = hashCode((TypeVariable)t, includeAnnotations);
      break;
    case UNION:
      hc = hashCode((UnionType)t, includeAnnotations);
      break;
    case WILDCARD:
      hc = hashCode((WildcardType)t, includeAnnotations);
      break;
    case OTHER:
    default:
      hc = System.identityHashCode(t);
    }
    return
      hashCode(includeAnnotations,
               includeAnnotations ? hashCode(t.getAnnotationMirrors(), includeAnnotations) : 0,
               t.getKind().hashCode(),
               hc);
  }

  static final int hashCode(final Directive d, final boolean includeAnnotations) {
    if (d == null) {
      return 0;
    }
    switch (d.getKind()) {
    case EXPORTS:
      return hashCode((ExportsDirective)d, includeAnnotations);
    case OPENS:
      return hashCode((OpensDirective)d, includeAnnotations);
    case PROVIDES:
      return hashCode((ProvidesDirective)d, includeAnnotations);
    case REQUIRES:
      return hashCode((RequiresDirective)d, includeAnnotations);
    case USES:
      return hashCode((UsesDirective)d, includeAnnotations);
    default:
      return System.identityHashCode(d);
    }
  }

  private static final int hashCode(final ExportsDirective d, final boolean includeAnnotations) {
    return d == null ? 0 :
      hashCode(includeAnnotations,
               d.getKind().hashCode(),
               hashCode(d.getPackage(), includeAnnotations),
               hashCode(d.getTargetModules(), includeAnnotations));
  }

  private static final int hashCode(final OpensDirective d, final boolean includeAnnotations) {
    return d == null ? 0 :
      hashCode(includeAnnotations,
               d.getKind().hashCode(),
               hashCode(d.getPackage(), includeAnnotations),
               hashCode(d.getTargetModules(), includeAnnotations));
  }

  private static final int hashCode(final ProvidesDirective d, final boolean includeAnnotations) {
    return d == null ? 0 :
      hashCode(includeAnnotations,
               d.getKind().hashCode(),
               hashCode(d.getImplementations(), includeAnnotations),
               hashCode(d.getService(), includeAnnotations));
  }

  private static final int hashCode(final RequiresDirective d, final boolean includeAnnotations) {
    return d == null ? 0 :
      hashCode(includeAnnotations,
               d.getKind().hashCode(),
               hashCode(d.getDependency(), includeAnnotations),
               d.isStatic() ? 1 : 0,
               d.isTransitive() ? 1 : 0);
  }

  private static final int hashCode(final UsesDirective d, final boolean includeAnnotations) {
    return
      hashCode(includeAnnotations,
               d.getKind().hashCode(),
               hashCode(d.getService(), includeAnnotations));
  }

  private static final int hashCode(final ArrayType t, final boolean includeAnnotations) {
    return t == null ? 0 :
      hashCode(includeAnnotations,
               hashCode(t.getComponentType(), includeAnnotations));
  }

  private static final int hashCode(final DeclaredType t, final boolean includeAnnotations) {
    return t == null ? 0 :
      hashCode(includeAnnotations,
               hashCode(t.getEnclosingType(), includeAnnotations),
               hashCode(t.getTypeArguments(), includeAnnotations));
  }

  /*
  private static final int hashCode(final ErrorType t, final boolean includeAnnotations) {
    return hashCode((DeclaredType)t, includeAnnotations);
  }
  */

  private static final int hashCode(final ExecutableType t, final boolean includeAnnotations) {
    return t == null ? 0 :
      hashCode(includeAnnotations,
               hashCode(t.getParameterTypes(), includeAnnotations),
               hashCode(t.getReceiverType(), includeAnnotations),
               hashCode(t.getReturnType(), includeAnnotations),
               hashCode(t.getThrownTypes(), includeAnnotations),
               hashCode(t.getTypeVariables(), includeAnnotations));
  }

  private static final int hashCode(final IntersectionType t, final boolean includeAnnotations) {
    return t == null ? 0 :
      hashCode(includeAnnotations,
               hashCode(t.getBounds(), includeAnnotations));
  }

  private static final int hashCode(final NoType t, final boolean includeAnnotations) {
    return 0;
  }

  private static final int hashCode(final NullType t, final boolean includeAnnotations) {
    return 0;
  }

  private static final int hashCode(final PrimitiveType t, final boolean includeAnnotations) {
    return 0;
  }

  private static final int hashCode(final TypeVariable t, final boolean includeAnnotations) {
    return
      hashCode(includeAnnotations,
               hashCode(t.getLowerBound(), includeAnnotations),
               hashCode(t.getUpperBound(), includeAnnotations));
  }

  private static final int hashCode(final UnionType t, final boolean includeAnnotations) {
    return t == null ? 0 :
      hashCode(includeAnnotations,
               hashCode(t.getAlternatives(), includeAnnotations));
  }

  private static final int hashCode(final WildcardType t, final boolean includeAnnotations) {
    return t == null ? 0 :
      hashCode(includeAnnotations,
               hashCode(t.getExtendsBound(), includeAnnotations),
               hashCode(t.getSuperBound(), includeAnnotations));
  }

  static final int hashCode(final Name name) {
    return name == null ? 0 : name.toString().hashCode();
  }

  private static final int hashCode(final ExecutableElement e, final boolean includeAnnotations) {
    return
      hashCode(includeAnnotations,
               hashCode(e.getDefaultValue(), includeAnnotations),
               hashCode(e.getParameters(), includeAnnotations),
               hashCode(e.getTypeParameters(), includeAnnotations),
               e.isDefault() ? 1 : 0,
               e.isVarArgs() ? 1 : 0);
  }

  private static final int hashCode(final ModuleElement e, final boolean includeAnnotations) {
    return
      hashCode(includeAnnotations,
               hashCode(e.getDirectives(), includeAnnotations),
               e.isOpen() ? 1 : 0,
               e.isUnnamed() ? 1 : 0);
  }

  private static final int hashCode(final PackageElement e, final boolean includeAnnotations) {
    return e.isUnnamed() ? 1 : 0;
  }

  private static final int hashCode(final RecordComponentElement e, final boolean includeAnnotations) {
    return hashCode(e.getAccessor(), includeAnnotations);
  }

  private static final int hashCode(final TypeElement e, final boolean includeAnnotations) {
    return
      hashCode(includeAnnotations,
               hashCode(e.getInterfaces(), includeAnnotations),
               e.getNestingKind().hashCode(),
               hashCode(e.getPermittedSubclasses(), includeAnnotations),
               hashCode(e.getRecordComponents(), includeAnnotations),
               hashCode(e.getSuperclass(), includeAnnotations),
               hashCode(e.getTypeParameters(), includeAnnotations));
  }

  private static final int hashCode(final TypeParameterElement e, final boolean includeAnnotations) {
    return
      hashCode(includeAnnotations,
               hashCode(e.getBounds(), includeAnnotations));
  }

  private static final int hashCode(final VariableElement e, final boolean includeAnnotations) {
    final Object constantValue = e.getConstantValue();
    return constantValue == null ? 0 : constantValue.hashCode();
  }

  public static final boolean identical(final Object o1, final Object o2, final boolean includeAnnotations) {
    if (o1 == o2) {
      return true;
    } else if (o1 == null || o2 == null) {
      return false;
    } else if (o1 instanceof AnnotationMirror am1) {
      if (o2 instanceof AnnotationMirror am2) {
        return identical(am1, am2, includeAnnotations);
      }
    } else if (o1 instanceof AnnotationValue av1) {
      if (o2 instanceof AnnotationValue av2) {
        return identical(av1, av2, includeAnnotations);
      }
    } else if (o1 instanceof AnnotatedConstruct ac1) {
      if (o2 instanceof AnnotatedConstruct ac2) {
        return identical(ac1, ac2, includeAnnotations);
      }
    } else if (o1 instanceof List<?> list1) {
      if (o2 instanceof List<?> list2) {
        return identical(list1, list2, includeAnnotations);
      }
    } else if (o1 instanceof Directive d1) {
      if (o2 instanceof Directive d2) {
        return identical(d1, d2, includeAnnotations);
      }
    }
    return false;
  }

  private static final boolean identical(final AnnotatedConstruct ac1, final AnnotatedConstruct ac2, final boolean includeAnnotations) {
    if (ac1 == ac2) {
      return true;
    } else if (ac1 == null || ac2 == null) {
      return false;
    } else if (ac1 instanceof Element e1) {
      if (ac2 instanceof Element e2) {
        return identical(e1, e2, includeAnnotations);
      }
    } else if (ac1 instanceof TypeMirror t1) {
      if (ac2 instanceof TypeMirror t2) {
        return identical(t1, t2, includeAnnotations);
      }
    }
    return false;
  }

  static final boolean identical(final Element e1, final Element e2, final boolean includeAnnotations) {
    if (e1 == e2) {
      return true;
    } else if (e1 == null || e2 == null) {
      return false;
    }
    final ElementKind k = e1.getKind();
    if (k != e2.getKind()) {
      return false;
    }
    if (!identical(e1.asType(), e2.asType(), includeAnnotations)) {
      return false;
    }
    if (!identical(e1.getEnclosingElement(), e2.getEnclosingElement(), includeAnnotations)) {
      return false;
    }
    if (e1.getModifiers().equals(e2.getModifiers())) {
      return false;
    }
    if (includeAnnotations && !identical(e1.getAnnotationMirrors(), e2.getAnnotationMirrors(), includeAnnotations)) {
      return false;
    }
    switch (k) {
    case ANNOTATION_TYPE:
    case CLASS:
    case ENUM:
    case INTERFACE:
    case RECORD:
      return identical((TypeElement)e1, (TypeElement)e2, includeAnnotations);
    case TYPE_PARAMETER:
      return identical((TypeParameterElement)e1, (TypeParameterElement)e2, includeAnnotations);
    case BINDING_VARIABLE:
    case ENUM_CONSTANT:
    case EXCEPTION_PARAMETER:
    case FIELD:
    case LOCAL_VARIABLE:
    case PARAMETER:
    case RESOURCE_VARIABLE:
      return identical((VariableElement)e1, (VariableElement)e2, includeAnnotations);
    case RECORD_COMPONENT:
      return identical((RecordComponentElement)e1, (RecordComponentElement)e2, includeAnnotations);
    case CONSTRUCTOR:
    case INSTANCE_INIT:
    case METHOD:
    case STATIC_INIT:
      return identical((ExecutableElement)e1, (ExecutableElement)e2, includeAnnotations);
    case PACKAGE:
      return identical((PackageElement)e1, (PackageElement)e2, includeAnnotations);
    case MODULE:
      return identical((ModuleElement)e1, (ModuleElement)e2, includeAnnotations);
    case OTHER:
    default:
      return false;
    }
  }

  private static final boolean identical(final AnnotationMirror a1, final AnnotationMirror a2, final boolean includeAnnotations) {
    if (a1 == a2) {
      return true;
    } else if (a1 == null || a2 == null) {
      return false;
    } else if (!identical(a1.getAnnotationType(), a2.getAnnotationType(), includeAnnotations)) {
      return false;
    }
    return false;
  }

  private static final boolean identical(final List<?> list1, final List<?> list2, final boolean includeAnnotations) {
    if (list1 == null || list2 == null) {
      return false;
    } else if (list1 == list2) {
      return true;
    }
    final int size = list1.size();
    if (size != list2.size()) {
      return false;
    }
    for (int i = 0; i < size; i++) {
      if (!identical(list1.get(i), list2.get(i), includeAnnotations)) {
        return false;
      }
    }
    return true;
  }

  @SuppressWarnings("unchecked")
  private static final boolean identical(final AnnotationValue a1, final AnnotationValue a2, final boolean includeAnnotations) {
    if (a1 == a2) {
      return true;
    } else if (a1 == null || a2 == null) {
      return false;
    }
    final Object v1 = a1.getValue();
    final Object v2 = a2.getValue();
    if (v1 instanceof AnnotationMirror am1) {
      if (v2 instanceof AnnotationMirror am2) {
        return identical(am1, am2, includeAnnotations);
      }
    } else if (v1 instanceof List<?> list1) {
      if (v2 instanceof List<?> list2) {
        return identical((List<? extends AnnotationValue>)list1, (List<? extends AnnotationValue>)list2, includeAnnotations);
      }
    } else if (v1 instanceof TypeMirror t1) {
      if (v2 instanceof TypeMirror t2) {
        return identical(t1, t2, includeAnnotations);
      }
    } else if (v1 instanceof VariableElement e1) {
      if (v2 instanceof VariableElement e2) {
        return identical(e1, e2, includeAnnotations);
      }
    }
    return v1.equals(v2);
  }

  private static final boolean identical(final Directive d1, final Directive d2, final boolean includeAnnotations) {
    final DirectiveKind k = d1.getKind();
    if (d2.getKind() != k) {
      return false;
    }
    switch (k) {
    case EXPORTS:
      return identical((ExportsDirective)d1, (ExportsDirective)d2, includeAnnotations);
    case OPENS:
      return identical((OpensDirective)d1, (OpensDirective)d2, includeAnnotations);
    case PROVIDES:
      return identical((ProvidesDirective)d1, (ProvidesDirective)d2, includeAnnotations);
    case REQUIRES:
      return identical((RequiresDirective)d1, (RequiresDirective)d2, includeAnnotations);
    case USES:
      return identical((UsesDirective)d1, (UsesDirective)d2, includeAnnotations);
    default:
      throw new IllegalArgumentException("d1: " + d1);
    }
  }

  private static final boolean identical(final ExportsDirective d1, final ExportsDirective d2, final boolean includeAnnotations) {
    return
      identical(d1.getPackage(), d2.getPackage(), includeAnnotations) &&
      identical(d1.getTargetModules(), d2.getTargetModules(), includeAnnotations);
  }

  private static final boolean identical(final OpensDirective d1, final OpensDirective d2, final boolean includeAnnotations) {
    return
      identical(d1.getPackage(), d2.getPackage(), includeAnnotations) &&
      identical(d1.getTargetModules(), d2.getTargetModules(), includeAnnotations);
  }

  private static final boolean identical(final ProvidesDirective d1, final ProvidesDirective d2, final boolean includeAnnotations) {
    return
      identical(d1.getImplementations(), d2.getImplementations(), includeAnnotations) &&
      identical(d1.getService(), d2.getService(), includeAnnotations);
  }

  private static final boolean identical(final RequiresDirective d1, final RequiresDirective d2, final boolean includeAnnotations) {
    return
      identical(d1.getDependency(), d2.getDependency(), includeAnnotations) &&
      d1.isStatic() && d2.isStatic() &&
      d1.isTransitive() && d2.isTransitive();
  }

  private static final boolean identical(final UsesDirective d1, final UsesDirective d2, final boolean includeAnnotations) {
    return
      identical(d1.getService(), d2.getService(), includeAnnotations);
  }

  // When checking TypeMirrors, do NOT include asElement() EXCEPT perhaps to check reference identity.
  static final boolean identical(final TypeMirror t1, final TypeMirror t2, final boolean includeAnnotations) {
    if (t1 == t2) {
      return true;
    } else if (t1 == null || t2 == null) {
      return false;
    }
    final TypeKind k = t1.getKind();
    final TypeKind k2 = t2.getKind();
    if (k != k2) {
      return false;
    }
    if (includeAnnotations && !identical(t1.getAnnotationMirrors(), t2.getAnnotationMirrors(), includeAnnotations)) {
      return false;
    }
    switch (k) {
    case ARRAY:
      return identical((ArrayType)t1, (ArrayType)t2, includeAnnotations);
    case DECLARED:
    case ERROR:
      return identical((DeclaredType)t1, (DeclaredType)t2, includeAnnotations);
    case EXECUTABLE:
      return identical((ExecutableType)t1, (ExecutableType)t2, includeAnnotations);
    case INTERSECTION:
      return identical((IntersectionType)t1, (IntersectionType)t2, includeAnnotations);
    case MODULE:
    case NONE:
    case PACKAGE:
    case VOID:
      return identical((NoType)t1, (NoType)t2, includeAnnotations);
    case NULL:
      return identical((NullType)t1, (NullType)t2, includeAnnotations);
    case BOOLEAN:
    case BYTE:
    case CHAR:
    case DOUBLE:
    case FLOAT:
    case INT:
    case LONG:
    case SHORT:
      return identical((PrimitiveType)t1, (PrimitiveType)t2, includeAnnotations);
    case TYPEVAR:
      return identical((TypeVariable)t1, (TypeVariable)t2, includeAnnotations);
    case UNION:
      return identical((UnionType)t1, (UnionType)t2, includeAnnotations);
    case WILDCARD:
      return identical((WildcardType)t1, (WildcardType)t2, includeAnnotations);
    case OTHER:
    default:
      return false;
    }
  }


  /*
   * In these methods, identity (==) and nulls (forbidden) have been
   * taken care of already.
   *
   * The enclosing element, if any, has been taken care of already.
   */


  private static final boolean identical(final Name n, final CharSequence cs2) {
    return n.contentEquals(cs2);
  }

  private static final boolean identical(final TypeElement e1,
                                         final TypeElement e2,
                                         final boolean includeAnnotations) {
    return
      // identical(e1.asType(), e2.asType(), includeAnnotations) &&
      identical(e1.getInterfaces(), e2.getInterfaces(), includeAnnotations) &&
      e1.getNestingKind() == e2.getNestingKind() &&
      identical(e1.getPermittedSubclasses(), e2.getPermittedSubclasses(), includeAnnotations) &&
      identical(e1.getRecordComponents(), e2.getRecordComponents(), includeAnnotations) &&
      identical(e1.getSimpleName(), e2.getSimpleName()) &&
      identical(e1.getSuperclass(), e2.getSuperclass(), includeAnnotations) &&
      identical(e1.getTypeParameters(), e2.getTypeParameters(), includeAnnotations);
  }

  private static final boolean identical(final TypeParameterElement e1,
                                         final TypeParameterElement e2,
                                         final boolean includeAnnotations) {
    return
      // identical(e1.asType(), e2.asType(), includeAnnotations) &&
      identical(e1.getBounds(), e2.getBounds(), includeAnnotations) &&
      identical(e1.getSimpleName(), e2.getSimpleName(), includeAnnotations); // I think?
  }

  private static final boolean identical(final VariableElement e1,
                                         final VariableElement e2,
                                         final boolean includeAnnotations) {
    return
      // identical(e1.asType(), e2.asType(), includeAnnotations) &&
      identical(e1.getConstantValue(), e2.getConstantValue(), includeAnnotations) &&
      identical(e1.getSimpleName(), e2.getSimpleName());
  }

  private static final boolean identical(final RecordComponentElement e1,
                                         final RecordComponentElement e2,
                                         final boolean includeAnnotations) {
    return
      // identical(e1.asType(), e2.asType(), includeAnnotations) &&
      identical(e1.getAccessor(), e2.getAccessor(), includeAnnotations) &&
      identical(e1.getSimpleName(), e2.getSimpleName());
  }

  private static final boolean identical(final ExecutableElement e1,
                                         final ExecutableElement e2,
                                         final boolean includeAnnotations) {
    return
      // identical(e1.asType(), e2.asType(), includeAnnotations) &&
      identical(e1.getDefaultValue(), e2.getDefaultValue(), includeAnnotations) &&
      identical(e1.getParameters(), e2.getParameters(), includeAnnotations) &&
      identical(e1.getSimpleName(), e2.getSimpleName()) &&
      identical(e1.getTypeParameters(), e2.getTypeParameters(), includeAnnotations) &&
      e1.isDefault() && e2.isDefault() &&
      e1.isVarArgs() && e2.isVarArgs();
  }

  private static final boolean identical(final PackageElement e1,
                                         final PackageElement e2,
                                         final boolean includeAnnotations) {
    return
      // identical(e1.asType(), e2.asType(), includeAnnotations) &&
      identical(e1.getSimpleName(), e2.getSimpleName(), includeAnnotations) &&
      e1.isUnnamed() && e2.isUnnamed();
  }

  private static final boolean identical(final ModuleElement e1,
                                         final ModuleElement e2,
                                         final boolean includeAnnotations) {
    return
      // identical(e1.asType(), e2.asType(), includeAnnotations) &&
      identical(e1.getDirectives(), e2.getDirectives(), includeAnnotations) &&
      identical(e1.getQualifiedName(), e2.getQualifiedName()) &&
      e1.isOpen() && e2.isOpen() &&
      e1.isUnnamed() && e2.isUnnamed();
  }

  private static final boolean identical(final ArrayType t1,
                                         final ArrayType t2,
                                         final boolean includeAnnotations) {
    return
      identical(t1.getComponentType(), t2.getComponentType(), includeAnnotations);
  }

  private static final boolean identical(final DeclaredType t1,
                                         final DeclaredType t2,
                                         final boolean includeAnnotations) {
    return
      identical(t1.getTypeArguments(), t2.getTypeArguments(), includeAnnotations);
  }

  private static final boolean identical(final ExecutableType t1,
                                         final ExecutableType t2,
                                         final boolean includeAnnotations) {
    return
      identical(t1.getParameterTypes(), t2.getParameterTypes(), includeAnnotations) &&
      identical(t1.getReceiverType(), t2.getReceiverType(), includeAnnotations) &&
      identical(t1.getReturnType(), t2.getReturnType(), includeAnnotations) &&
      identical(t1.getThrownTypes(), t2.getThrownTypes(), includeAnnotations) &&
      identical(t1.getTypeVariables(), t2.getTypeVariables(), includeAnnotations);
  }

  private static final boolean identical(final IntersectionType t1,
                                         final IntersectionType t2,
                                         final boolean includeAnnotations) {
    return
      identical(t1.getBounds(), t2.getBounds(), includeAnnotations);
  }

  private static final boolean identical(final NoType t1,
                                         final NoType t2,
                                         final boolean includeAnnotations) {
    return
      t1.getKind() == t2.getKind(); // already checked
  }

  private static final boolean identical(final NullType t1,
                                         final NullType t2,
                                         final boolean includeAnnotations) {
    return
      t1.getKind() == t2.getKind(); // already checked
  }

  private static final boolean identical(final PrimitiveType t1,
                                         final PrimitiveType t2,
                                         final boolean includeAnnotations) {
    return
      t1.getKind() == t2.getKind(); // already checked
  }

  private static final boolean identical(final TypeVariable t1,
                                         final TypeVariable t2,
                                         final boolean includeAnnotations) {
    return
      identical(t1.getLowerBound(), t2.getLowerBound(), includeAnnotations) &&
      identical(t1.getUpperBound(), t2.getUpperBound(), includeAnnotations);
  }

  private static final boolean identical(final UnionType t1,
                                         final UnionType t2,
                                         final boolean includeAnnotations) {
    return
      identical(t1.getAlternatives(), t2.getAlternatives(), includeAnnotations);
  }

  private static final boolean identical(final WildcardType t1,
                                         final WildcardType t2,
                                         final boolean includeAnnotations) {
    return
      identical(t1.getExtendsBound(), t2.getExtendsBound(), includeAnnotations) &&
      identical(t1.getSuperBound(), t2.getSuperBound(), includeAnnotations);
  }

}
