# 바른 배치 (기본 배치)

주문(`order`)과 결제(`payment`) 두 기능. 결제는 주문의 `port/in` 과 `domain` 공개 값으로만 주문을 본다.

```text
com/example/
├── order/
│   ├── domain/
│   │   ├── Order.java
│   │   ├── OrderStatus.java
│   │   ├── OrderSummary.java                  다른 기능에 공개하는 값
│   │   └── exception/
│   │       └── OrderNotFoundException.java
│   ├── application/
│   │   ├── port/
│   │   │   ├── in/
│   │   │   │   └── GetOrderSummaryUseCase.java   다른 기능이 부르는 입구
│   │   │   └── out/
│   │   │       └── OrderRepository.java
│   │   └── service/
│   │       └── OrderQueryService.java            GetOrderSummaryUseCase 구현
│   ├── adapter/
│   │   ├── in/
│   │   │   └── web/
│   │   │       ├── OrderController.java
│   │   │       └── OrderResponse.java
│   │   └── out/
│   │       └── persistence/
│   │           ├── OrderJpaEntity.java
│   │           ├── OrderJpaRepository.java       Spring Data 리포지토리
│   │           └── JpaOrderRepository.java       OrderRepository 포트 구현
│   └── config/
│       └── OrderConfig.java
├── payment/
│   ├── domain/
│   │   ├── Payment.java
│   │   └── exception/
│   ├── application/
│   │   ├── port/
│   │   │   └── out/
│   │   │       └── PaymentRepository.java
│   │   └── service/
│   │       └── PaymentCreationService.java       order 의 port/in 만 부른다
│   └── adapter/
│       ├── in/
│       │   └── web/
│       │       └── PaymentController.java        입구가 하나라 port/in 없이 서비스를 바로 부른다
│       └── out/
│           └── persistence/
│               └── PaymentJpaEntity.java         주문은 키 칸 orderId 로 든다
└── shared/
    └── exception/
        ├── BusinessException.java
        ├── NotFoundException.java                상태 공통 형제(ADR-006)
        └── ConflictException.java
```

- `payment` 에는 외부 호출이 없어 `adapter/out/client/` 를 만들지 않았다. 바깥 입구가 하나라 `port/in/` 도 없다(작은 기능은 폴더를 줄여도 된다).
- `shared/` 에는 여러 기능이 같이 쓰는 것만 둔다.
