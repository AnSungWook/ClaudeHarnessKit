// ADR-001 위반: 표시 없이 패턴으로 서비스 전체에 걸고, 반환값(업무 결과)을 바꾼다. 서비스 코드 어디에도 흔적이 없다.
@Aspect
@Component
public class DiscountAdjustingAspect {

    @Around("execution(* com.example..application.service..*(..))")
    public Object applyMemberDiscount(ProceedingJoinPoint call) throws Throwable {
        Object result = call.proceed();
        if (result instanceof PlacedOrder placed && memberContext.isVip()) {
            return placed.withDiscount(0.1);   // 업무 결과를 바꾼다
        }
        return result;
    }
}
