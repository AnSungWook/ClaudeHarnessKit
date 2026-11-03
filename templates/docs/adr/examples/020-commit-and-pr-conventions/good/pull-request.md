## 무엇을, 왜

회원이 결제 전 주문을 고객센터를 거치지 않고 취소할 수 있게 한다. 지금은 상담원이 관리 화면에서 상태를 바꾼다.

| 바뀐 것 | 왜 |
|---|---|
| `POST /orders/{orderId}/cancellations` 창구 | 회원이 직접 취소한다 |
| `OrderStatus.isCancellable()` | 취소할 수 있는 상태의 판단을 enum 한 자리에 둔다(ADR-002) |
| `RefundRequestListener` 가 커밋 뒤 환불을 요청 | 취소가 롤백되면 환불도 안 나가게(ADR-014) |

## 흐름

```text
POST /orders/{orderId}/cancellations → orderController.cancelOrder(orderId, request)
  orderCancellationService.cancelOrder(command)
    orders.findById(orderId)                  없으면 OrderNotFoundException(404)
    order.cancel()                            취소할 수 없는 상태면 OrderNotCancellableException(409)
    orders.save(canceledOrder)
    events.publishEvent(new OrderCanceledEvent(orderId))
커밋 뒤 → refundRequestListener.requestRefundOnOrderCanceled(event)   refundExecutor 에서
```

## 돌린 검증

- `./gradlew :order:test` 통과, 새 테스트 4건(결제 전 취소, 결제 후 409, 없는 주문 404, 커밋 뒤 환불 요청)
- SonarQube: blocker 0, critical 0, major 0, minor 1(고침)
- 기계 검사(`AdrArchitectureTest`, commitlint) 통과

## 안 한 검증

- 통합 테스트 `:order:integrationTest` - 안 함. 로컬에 DB 컨테이너가 없어 CI 결과로 본다.
- 실제 결제사 환불 호출 - 안 함. 개발 환경 결제사 계정이 아직 없다.

## 남긴 것

- 결제 후 취소(부분 환불 포함)는 이 PR 밖이다. 후속 이슈로 남겼다.
