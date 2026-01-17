package com.vega.techtest.aspect;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class TimingAspect {

    private final MeterRegistry meterRegistry;

    @Around("@annotation(timed)")
    public Object timeMethod(ProceedingJoinPoint joinPoint, Timed timed) throws Throwable {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            return joinPoint.proceed();
        } finally {
            Timer timer = meterRegistry.timer(timed.value());
            sample.stop(timer);
        }
    }
}
