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
import java.util.Map.Entry;
import java.util.Map;
import java.util.TreeMap;

import javax.lang.model.AnnotatedConstruct;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement.Directive;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

final class Equality {

  private Equality() {
    super();
  }

  static final boolean equals(final Object o1, final Object o2, final boolean ia) {
    if (o1 == o2) {
      return true;
    } else if (o1 == null || o2 == null) {
      return false;
    } else if (o1 instanceof AnnotationMirror am1) {
      return o2 instanceof AnnotationMirror am2 && equals(am1, am2, ia);
    } else if (o1 instanceof AnnotationValue av1) {
      return o2 instanceof AnnotationValue av2 && equals(av1, av2, ia);
    } else if (o1 instanceof AnnotatedConstruct ac1) {
      return o2 instanceof AnnotatedConstruct ac2 && equals(ac1, ac2, ia);
    } else if (o1 instanceof List<?> list1) {
      return o2 instanceof List<?> list2 && equals(list1, list2, ia);
    } else if (o1 instanceof Directive d1) {
      return o2 instanceof Directive d2 && equals(d1, d2, ia);
    } else {
      return o1.equals(o2);
    }
  }

  static final boolean equals(final List<?> list1, final List<?> list2, final boolean ia) {
    if (list1 == list2) {
      return true;
    } else if (list1 == null || list2 == null) {
      return false;
    }
    final int size = list1.size();
    if (size != list2.size()) {
      return false;
    }
    for (int i = 0; i < size; i++) {
      if (!equals(list1.get(i), list2.get(i), ia)) {
        return false;
      }
    }
    return true;
  }

  static final boolean equals(final CharSequence c1, final CharSequence c2) {
    if (c1 == c2) {
      return true;
    } else if (c1 == null || c2 == null) {
      return false;
    } else if (c1 instanceof Name n1) {
      return n1.contentEquals(c2);
    } else if (c2 instanceof Name n2) {
      return n2.contentEquals(c1);
    } else {
      return c1.equals(c2);
    }
  }

  static final boolean equals(final AnnotationMirror am1, final AnnotationMirror am2, final boolean ia) {
    if (am1 == am2) {
      return true;
    } else if (am1 == null || am2 == null) {
      return false;
    }
    final DeclaredType t1 = am1.getAnnotationType();
    final DeclaredType t2 = am2.getAnnotationType();
    if (!equals(t1, t2, ia)) {
      return false;
    }

    // Make maps of the default values, if any.
    //
    // Because we know we're working with annotation interfaces, there
    // won't be method overloads because there are no method
    // parameters.  ExecutableElements in this case reduce to their
    // names only.  That's convenient for sorting.
    final Map<String, AnnotationValue> map1 = new TreeMap<>();
    final Map<String, AnnotationValue> map2 = new TreeMap<>();

    final List<? extends Element> enclosedElements1 = t1.asElement().getEnclosedElements();
    final List<? extends Element> enclosedElements2 = t2.asElement().getEnclosedElements();
    final int size = enclosedElements1.size();
    assert enclosedElements2.size() == size;

    for (int i = 0; i < size; i++) {
      if (enclosedElements1.get(i) instanceof ExecutableElement ee1) {
        final AnnotationValue dv1 = ee1.getDefaultValue();
        if (dv1 != null) {
          map1.put(ee1.getSimpleName().toString(), dv1);
        }
      }
      if (enclosedElements2.get(i) instanceof ExecutableElement ee2) {
        final AnnotationValue dv2 = ee2.getDefaultValue();
        if (dv2 != null) {
          map2.put(ee2.getSimpleName().toString(), dv2);
        }
      }
    }

    // Override default values as needed.
    for (final Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am1.getElementValues().entrySet()) {
      map1.put(entry.getKey().getSimpleName().toString(), entry.getValue());
    }
    for (final Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am2.getElementValues().entrySet()) {
      map2.put(entry.getKey().getSimpleName().toString(), entry.getValue());
    }

    // Think hard, but the map sizes should be the same, because the
    // annotation types are equal, so their ExecutableElements must be
    // equal in number and kind.
    assert map1.size() == map2.size();

    // Compare values as simple Lists.
    return equals(List.of(map1.values()), List.of(map2.values()), ia);
  }

