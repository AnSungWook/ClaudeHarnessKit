# ADR-016: 패키지는 기능별로 나누고, 그 안은 성격별 폴더로 고정한다

## Status

Accepted

## Date

2026-10-30

## Intent

폴더 이름만 보고 그 안에 무엇이 있는지 알게 하고, 어느 저장소를 열어도 같은 자리에 같은 성격의 코드가 있게 한다. ADR-004 의 층(역할)이 폴더에 그대로 드러나게 한다.

## Context

- 기존 상태: 한 패키지 안에 엔티티, 설정, 서비스, 컨트롤러가 섞여 있다. 같은 회사의 두 저장소가 서로 다른 모양이다(한쪽은 성격별, 한쪽은 기능별에 제각각인 하위 폴더). `api` 라는 폴더에 컨트롤러는 없고 모듈 사이에 공개하는 값(정책, 이력, 종류)이 들어 있는 식으로, 흔한 뜻과 다른 이름을 쓴다.
- 문제점: 처음 보는 사람이 "컨트롤러는 어디, 설정은 어디" 를 저장소마다 새로 찾는다. 이름이 흔한 뜻과 달라 잘못 읽는다. 역할은 문서(대응표)에만 있고 폴더에 없어서, 리팩토링할 때도 폴더 정리는 범위에서 빠진다.
- 제약 조건: 이미 커진 저장소를 한 번에 옮기면 diff 와 진행 중인 브랜치 충돌이 크다.

## Decision

**최상위는 기능(업무 단위)으로 나누고, 각 기능 안은 아래 성격별 폴더로 고정한다. 이름과 자리를 바꾸지 않는다.**

```
<루트 패키지>/
├── <기능>/                        # 주문, 결제, 정산처럼 업무 단위 하나
│   ├── domain/                    # 값, 도메인 모델, 판단, 도메인 서비스 (ADR-001)
│   │   └── exception/             # 그 기능의 사유 예외 (ADR-005)
│   ├── application/
│   │   ├── port/
│   │   │   ├── in/                # 유스케이스 interface - 다른 기능과 바깥이 부르는 입구
│   │   │   └── out/               # 바깥에 기대는 약속 interface (저장, 외부 호출)
│   │   └── service/               # 유스케이스 구현 - 흐름만, 트랜잭션 경계 (ADR-013)
│   ├── adapter/
│   │   ├── in/
│   │   │   ├── web/               # 컨트롤러, 요청·응답 DTO (ADR-004, ADR-012)
│   │   │   └── batch/, message/   # 배치 잡, 메시지 수신 등 다른 입구
│   │   └── out/
│   │       ├── persistence/       # 영속 엔티티, 리포지토리·매퍼, out 포트 구현
│   │       └── client/            # 외부 API 호출, out 포트 구현
│   └── config/                    # 그 기능의 빈 조립, 설정 객체 (ADR-002)
└── shared/                        # 여러 기능이 같이 쓰는 것만. 최소로 유지
```

- **폴더 이름은 위 목록만 쓴다.** `api`, `common`, `util`, `helper`, `manager`, `etc`, `impl`, `core` 를 기능 안 폴더 이름으로 쓰지 않는다(ADR-015). 특히 `api` 는 HTTP API 로 읽히므로, 모듈이 공개하는 입구는 `application/port/in`, 공개하는 값은 `domain` 에 둔다.
- **한 폴더에는 한 성격만.** 컨트롤러는 `adapter/in/web` 에만, 설정은 `config` 에만, 영속 엔티티는 `adapter/out/persistence` 에만 둔다. 섞이면 위반이다.
- **기능 사이 호출은 상대 기능의 `application/port/in` 과 `domain` 의 공개 값으로만.** 상대의 `adapter`, 영속 엔티티, `service` 구현을 import 하지 않는다.
- **의존 방향은 ADR-004 그대로**: `adapter` → `application` → `domain`. `domain` 은 다른 폴더를 import 하지 않는다.
- **작은 기능은 폴더를 줄여도 된다.** 바깥 입구가 하나뿐이면 `port/in` 없이 `application/service` 를 바로 부를 수 있고, 외부 호출이 없으면 `adapter/out/client` 를 만들지 않는다. 다만 있는 폴더의 이름과 자리는 바꾸지 않는다.
- **리팩토링의 범위에는 패키지 정렬이 기본으로 들어간다.** 구조·설계 리팩토링을 하면 그 범위의 파일이 이 배치와 다른지 보고 같이 옮긴다. 따로 지시하지 않아도 한다. 옮기는 커밋은 기능 변경 커밋과 나눈다(ADR-006).
- **이미 커진 저장소는 기능 하나씩 옮긴다.** 한 번에 다 옮기지 않고, 기능 단위로 이동 커밋을 따로 낸다. 옮기기 전 상태는 대응표로 문서에 두되, 대응표는 옮길 때까지의 임시 장치다.

