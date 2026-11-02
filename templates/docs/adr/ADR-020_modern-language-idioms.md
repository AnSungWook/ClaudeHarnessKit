# ADR-020: 같은 일은 짧고 선언적인 최신 문법 한 모양으로 쓴다 - 스트림, switch 식, 패턴 매칭, sealed 와 record

## Status

Accepted

## Date

2026-11-02

## Intent

같은 일을 하는 코드가 한 모양이 되어 읽는 사람이 하나만 익히면 되게 하고, 빠진 분기와 잘못된 캐스팅을 컴파일러가 잡게 한다.

## Context

- 기존 상태: 같은 저장소 안에 `for` 로 쌓는 코드와 스트림, 문장형 `switch` 와 `switch` 식, `instanceof` 뒤 캐스팅과 패턴 매칭이 섞여 있다. `new ArrayList<>()` 에 담아 돌려준다.
- 문제점: 두 모양을 다 익혀야 하고, 리뷰마다 "이렇게 쓰자" 가 반복된다. 문장형 `switch` 의 fallthrough 와 `default` 가 새 enum 값을 조용히 삼킨다.
- 제약 조건: 언어 판이 문법을 정한다. 본문은 Java 21 기준이다. 판이 낮으면 쓸 수 있는 것까지만 따른다.

## Decision

**결과가 나오는 순회는 스트림, 분기는 `switch` 식, 타입 검사는 패턴 매칭, 닫힌 집합은 sealed 와 record.**

- 결과가 나오는 컬렉션 순회는 스트림으로 쓴다(`filter`, `map`, `toList()`, `groupingBy`, `anyMatch`). `for-each` 는 자원을 닫거나, 중간에 끊거나(`break`), 두 컬렉션을 인덱스로 짝지을 때만 쓴다. 인덱스 `for` 는 배열, 문자 훑기, 수치 계산에만 쓴다.
- 스트림 안에서 부수효과를 내지 않는다. `forEach` 로 바깥 컬렉션에 담지 않고 `collect` 나 `toList()` 로 받는다. `forEach` 는 로그나 호출처럼 결과가 없는 일에만 쓴다.
- 컬렉션은 불변이 기본이다(ADR-008). `new ArrayList<>()` 에 쌓아 돌려주는 모양은 스트림으로 바꾼다.
- enum 에 따른 판단은 enum 안에 둔다(ADR-002). 그 밖의 분기는 `switch` 식(`->`)이다. 문장형 `switch` 와 fallthrough 는 쓰지 않는다. enum 과 sealed 타입을 `switch` 할 때는 `default` 없이 전부 나열해 컴파일러가 빠진 가지를 잡게 한다.
- 타입 검사는 패턴 매칭으로 한다(`instanceof T t`, record 패턴, `switch` 의 타입 패턴). `instanceof` 뒤에 따로 캐스팅하지 않는다.
- 닫힌 타입 집합(성공과 실패 같은 결과)은 `sealed interface` 와 record 로 만든다.
- 문자열은 이렇게 쓴다.
  - 여러 줄은 텍스트 블록(`"""`)으로 쓴다.
  - 끼워 넣기는 `.formatted()` 로 하고, 잇기는 `String.join` 이나 `Collectors.joining` 으로 한다.
  - `StringBuilder` 는 루프 안에서 많이 조립할 때만 쓴다.
  - Locale 에 따라 결과가 바뀌는 형식은 `String.format(Locale.ROOT, ...)` 로 쓴다.
  - SQL 과 로그 문구를 `+` 로 조립하지 않는다(ADR-010, ADR-012).
- `var` 는 오른쪽에서 타입이 바로 보일 때만 쓴다. `var order = Order.of(...)` 는 되고 `var result = service.run(x)` 는 안 된다.
- 한 번에 저장소 전체를 바꾸지 않는다. 새 코드에 걸고, 기존 코드는 그 파일을 고칠 때 맞춘다(ADR-006).

### 체크리스트

- [ ] 결과를 쌓아 돌려주는 `for` + `new ArrayList<>()` 가 새로 들지 않았다
- [ ] 스트림 `forEach` 안에서 바깥 컬렉션을 고치지 않는다
- [ ] 문장형 `switch`(`case X:`)가 새로 들지 않았다
- [ ] enum, sealed 타입 `switch` 에 `default` 가 없다
- [ ] `instanceof` 뒤 별도 캐스팅이 없다
- [ ] `var` 가 오른쪽 타입이 보이는 자리뿐이다

### 안티패턴

| 안티패턴 | 올바른 패턴 |
|---------|-----------|
| `List<String> r = new ArrayList<>(); for (...) if (...) r.add(x); return r;` | `return items.stream().filter(...).map(...).toList();` |
| `list.stream().forEach(x -> result.add(x))` | `list.stream().toList()` |
| `switch (s) { case A: ...; break; default: ... }` | `switch (s) { case A -> ...; case B -> ...; }` (default 없이 전부) |
| `if (o instanceof Card) { Card c = (Card) o; ... }` | `if (o instanceof Card c) { ... }` |
| 성공, 실패를 `boolean ok` 와 nullable 칸 둘로 | `sealed interface Result permits Success, Failure` |
| `var x = repository.load(id);` | `Order order = repository.load(id);` |

## 다른 언어에서

- 원칙(결과 순회는 선언형, 부수효과 없는 변환, 빠짐없는 분기를 컴파일러에, 닫힌 집합은 타입으로)은 같다.
- Kotlin: 컬렉션 함수(`filter`, `map`), `when` 식을 `sealed class` 와 함께 `else` 없이, 스마트 캐스트.
- TypeScript: `map`/`filter`/`reduce`, 판별 유니온(discriminated union) + `switch` 에서 `never` 로 빠짐 검사, `readonly` 배열.
- Python: 컴프리헨션, `match` 문(3.10+), `Enum`, `dataclass(frozen=True)`, 타입 힌트의 `Literal` 과 유니온.

## Consequences

### 긍정적
- 한 모양만 읽으면 된다. "무엇을" 이 "어떻게" 보다 먼저 보인다.
- 새 enum 값, 새 결과 타입을 빠뜨리면 컴파일이 깨진다.

### 부정적
- 스트림이 길어지면 오히려 읽기 어렵다 - 단계가 많으면 이름 있는 메서드로 나눈다.
- 기존 코드와 한동안 두 모양이 같이 산다.

## References

- ADR-002(enum 안의 판단), ADR-006(바꾼 줄만), ADR-008(불변), ADR-010(로그 문구), ADR-012(SQL 바인딩), ADR-014(잠금)
