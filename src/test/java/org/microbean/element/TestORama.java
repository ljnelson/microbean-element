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

import java.io.IOException;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;

import javax.lang.model.SourceVersion;

import javax.lang.model.element.TypeElement;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

import org.junit.jupiter.api.Test;

import static javax.lang.model.SourceVersion.RELEASE_17;

import static javax.tools.ToolProvider.getSystemJavaCompiler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestORama {

  private TestORama() {
    super();
  }

  @Test
  final void testORama() throws IOException {
    final JavaCompiler compiler = getSystemJavaCompiler();
    assertNotNull(compiler);
    try (final StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
      final Path frob = Paths.get(System.getProperty("project.basedir"), "src", "test", "resources", "Frob.java");
      final Iterable<? extends JavaFileObject> fileObjects = fileManager.getJavaFileObjectsFromPaths(List.of(frob));
      final CompilationTask task =
        compiler.getTask(null,
                         fileManager,
                         null,
                         null,
                         null,
                         fileObjects);
      task.setProcessors(List.of(new StupidProcessor()));
      assertTrue(task.call());
    }
  }

  private static final class StupidProcessor extends AbstractProcessor {

    private StupidProcessor() {
      super();
    }

    @Override // AbstractProcessor
    public final SourceVersion getSupportedSourceVersion() {
      return RELEASE_17;
    }

    @Override // AbstractProcessor
    public final void init(final ProcessingEnvironment processingEnvironment) {
      final javax.lang.model.util.Elements elements = processingEnvironment.getElementUtils();
      final TypeElement e = elements.getTypeElement("Frob");
      assertNotNull(e);
      System.out.println("*** e: " + e);
      System.out.println("*** e.asType: " + e.asType());
      System.out.println("*** java.lang.Comparable: " + elements.getTypeElement("java.lang.Comparable"));
    }
    
    @Override // Processor
    public final boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnvironment) {
      return false;
    }
    
  }
  
}