### 체크리스트

- [ ] 새 파일이 위 배치의 자리에 있다
- [ ] 기능 안에 목록 밖 폴더 이름(`api`, `common`, `util` 등)이 없다
- [ ] 한 폴더에 컨트롤러·설정·엔티티·서비스가 섞여 있지 않다
- [ ] 다른 기능의 `adapter`, 영속 엔티티, `service` 구현을 import 하지 않는다
- [ ] 구조 리팩토링 PR 에 패키지 정렬이 들어 있다(또는 안 한 이유가 적혀 있다)
- [ ] 파일 이동 커밋과 기능 변경 커밋이 나뉘어 있다

### 안티패턴

| 안티패턴 | 올바른 패턴 |
|---------|-----------|
| `order/api/` 에 정책·이력 record, 컨트롤러는 `order/web/` | 공개 값은 `order/domain/`, 입구는 `order/application/port/in/`, 컨트롤러는 `order/adapter/in/web/` |
| `order/` 한 폴더에 `OrderController`, `OrderConfig`, `OrderEntity`, `OrderService` | 성격별 폴더로 나눈다 |
| 저장소 전체를 `controller/`, `service/`, `repository/` 로만 가르기(기능이 흩어짐) | 기능이 최상위, 성격은 그 안 |
| `payment` 가 `order/adapter/out/persistence/OrderEntity` 를 import | `order/application/port/in` 의 유스케이스를 부른다 |
| 리팩토링 PR 에서 클래스만 고치고 폴더는 그대로 | 범위의 파일을 이 배치로 같이 옮긴다 |
| `common/`, `util/` 에 아무 기능의 코드나 모음 | 주인 기능으로 옮기고, 진짜 공용만 `shared/` |

## 다른 언어에서

- Kotlin: 같은 배치를 그대로 쓴다.
- TypeScript(NestJS): `src/<기능>/` 아래 `domain/`, `application/`, `adapter/in/http/`(컨트롤러), `adapter/out/persistence/`, 모듈 파일(`<기능>.module.ts`)은 `config` 자리에 둔다.
- Python: `<패키지>/<기능>/` 아래 같은 하위 패키지(`domain`, `application`, `adapter/inbound/web`, `adapter/outbound/persistence`). `in` 은 예약어라 `inbound`/`outbound` 로 쓴다.

## Consequences

### 긍정적
- 어느 저장소든 같은 자리에서 같은 성격의 코드를 찾는다.
- 층(ADR-004)이 폴더에 드러나, import 경로만 보고 의존 방향 위반을 알아챈다(아키텍처 테스트로 검사하기도 쉽다).
- 리팩토링 때 구조가 같이 정리돼 "역할은 문서에만 있는" 상태가 다시 생기지 않는다.

### 부정적
- 폴더 깊이가 깊어진다.
- 기존 저장소를 옮기는 동안은 두 모양이 섞인다.

## References

- 배치 출처: 헥사고날(포트와 어댑터) 구현에서 널리 쓰는 `adapter/in`, `adapter/out`, `application/port/in|out`, `application/service`, `domain` 배치(Tom Hombergs, *Get Your Hands Dirty on Clean Architecture* 의 패키지 구조)
- ADR-002, ADR-004, ADR-005, ADR-006, ADR-012, ADR-013, ADR-015