  static final boolean equals(final AnnotationValue av1, final AnnotationValue av2, final boolean ia) {
    if (av1 == av2) {
      return true;
    } else if (av1 == null || av2 == null) {
      return false;
    }
    final Object v1 = av1.getValue(); // won't be null
    final Object v2 = av2.getValue(); // won't be null
    if (av1 instanceof AnnotationMirror am1) {
      return av2.getValue() instanceof AnnotationMirror am2 && equals(am1, am2, ia);
    } else if (av1 instanceof List<?> list1) {
      return av2.getValue() instanceof List<?> list2 && equals(list1, list2, ia);
    } else if (av1 instanceof TypeMirror t1) {
      return av2.getValue() instanceof TypeMirror t2 && equals(t1, t2, ia);
    } else if (v1 instanceof VariableElement ve1) {
      return av2.getValue() instanceof VariableElement ve2 && equals(ve1, ve2, ia);
    } else {
      return v1.equals(v2);
    }
  }

  static final boolean equals(final AnnotatedConstruct ac1, final AnnotatedConstruct ac2, final boolean ia) {
    if (ac1 == ac2) {
      return true;
    } else if (ac1 == null || ac2 == null) {
      return false;
    } else if (ac1 instanceof Element e1) {
      return ac2 instanceof Element e2 && equals(e1, e2, ia);
    } else if (ac1 instanceof TypeMirror t1) {
      return ac2 instanceof TypeMirror t2 && equals(t1, t2, ia);
    } else {
      return false;
    }
  }

  static final boolean equals(final Element e1, final Element e2, final boolean ia) {
    if (e1 == e2) {
      return true;
    } else if (e1 == null || e2 == null) {
      return false;
    }
    final ElementKind k = e1.getKind();
    if (k != e2.getKind()) {
      return false;
    }
    switch (k) {

    case ANNOTATION_TYPE:
    case CLASS:
    case ENUM:
    case INTERFACE:
    case RECORD:
      return equals((TypeElement)e1, (TypeElement)e2, ia);

    case TYPE_PARAMETER:
      return equals((TypeParameterElement)e1, (TypeParameterElement)e2, ia);

    case BINDING_VARIABLE:
    case ENUM_CONSTANT:
    case EXCEPTION_PARAMETER:
    case FIELD:
    case LOCAL_VARIABLE:
    case PARAMETER:
    case RESOURCE_VARIABLE:
      return equals((VariableElement)e1, (VariableElement)e2, ia);

    case RECORD_COMPONENT:
      return equals((RecordComponentElement)e1, (RecordComponentElement)e2, ia);

    case CONSTRUCTOR:
    case INSTANCE_INIT:
    case METHOD:
    case STATIC_INIT:
      return equals((ExecutableElement)e1, (ExecutableElement)e2, ia);

    case PACKAGE:
      return equals((PackageElement)e1, (PackageElement)e2, ia);

    case MODULE:
      return equals((ModuleElement)e1, (ModuleElement)e2, ia);

    case OTHER:
    default:
      return false;
    }
  }

  static final boolean equals(final ExecutableElement e1, final ExecutableElement e2, final boolean ia) {
    if (e1 == e2) {
      return true;
    } else if (e1 == null || e2 == null) {
      return false;
    }
    switch (e1.getKind()) {
    case CONSTRUCTOR:
      if (e2.getKind() != ElementKind.CONSTRUCTOR) {
        return false;
      }
      break;
    case INSTANCE_INIT:
      if (e2.getKind() != ElementKind.CONSTRUCTOR) {
        return false;
      }
      break;
    case METHOD:
      if (e2.getKind() != ElementKind.CONSTRUCTOR) {
        return false;
      }
      break;
    case STATIC_INIT:
      if (e2.getKind() != ElementKind.CONSTRUCTOR) {
        return false;
      }
      break;
    default:
      return false; // illegal argument
    }
    // This is kind of the runtime equality contract of, say,
    // java.lang.reflect.Method.  Note in particular that
    // TypeParameterElements are not evaluated.
    return
      equals(e1.getEnclosingElement(), e2.getEnclosingElement(), ia) &&
      equals(e1.getSimpleName(), e2.getSimpleName()) &&
      equals(e1.getParameters(), e2.getParameters(), ia) &&
      equals(e1.getReturnType(), e2.getReturnType(), ia);
  }

