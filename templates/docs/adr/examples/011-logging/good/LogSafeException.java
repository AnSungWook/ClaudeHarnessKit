// 던지지 않고 로그에만 넘기는 그릇이다. 바깥 API·DB 예외 문구에는 요청 본문과 바인딩 값이 실리므로,
// 어디서 났는지(클래스 이름과 스택 위치)만 남기고 문구는 버린다(ADR-011).
public final class LogSafeException extends RuntimeException {

    private LogSafeException(String originalType, LogSafeException safeCause) {
        super(originalType, safeCause, false, true);
    }

    public static LogSafeException of(Throwable original) {
        List<Throwable> chain = causeChainOf(original);
        LogSafeException safeCause = null;
        for (int i = chain.size() - 1; i >= 0; i--) {
            Throwable link = chain.get(i);
            LogSafeException safeLink = new LogSafeException(link.getClass().getName(), safeCause);
            safeLink.setStackTrace(link.getStackTrace());
            safeCause = safeLink;
        }
        return safeCause;
    }

    private static List<Throwable> causeChainOf(Throwable original) {
        // 원인이 돌고 도는 사슬도 있어 이미 본 예외에서 멈춘다.
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Throwable> chain = new ArrayList<>();
        for (Throwable link = original; link != null && seen.add(link); link = link.getCause()) {
            chain.add(link);
        }
        return chain;
    }
}
