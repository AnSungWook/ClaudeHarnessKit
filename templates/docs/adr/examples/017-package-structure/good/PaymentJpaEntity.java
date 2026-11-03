@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payment")
@Entity
public class PaymentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주문은 다른 기능의 엔티티다. 이 엔티티가 주문 값으로 판단할 일이 없어 연관 대신 키 칸만 든다.
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
