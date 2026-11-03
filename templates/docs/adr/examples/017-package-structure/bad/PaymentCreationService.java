package com.example.payment.application.service;

import com.example.order.adapter.out.persistence.OrderJpaEntity; // ADR-017 위반: 다른 기능의 영속 엔티티를 application 에서 import 한다
import com.example.order.adapter.out.persistence.OrderJpaRepository; // ADR-017 위반: 다른 기능의 adapter 를 바로 쓴다
// ...

@RequiredArgsConstructor
@Service
public class PaymentCreationService {

    private final OrderJpaRepository orderJpaRepository;
    private final PaymentRepository payments;
    private final Clock clock;

    @Transactional
    public PaymentResult createPayment(PaymentCreateCommand command) {
        OrderJpaEntity order = orderJpaRepository.findById(command.orderId())
            .orElseThrow(() -> new OrderNotFoundException(command.orderId()));
        Payment payment = Payment.requested(order.toOrderSummary(), command.method(), clock.instant());
        return PaymentResult.of(payments.save(payment));
    }
}
