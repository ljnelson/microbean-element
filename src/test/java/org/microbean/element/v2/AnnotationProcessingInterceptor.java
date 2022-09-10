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

import java.lang.reflect.Method;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;

import javax.lang.model.SourceVersion;

import javax.lang.model.element.TypeElement;

import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import javax.tools.JavaCompiler.CompilationTask;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.InvocationInterceptor.Invocation;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import static javax.tools.ToolProvider.getSystemJavaCompiler;

public final class AnnotationProcessingInterceptor implements InvocationInterceptor, ParameterResolver {

  @Deprecated // for use by JUnit Jupiter internals only
  AnnotationProcessingInterceptor() {
    super();
  }

  /*
   * Invocation order: supportsParameter, resolveParameter, interceptTestMethod
   */

  @Deprecated // for use by JUnit Jupiter internals only
  @Override // ParameterResolver
  public final boolean supportsParameter(final ParameterContext parameterContext,
                                         final ExtensionContext extensionContext)
    throws ParameterResolutionException {
    return ProcessingEnvironment.class.isAssignableFrom(parameterContext.getParameter().getType());
  }

  @Deprecated // for use by JUnit Jupiter internals only
  @Override // ParameterResolver
  public final ProcessingEnvironment resolveParameter(final ParameterContext parameterContext,
                                                      final ExtensionContext extensionContext)
    throws ParameterResolutionException {
    return extensionContext.getStore(Namespace.create(Thread.currentThread().getId()))
      .getOrComputeIfAbsent(ForwardingProcessingEnvironment.class,
                            k -> new ForwardingProcessingEnvironment(),
                            ForwardingProcessingEnvironment.class);
  }

  @Deprecated // for use by JUnit Jupiter internals only
  @Override // InvocationInterceptor
  public final void interceptTestMethod(final Invocation<Void> invocation,
                                        final ReflectiveInvocationContext<Method> invocationContext,
                                        final ExtensionContext extensionContext)
    throws Throwable {
    final Store store = extensionContext.getStore(Namespace.create(Thread.currentThread().getId()));
    final ForwardingProcessingEnvironment fpe = store.get(ForwardingProcessingEnvironment.class, ForwardingProcessingEnvironment.class);
    if (fpe == null) {
      invocation.proceed();
      return;
    }
    try {
      final CompilationTask task = getSystemJavaCompiler()
        .getTask(null,
                 null,
                 null,
                 List.of("-proc:only"),
                 List.of("java.lang.Object"),
                 null);
      task.setProcessors(List.of(new AbstractProcessor() {

          @Override // AbstractProcessor
          public final void init(final ProcessingEnvironment processingEnvironment) {
            try {
              fpe.delegate = processingEnvironment;
              invocation.proceed();
            } catch (final RuntimeException | Error e) {
              throw e;
            } catch (final Exception e) {
              throw new RuntimeException(e.getMessage(), e);
            } catch (final Throwable t) {
              throw new AssertionError(t.getMessage(), t);
            } finally {
              fpe.delegate = null;
            }
          }

          @Override // AbstractProcessor
          public final SourceVersion getSupportedSourceVersion() {
            final SupportedSourceVersion ssv = invocationContext.getTargetClass().getAnnotation(SupportedSourceVersion.class);
            return ssv == null ? SourceVersion.latest() : ssv.value();
          }

          @Override // AbstractProcessor
          public final boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnvironment) {
            return false;
          }

        }));
      task.call();
    } finally {
      store.remove(ForwardingProcessingEnvironment.class);
    }
  }


  /*
   * Inner and nested classes.
   */


  private static final class ForwardingProcessingEnvironment implements ProcessingEnvironment {

    private volatile ProcessingEnvironment delegate;

    private ForwardingProcessingEnvironment() {
      super();
    }

    @Override
    public final Elements getElementUtils() {
      return this.delegate.getElementUtils();
    }

    @Override
    public final Filer getFiler() {
      return this.delegate.getFiler();
    }

    @Override
    public final Locale getLocale() {
      return this.delegate.getLocale();
    }

    @Override
    public final Messager getMessager() {
      return this.delegate.getMessager();
    }

    @Override
    public final Map<String, String> getOptions() {
      return this.delegate.getOptions();
    }

    @Override
    public final SourceVersion getSourceVersion() {
      return this.delegate.getSourceVersion();
    }

    @Override
    public final Types getTypeUtils() {
      return this.delegate.getTypeUtils();
    }

    @Override
    public final boolean isPreviewEnabled() {
      return this.delegate.isPreviewEnabled();
    }

    @Override
    public final int hashCode() {
      return this.delegate == null ? 0 : this.delegate.hashCode();
    }

    @Override
    public final boolean equals(final Object other) {
      if (other == this) {
        return true;
      } else if (other != null && this.getClass() == other.getClass()) {
        return Objects.equals(this.delegate, ((ForwardingProcessingEnvironment)other).delegate);
      } else {
        return false;
      }
    }

    @Override
    public final String toString() {
      return this.delegate == null ? super.toString() : this.delegate.toString();
    }

  }

}
