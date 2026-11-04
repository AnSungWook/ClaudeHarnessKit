@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payment")
@Entity
public class PaymentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주문은 다른 기능의 엔티티라 연관 없이 키 칸만 든다. 주문 값이 필요하면 서비스가 주문 port/in 에서 받는다.
    private long orderId;

    // ...

    public static PaymentJpaEntity of(Payment payment) {
        PaymentJpaEntity entity = new PaymentJpaEntity();
        // 새 결제는 id 가 null 이라 저장 때 채번되고, 이미 있는 결제는 id 를 그대로 옮겨 같은 행을 고친다.
        entity.id = payment.id();
        entity.orderId = payment.orderId();
        // ...
        return entity;
    }

    // ...
}