  static final boolean equals(final ModuleElement e1, final ModuleElement e2, final boolean ia) {
    if (e1 == e2) {
      return true;
    } else if (e1 == null || e2 == null) {
      return false;
    }
    return
      e1.getKind() == ElementKind.MODULE && e2.getKind() == ElementKind.MODULE &&
      equals(e1.getQualifiedName(), e2.getQualifiedName());
  }

  static final boolean equals(final PackageElement e1, final PackageElement e2, final boolean ia) {
    if (e1 == e2) {
      return true;
    } else if (e1 == null || e2 == null) {
      return false;
    }
    return
      e1.getKind() == ElementKind.PACKAGE && e2.getKind() == ElementKind.PACKAGE &&
      equals(e1.getQualifiedName(), e2.getQualifiedName());
  }

  static final boolean equals(final RecordComponentElement r1, final RecordComponentElement r2, final boolean ia) {
    if (r1 == r2) {
      return true;
    } else if (r1 == null || r2 == null) {
      return false;
    }
    return
      r1.getKind() == ElementKind.RECORD_COMPONENT && r2.getKind() == ElementKind.RECORD_COMPONENT &&
      equals(r1.getSimpleName(), r2.getSimpleName()) &&
      equals(r1.getEnclosingElement(), r2.getEnclosingElement(), ia);
  }

  static final boolean equals(final TypeElement t1, final TypeElement t2, final boolean ia) {
    if (t1 == t2) {
      return true;
    } else if (t1 == null || t2 == null) {
      return false;
    }
    switch (t1.getKind()) {
    case ANNOTATION_TYPE:
      if (t2.getKind() != ElementKind.ANNOTATION_TYPE) {
        return false;
      }
      break;
    case CLASS:
            if (t2.getKind() != ElementKind.ANNOTATION_TYPE) {
        return false;
      }
      break;
    case ENUM:
            if (t2.getKind() != ElementKind.ANNOTATION_TYPE) {
        return false;
      }
      break;
    case INTERFACE:
            if (t2.getKind() != ElementKind.ANNOTATION_TYPE) {
        return false;
      }
      break;
    case RECORD:
            if (t2.getKind() != ElementKind.ANNOTATION_TYPE) {
        return false;
      }
      break;
    default:
      return false; // illegal argument
    }
    return equals(t1.getQualifiedName(), t2.getQualifiedName());
  }

  static final boolean equals(final TypeParameterElement t1, final TypeParameterElement t2, final boolean ia) {
    if (t1 == t2) {
      return true;
    } else if (t1 == null || t2 == null) {
      return false;
    }
    // This is the equality contract of
    // sun.reflect.generics.reflectiveObjects.TypeVariableImpl.
    return
      t1.getKind() == ElementKind.TYPE_PARAMETER && t2.getKind() == ElementKind.TYPE_PARAMETER &&
      equals(t1.getGenericElement(), t2.getGenericElement(), ia) &&
      equals(t1.getSimpleName(), t2.getSimpleName());
  }

