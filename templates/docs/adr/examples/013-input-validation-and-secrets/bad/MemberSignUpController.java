@RequiredArgsConstructor
@RestController
public class MemberSignUpController {

    private final MemberSignUpService memberSignUpService;

    @PostMapping("/members")
    public ResponseEntity<Map<String, Object>> signUp(@RequestBody MemberSignUpRequest request) { // ADR-013 위반: @Valid 없이 받아, 요청 타입에 제약이 있어도 안 돈다
        if (request.email() == null || !request.email().contains("@")) { // ADR-013 위반: 검사를 선언 대신 컨트롤러 if 로 - 창구마다 빠지는 칸이 생긴다
            return ResponseEntity.badRequest().body(Map.of("error", "email"));
        }
        try {
            MemberSignUpResult result = memberSignUpService.signUp(
                new MemberSignUpCommand(request.email(), request.name(), request.password()));
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("memberId", result.memberId()));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage(), "type", e.getClass().getName())); // ADR-013 위반: 예외 문구(SQL, 칸 값)와 클래스 이름이 응답으로 나간다
        }
    }
}
