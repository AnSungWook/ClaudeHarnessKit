@RequiredArgsConstructor
@RestController
public class MemberSignUpController {

    private final MemberSignUpService memberSignUpService;

    @PostMapping("/members")
    public ResponseEntity<CommonResponse<MemberSignUpResponse>> signUp(
        @Valid @RequestBody MemberSignUpRequest request) {
        MemberSignUpResult result = memberSignUpService.signUp(
            new MemberSignUpCommand(request.email(), request.name(), request.password()));
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.success(MemberSignUpResponse.of(result)));
    }
}
