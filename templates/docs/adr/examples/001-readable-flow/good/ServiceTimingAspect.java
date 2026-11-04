// shared/observability - 표시 없이 패턴으로 거는 advice 는 관측만 한다(ADR-001).
@Aspect
@Component
public class ServiceTimingAspect {

    private final MeterRegistry meters;

    // ...

    @Around("within(com.example..application.service..*)")
    public Object timeServiceCall(ProceedingJoinPoint call) throws Throwable {
        Timer.Sample sample = Timer.start(meters);
        try {
            return call.proceed();                 // 인자와 반환값을 그대로 넘긴다
        } finally {
            // 예외를 삼키거나 바꾸지 않고, 다시 실행하지 않는다. 인자 값(개인정보)을 태그로 남기지 않는다.
            sample.stop(meters.timer("service.call", "method", call.getSignature().toShortString()));
        }
    }
}
