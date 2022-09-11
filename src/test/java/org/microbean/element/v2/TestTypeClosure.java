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

import java.lang.reflect.Field;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;

import com.sun.tools.javac.model.JavacTypes;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(AnnotationProcessingInterceptor.class)
final class TestTypeClosure {

  private TestTypeClosure() {
    super();
  }

  @Test
  final void testTypeClosure(final ProcessingEnvironment env) {
    final javax.lang.model.util.Elements elements = env.getElementUtils();
    final javax.lang.model.util.Types javacModelTypes = env.getTypeUtils();
    assertTrue(javacModelTypes instanceof JavacTypes);
    com.sun.tools.javac.code.Types javacTypes = null;
    try {
      final Field f = JavacTypes.class.getDeclaredField("types");
      assertTrue(f.trySetAccessible());
      javacTypes = (com.sun.tools.javac.code.Types)f.get(javacModelTypes);
    } catch (final ReflectiveOperationException reflectiveOperationException) {
      fail(reflectiveOperationException);
    }
    assertNotNull(javacTypes);
    
    final TypeElement integerElement = elements.getTypeElement("java.lang.Integer");
    final DeclaredType integerElementType = (DeclaredType)integerElement.asType();
    assertSame(TypeKind.DECLARED, integerElementType.getKind());

    // https://github.com/openjdk/jdk/blob/jdk-20+14/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L3702-L3703
    //
    // "…(that is, subclasses come first, arbitrary but fixed otherwise)."
    
    final List<Type> closure = javacTypes.closure((Type)integerElementType);
    // 0: java.lang.Integer
    // 1: java.lang.Number (superclass of java.lang.Integer)
    // 2: java.io.Serializable (declared interface)
    // 3: java.lang.Comparable<java.lang.Integer> (declared interface)
    // 4: java.lang.constant.Constable (declared interface)
    // 5: java.lang.constant.ConstantDesc (declared interface)
    // 6: java.lang.Object (superclass of java.lang.Number)
    assertEquals(7, closure.size());

    // Let's try it with our visitor.

    // Set up the fundamentals.
    final Types2 types2 = new Types2();
    final EraseVisitor eraseVisitor = new EraseVisitor(types2);
    final SupertypeVisitor supertypeVisitor = new SupertypeVisitor(types2, eraseVisitor);

    // These have cycles.
    final ContainsTypeVisitor containsTypeVisitor = new ContainsTypeVisitor(types2);
    final IsSameTypeVisitor isSameTypeVisitor = new IsSameTypeVisitor(containsTypeVisitor, supertypeVisitor);    
    final SubtypeVisitor subtypeVisitor = new SubtypeVisitor(types2, supertypeVisitor, isSameTypeVisitor);
    containsTypeVisitor.subtypeVisitor = subtypeVisitor;
    subtypeVisitor.containsTypeVisitor = containsTypeVisitor;

    final PrecedesPredicate precedesPredicate = new PrecedesPredicate(supertypeVisitor, subtypeVisitor);
    final TypeClosureVisitor typeClosureVisitor = new TypeClosureVisitor(supertypeVisitor, precedesPredicate);

    final TypeClosure c = typeClosureVisitor.visit(integerElementType);
    final List<? extends TypeMirror> list = c.toList();
    assertEquals(7, list.size(), "Unexpected type closure list: " + list);
    
  }

}
