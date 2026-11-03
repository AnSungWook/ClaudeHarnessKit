package com.example.payment.application.service;

import com.example.order.application.port.in.GetOrderSummaryUseCase;
import com.example.order.domain.OrderSummary;
// ...

@RequiredArgsConstructor
@Service
public class PaymentCreationService {

    private final GetOrderSummaryUseCase orderSummaries;
    private final PaymentRepository payments;
    private final Clock clock;

    @Transactional
    public PaymentResult createPayment(PaymentCreateCommand command) {
        OrderSummary order = orderSummaries.getOrderSummary(command.orderId());
        Payment payment = Payment.requested(order, command.method(), clock.instant());
        return PaymentResult.of(payments.save(payment));
    }
}
