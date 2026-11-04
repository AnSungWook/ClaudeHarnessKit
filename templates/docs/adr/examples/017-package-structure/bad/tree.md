# 어긋난 배치

같은 두 기능을 흔히 보는 모양으로 놓았다. 줄 끝이 어긋난 까닭이다.

```text
com/example/
├── order/
│   ├── api/                                      ADR-017 위반: api 인데 컨트롤러가 없고 공개 값과 입구가 들어 있다
│   │   ├── OrderPolicy.java
│   │   └── OrderReader.java
│   ├── web/                                      ADR-017 위반: 고른 배치에 없는 폴더 이름(adapter/in/web 자리)
│   │   └── OrderController.java
│   ├── OrderService.java                         ADR-017 위반: 서비스, 엔티티, 설정이 한 폴더에 섞여 있다. 안에 @Scheduled 와 @EventListener 메서드까지 있다(입구는 adapter/in)
│   ├── OrderJpaEntity.java
│   ├── OrderJpaRepository.java
│   └── OrderConfig.java
├── payment/
│   ├── application/
│   │   └── service/
│   │       └── PaymentCreationService.java       ADR-017 위반: order 의 영속 어댑터를 바로 import 한다
│   └── adapter/
│       └── out/
│           └── persistence/
│               ├── PaymentJpaEntity.java         ADR-017 위반: 다른 기능 엔티티에 @ManyToOne 과 getter
│               └── PaymentMapper.java            ADR-017 위반: SQL 매퍼가 아닌 객체 변환 Mapper 클래스(ADR-002 의 of(원본) 자리)
└── common/                                       ADR-017 위반: 뭉뚱그린 폴더 이름
    └── util/
        └── DateUtil.java
```
