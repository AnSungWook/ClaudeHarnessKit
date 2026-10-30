# ADR-015: 이름은 뜻을 드러내고, 업무 용어 하나로 통일한다

## Status

Accepted

## Date

2026-10-30

## Intent

주석 없이 이름만 읽어도 무엇을 담고 무엇을 하는지 알게 하고, 같은 개념을 같은 낱말로 부르게 한다.

## Context

- 기존 상태: `data`, `info`, `temp`, `obj` 같은 이름, `OrderManager`, `CommonUtil`, `XxxHelper` 같은 뭉뚱그린 클래스, 사람마다 다른 약어(`ord`, `odr`), 같은 개념을 화면·코드·DB 가 각자 다른 낱말로 부르는 일이 섞여 있다.
- 문제점: 이름이 뜻을 못 말하니 주석과 코드 읽기에 기대고, 뭉뚱그린 클래스는 아무 일이나 받아 비대해진다. 같은 개념을 여러 낱말로 부르면 검색이 안 되고 서로 다른 것으로 오해한다.
- 제약 조건: 대소문자 표기(camelCase, snake_case)와 접두어 관례는 언어·프레임워크마다 다르다 - 그것은 그 언어의 관례를 따른다.

## Decision

**이름이 담은 것과 하는 일을 말하게 하고, 한 개념은 한 낱말로 부른다.**

- 뜻을 드러낸다. 변수·필드는 담은 것(`paidAmount`), 함수·메서드는 하는 일을 동사로(`cancelOrder`, `findActiveMembers`), 타입은 무엇인지 명사로(`OrderLine`).
- 불리언은 참·거짓으로 읽히게: `isPaid`, `hasCoupon`, `canRetry`. 부정형(`isNotValid`)은 피한다.
- 뭉뚱그린 이름을 쓰지 않는다: `Manager`, `Helper`, `Util`, `Common`, `Processor`, `Data`, `Info`, `Handler` 를 홀로 붙인 타입. 하는 일이 하나로 말해지지 않으면 나눈다(`OrderManager` → `OrderCanceller`, `OrderPriceCalculator`).
- 약어를 만들지 않는다. 누구나 아는 것(`id`, `url`, `http`)만 쓴다. 한 글자 이름은 짧은 반복 변수(`i`)와 람다 인자 정도에만.
- 업무 용어를 하나로 통일한다(유비쿼터스 언어). 같은 개념을 화면·코드·DB·문서에서 같은 낱말로 부르고, 프로젝트 용어집(`docs/` 아래)에 적는다. 다른 개념에는 다른 낱말을 쓴다.
- 이름에 타입이나 구현을 넣지 않는다: `orderList` 보다 `orders`, `strName` 보다 `name`, `IOrderRepository`/`OrderRepositoryImpl` 같은 접두·접미어 대신 역할 이름(`JpaOrderRepository`).
- 숫자 상수에는 뜻 있는 이름을 붙인다(ADR-002).
- 테스트 이름은 무엇을 확인하는지 말한다(ADR-011).
- 대소문자 표기와 파일 이름 규칙은 그 언어·프레임워크의 관례를 따른다.

### 체크리스트

- [ ] `data`, `info`, `temp`, `obj`, `tmp` 같은 뜻 없는 변수·필드 이름이 없다
- [ ] `Manager`/`Helper`/`Util`/`Common` 을 홀로 붙인 새 타입이 없다
- [ ] 불리언이 `is`/`has`/`can` 꼴로 읽힌다
- [ ] 새로 만든 약어가 없다
- [ ] 같은 개념에 두 낱말을 쓰지 않는다(용어집과 맞다)
- [ ] 이름에 타입·구현 표시(`List`, `Impl`, `I` 접두어)가 없다

### 안티패턴

| 안티패턴 | 올바른 패턴 |
|---------|-----------|
| `val data = repository.find(id)` | `val order = repository.find(id)` |
| `class OrderManager`(생성, 취소, 정산 다 함) | `OrderCanceller`, `SettlementCalculator` 로 나눈다 |
| `boolean flag`, `boolean check` | `boolean isExpired` |
| `ordAmt`, `usrNm` | `orderAmount`, `userName` |
| 화면은 "회원", 코드는 `User`, DB 는 `customer` | 한 낱말로 통일하고 용어집에 적는다 |
| `List<Order> orderList`, `OrderServiceImpl` | `orders`, 역할이 드러나는 구현 이름 |

## 다른 언어에서

- 원칙은 언어와 무관하다. 대소문자(camelCase/snake_case/PascalCase), 상수 표기, 파일 이름은 그 언어 관례를 따른다(Python 은 PEP 8, TypeScript 는 커뮤니티 관례, Kotlin 은 공식 코딩 컨벤션).
- Python 의 `utils.py`, TypeScript 의 `helpers.ts` 처럼 뭉뚱그린 모듈도 같은 규칙으로 본다.

## Consequences

### 긍정적
- 이름만 읽고 코드를 이해하는 범위가 넓어진다.
- 뭉뚱그린 클래스가 생기지 않아 책임이 나뉜다.
- 검색이 된다.

### 부정적
- 이름을 짓는 데 시간이 더 든다.
- 기존 이름을 바꾸면 diff 가 커져, 바꾼 줄부터 맞춰 간다(ADR-006).

## References

- 출처: [affaan-m/everything-claude-code rules/common/coding-style.md](https://github.com/affaan-m/everything-claude-code/blob/main/rules/common/coding-style.md)(Naming Conventions), [PatrickJS/awesome-cursorrules rules/clean-code.mdc](https://github.com/PatrickJS/awesome-cursorrules/blob/main/rules/clean-code.mdc)(Meaningful Names), [github/awesome-copilot code-review-generic.instructions.md](https://github.com/github/awesome-copilot/blob/main/instructions/code-review-generic.instructions.md)
- ADR-002(상수 이름), ADR-006(바꾼 줄만), ADR-011(테스트 이름)
