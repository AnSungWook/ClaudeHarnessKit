public class InternalTokenAuthenticationFilter extends OncePerRequestFilter {

    // 인증 없이 여는 경로만 적는다. 목록에 없는 경로는 새로 생겨도 막힌 채로 시작한다.
    private static final Set<String> OPEN_PATHS = Set.of("/actuator/health", "/actuator/info");

    // ...

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // CORS 사전 요청은 토큰 헤더 없이 온다. 통과시켜도 MVC 는 CORS 판정만 하고 컨트롤러를 부르지 않는다.
        return CorsUtils.isPreFlightRequest(request) || OPEN_PATHS.contains(pathWithinApplication(request));
    }

    private static String pathWithinApplication(HttpServletRequest request) {
        // getRequestURI 는 context-path 를 포함한다. 빼지 않으면 /app 아래로 배포했을 때 경로 판정이 어긋난다.
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        if (!hasValidToken(request)) {
            errorResponseWriter.writeUnauthorized(response);
            return;
        }
        chain.doFilter(request, response);
    }

    // ... hasValidToken: 헤더 토큰을 MessageDigest.isEqual 로 끝까지 견준다(틀린 자리에서 멈추면 걸린 시간으로 토큰이 샌다)
}