  static final boolean equals(final VariableElement v1, final VariableElement v2, final boolean ia) {
    if (v1 == v2) {
      return true;
    } else if (v1 == null || v2 == null) {
      return false;
    }
    switch (v1.getKind()) {
    case BINDING_VARIABLE:
      if (v2.getKind() != ElementKind.BINDING_VARIABLE) {
        return false;
      }
      break;
    case ENUM_CONSTANT:
      if (v2.getKind() != ElementKind.ENUM_CONSTANT) {
        return false;
      }
      break;
    case EXCEPTION_PARAMETER:
      if (v2.getKind() != ElementKind.EXCEPTION_PARAMETER) {
        return false;
      }
      break;
    case FIELD:
      if (v2.getKind() != ElementKind.FIELD) {
        return false;
      }
      break;
    case LOCAL_VARIABLE:
      if (v2.getKind() != ElementKind.LOCAL_VARIABLE) {
        return false;
      }
      break;
    case PARAMETER:
      if (v2.getKind() != ElementKind.PARAMETER) {
        return false;
      }
      break;
    case RESOURCE_VARIABLE:
      if (v2.getKind() != ElementKind.RESOURCE_VARIABLE) {
        return false;
      }
      break;
    default:
      return false; // illegal argument
    }
    return
      equals(v1.getSimpleName(), v2.getSimpleName()) &&
      equals(v1.getEnclosingElement(), v2.getEnclosingElement(), ia);
  }

  static final boolean equals(final TypeMirror t1, final TypeMirror t2, final boolean ia) {
    if (t1 == t2) {
      return true;
    } else if ((t1 == null || t2 == null) && ia && !equals(t1.getAnnotationMirrors(), t2.getAnnotationMirrors(), ia)) {
      return false;
    }
    final TypeKind k = t1.getKind();
    if (k != t2.getKind()) {
      return false;
    }
    switch (k) {

    case ARRAY:
      return equals((ArrayType)t1, (ArrayType)t2, ia);

    case DECLARED:
      return equals((DeclaredType)t1, (DeclaredType)t2, ia);

    case EXECUTABLE:
      return equals((ExecutableType)t1, (ExecutableType)t2, ia);

    case INTERSECTION:
      return equals((IntersectionType)t1, (IntersectionType)t2, ia);

    case MODULE:
    case NONE:
    case PACKAGE:
    case VOID:
      return equals((NoType)t1, (NoType)t2, ia);

    case NULL:
      return equals((NullType)t1, (NullType)t2, ia);

    case BOOLEAN:
    case BYTE:
    case CHAR:
    case DOUBLE:
    case FLOAT:
    case INT:
    case LONG:
    case SHORT:
      return equals((PrimitiveType)t1, (PrimitiveType)t2, ia);

    case TYPEVAR:
      return equals((TypeVariable)t1, (TypeVariable)t2, ia);

    case WILDCARD:
      return equals((WildcardType)t1, (WildcardType)t2, ia);

    case ERROR:
    case UNION:
      throw new IllegalArgumentException("t1: " + t1);

    case OTHER:
    default:
      return false;
    }
  }

  static final boolean equals(final ArrayType t1, final ArrayType t2, final boolean ia) {
    if (t1 == t2) {
      return true;
    } else if ((t1 == null || t2 == null) && ia && !equals(t1.getAnnotationMirrors(), t2.getAnnotationMirrors(), ia)) {
      return false;
    }
    return
      t1.getKind() == TypeKind.ARRAY && t2.getKind() == TypeKind.ARRAY &&
      equals(t1.getComponentType(), t2.getComponentType(), ia);
  }

  static final boolean equals(final DeclaredType t1, final DeclaredType t2, final boolean ia) {
    if (t1 == t2) {
      return true;
    } else if ((t1 == null || t2 == null) && ia && !equals(t1.getAnnotationMirrors(), t2.getAnnotationMirrors(), ia)) {
      return false;
    }
    return
      t1.getKind() == TypeKind.DECLARED && t2.getKind() == TypeKind.DECLARED &&
      equals(t1.asElement(), t2.asElement(), ia) &&
      equals(t1.getTypeArguments(), t2.getTypeArguments(), ia);
  }

  static final boolean equals(final ExecutableType t1, final ExecutableType t2, final boolean ia) {
    if (t1 == t2) {
      return true;
    } else if ((t1 == null || t2 == null) && ia && !equals(t1.getAnnotationMirrors(), t2.getAnnotationMirrors(), ia)) {
      return false;
    }
    return
      t1.getKind() == TypeKind.EXECUTABLE && t2.getKind() == TypeKind.EXECUTABLE &&
      equals(t1.getParameterTypes(), t2.getParameterTypes(), ia) &&
      equals(t1.getReceiverType(), t2.getReceiverType(), ia) &&
      equals(t1.getReturnType(), t2.getReturnType(), ia) &&
      // no thrown types
      equals(t1.getTypeVariables(), t2.getTypeVariables(), ia); // not super sure this is necessary
  }

