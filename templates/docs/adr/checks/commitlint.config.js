// ADR-020: 커밋 제목은 `type(scope): subject`. subject 는 팀 언어(한국어 가능)로 쓰고 마침표로 끝내지 않는다.
// 쓰는 법은 같은 폴더 README.md.
module.exports = {
  extends: ['@commitlint/config-conventional'],
  plugins: [
    {
      rules: {
        // ADR-020: scope 는 영문 소문자와 하이픈만. 내장 scope-case 는 문자 집합을 제한하지 않아 따로 둔다.
        'scope-lowercase-hyphen': ({ scope }) => [
          !scope || /^[a-z]+(-[a-z]+)*$/.test(scope),
          'scope 는 영문 소문자와 하이픈만 쓴다(예: order, http-client)',
        ],
      },
    },
  ],
  rules: {
    'type-enum': [2, 'always', ['feat', 'fix', 'refactor', 'test', 'docs', 'chore', 'build']],
    'scope-case': [0],
    'scope-lowercase-hyphen': [2, 'always'],
    'subject-case': [0],
    'subject-full-stop': [2, 'never', '.'],
    'header-max-length': [0],
    'body-max-line-length': [0],
    'footer-max-line-length': [0],
  },
  // 머지 커밋만 도구가 만든 제목 그대로 둔다(ADR-020). 기본 무시 목록(Revert, fixup!, 버전 태그 등)은 끈다.
  defaultIgnores: false,
  ignores: [(message) => /^Merge (pull request|branch|remote-tracking branch) /.test(message)],
};
