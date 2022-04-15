package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final Object delegate; // Profiled class
  private final ProfilingState profilingState;

  // TODO: You will need to add more instance fields and constructor arguments to this class.
  ProfilingMethodInterceptor(Object delegate, ProfilingState profilingState, Clock clock) {
    this.delegate = Objects.requireNonNull(delegate);
    this.profilingState = Objects.requireNonNull(profilingState);
    this.clock = Objects.requireNonNull(clock);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // TODO: This method interceptor should inspect the called method to see if it is a profiled
    //       method. For profiled methods, the interceptor should record the start time, then
    //       invoke the method using the object that is being profiled. Finally, for profiled
    //       methods, the interceptor should record how long the method call took, using the
    //       ProfilingState methods.


    Instant startTime = clock.instant();
    try {
      return method.invoke(delegate, args);
    } catch (InvocationTargetException e) {
      throw e.getTargetException();
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } finally {
      if (isMethodProfiled(method)) {
        Instant endTime = clock.instant();
        profilingState.record(delegate.getClass(), method, Duration.between(startTime, endTime));
      }
    }
  }

  private boolean isMethodProfiled(Method method) {
    return method.getAnnotation(Profiled.class) != null;
  }
}
