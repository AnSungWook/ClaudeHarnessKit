public record MemberSignUpRequest(
    @NotBlank @Email @Size(max = 320) String email,
    @NotBlank @Size(max = 50) String name,
    @NotBlank @Size(min = 8, max = 64) String password) {

    @Override
    public String toString() {
        // record 기본 toString 은 모든 칸을 찍는다. 누가 요청을 로그에 남겨도 비밀번호는 새지 않게 가린다.
        return "MemberSignUpRequest[email=%s, name=%s, password=****]".formatted(email, name);
    }
}
