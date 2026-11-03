@Getter // ADR-017 위반: 연관 칸에도 getter 가 생겨 서비스가 payment.getOrder().getXxx() 로 주문 엔티티를 넘나든다
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payment")
@Entity
public class PaymentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // ADR-017 위반: 이 엔티티의 메서드가 주문 값으로 판단하지 않는데 다른 기능 엔티티에 연관을 건다
    @JoinColumn(name = "order_id") // ADR-017 위반: 키 칸이 따로 없어 연관만이 주문을 가리킨다
    private OrderJpaEntity order;

    private long amount;
}