  static final boolean equals(final IntersectionType t1, final IntersectionType t2, final boolean ia) {
    if (t1 == t2) {
      return true;
    } else if ((t1 == null || t2 == null) && ia && !equals(t1.getAnnotationMirrors(), t2.getAnnotationMirrors(), ia)) {
      return false;
    }
    return
      t1.getKind() == TypeKind.INTERSECTION && t2.getKind() == TypeKind.INTERSECTION &&
      equals(t1.getBounds(), t2.getBounds(), ia);
  }

  static final boolean equals(final NoType t1, final NoType t2, final boolean ia) {
    if (t1 == t2) {
      return true;
    } else if ((t1 == null || t2 == null) && ia && !equals(t1.getAnnotationMirrors(), t2.getAnnotationMirrors(), ia)) {
      return false;
    }
    switch (t1.getKind()) {
    case MODULE:
      return t2.getKind() == TypeKind.MODULE;
    case PACKAGE:
      return t2.getKind() == TypeKind.PACKAGE;
    case NONE:
      return t2.getKind() == TypeKind.NONE;
    case VOID:
      return t2.getKind() == TypeKind.VOID;
    default:
      return false;
    }
  }

  static final boolean equals(final NullType t1, final NullType t2, final boolean ia) {
    if (t1 == t2) {
      return true;
    } else if (t1 == null || t2 == null) {
      return false;
    }
    return t1.getKind() == TypeKind.NULL && t2.getKind() == TypeKind.NULL;
  }

  static final boolean equals(final PrimitiveType t1, final PrimitiveType t2, final boolean ia) {
    if (t1 == t2) {
      return true;
    } else if ((t1 == null || t2 == null) && ia && !equals(t1.getAnnotationMirrors(), t2.getAnnotationMirrors(), ia)) {
      return false;
    }
    switch (t1.getKind()) {
    case BOOLEAN:
      return t2.getKind() == TypeKind.BOOLEAN;
    case BYTE:
      return t2.getKind() == TypeKind.BYTE;
    case CHAR:
      return t2.getKind() == TypeKind.CHAR;
    case DOUBLE:
      return t2.getKind() == TypeKind.DOUBLE;
    case FLOAT:
      return t2.getKind() == TypeKind.FLOAT;
    case INT:
      return t2.getKind() == TypeKind.INT;
    case LONG:
      return t2.getKind() == TypeKind.LONG;
    case SHORT:
      return t2.getKind() == TypeKind.SHORT;
    default:
      return false;
    }
  }

  static final boolean equals(final TypeVariable t1, final TypeVariable t2, final boolean ia) {
    if (t1 == t2) {
      return true;
    } else if ((t1 == null || t2 == null) && ia && !equals(t1.getAnnotationMirrors(), t2.getAnnotationMirrors(), ia)) {
      return false;
    }
    return
      t1.getKind() == TypeKind.TYPEVAR && t2.getKind() == TypeKind.TYPEVAR &&
      equals(t1.asElement(), t2.asElement(), ia) &&
      equals(t1.getUpperBound(), t2.getUpperBound(), ia) &&
      equals(t2.getLowerBound(), t2.getLowerBound(), ia);
  }

  static final boolean equals(final WildcardType t1, final WildcardType t2, final boolean ia) {
    if (t1 == t2) {
      return true;
    } else if (t1 == null || t2 == null) {
      return false;
    }
    // The Java type system doesn't actually say that a wildcard type
    // is a type.  At the same time they say that, for example, "?" is
    // equivalent to "? extends Object".  Let's start by simply
    // comparing bounds exactly.
    return
      t1.getKind() == TypeKind.WILDCARD && t2.getKind() == TypeKind.WILDCARD &&
      equals(t1.getExtendsBound(), t2.getExtendsBound(), ia) &&
      equals(t1.getSuperBound(), t2.getSuperBound(), ia);
  }

}
