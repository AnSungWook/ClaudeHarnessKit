// ADR-005 위반: 서비스(application)가 어댑터 층의 영속 엔티티와 Spring Data 저장소를 import 한다(포트를 거치지 않는다)
import com.example.order.adapter.out.persistence.OrderEntity;
import com.example.order.adapter.out.persistence.OrderJpaRepository;

@Service
public class OrderQueryService {

    private final OrderJpaRepository orderJpaRepository;

    // ...

    // ADR-005 위반: 도메인 값이 아니라 영속 엔티티를 서비스 밖으로 낸다
    @Transactional(readOnly = true)
    public OrderEntity getOrder(long orderId) {
        return orderJpaRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
