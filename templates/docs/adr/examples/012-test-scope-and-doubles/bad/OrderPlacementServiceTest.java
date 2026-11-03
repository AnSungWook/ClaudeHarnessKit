@SpringBootTest // ADR-012 위반: 서비스 흐름 하나를 보려고 컨텍스트 전체를 띄운다
class OrderPlacementServiceTest {

    @MockitoBean
    private OrderRepository orders;
    // ... ReceiptMailSender 도 @MockitoBean, 서비스는 @Autowired. orders.save 는 받은 주문을 돌려주게 해 둔다

    @Test
    void placeOrder() { // ADR-012 위반: 이름이 무엇을 확인하는지 말하지 않는다
        OrderLine line = mock(OrderLine.class); // ADR-012 위반: 값 객체를 목으로 바꾼다 - 진짜 금액 계산을 안 거친다
        when(line.amount()).thenReturn(3_000L);

        orderPlacementService.placeOrder(new PlaceOrderCommand(7L, List.of(line)));

        verify(orders).save(any()); // ADR-012 위반: 불렸는지만 보고, 돌려준 결과도 저장된 값도 단언하지 않는다
    }

    @Test
    void placeOrderReturnsTotalAmountOfLines() {
        OrderPlacementResult result = orderPlacementService.placeOrder(
            new PlaceOrderCommand(7L, List.of(new OrderLine(10L, 2, 1_500))));

        assertThat(result.totalAmount()).isEqualTo(1_500L * 2); // ADR-012 위반: 기대값을 대상 코드와 같은 계산으로 만든다 - 계산이 틀려도 같이 틀린다
    }

    @Test
    void sendsReceiptAfterPlacingOrder() throws InterruptedException {
        orderPlacementService.placeOrder(new PlaceOrderCommand(7L, List.of(new OrderLine(10L, 1, 1_000))));

        Thread.sleep(2_000); // ADR-012 위반: 비동기 결과를 sleep 으로 기다린다 - 느리고 가끔 실패한다
        verify(receiptMailSender).sendReceipt(anyLong());
    }
}
