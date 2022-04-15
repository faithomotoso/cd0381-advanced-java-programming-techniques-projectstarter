package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private Instant startTime, endTime;

  // TODO: You will need to add more instance fields and constructor arguments to this class.
  ProfilingMethodInterceptor(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // TODO: This method interceptor should inspect the called method to see if it is a profiled
    //       method. For profiled methods, the interceptor should record the start time, then
    //       invoke the method using the object that is being profiled. Finally, for profiled
    //       methods, the interceptor should record how long the method call took, using the
    //       ProfilingState methods.


    if (!isMethodProfiled(method)) {
      // No profiled annotation
      try {
        return method.invoke(proxy, args);
      } catch (InvocationTargetException e) {
        throw e.getCause();
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    Object result = null;
    Exception exception = null;
    startTime = clock.instant();

    try {
      result = method.invoke(proxy, args);
    } catch (Exception e) {
      exception = e;
    }
    finally {
      endTime = clock.instant();
    }

    if (exception != null) {
      throw exception;
    }

    return result;
  }

  private boolean isMethodProfiled(Method method) {
    return method.getAnnotation(Profiled.class) != null;
  }
}
