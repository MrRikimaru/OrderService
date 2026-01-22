package com.example.orderservice.kafka;

import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.event.PaymentEvent;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentKafkaConsumer {

    private final OrderRepository orderRepository;

    @KafkaListener(
            topics = "payment-events",
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handlePaymentEvent(@Payload PaymentEvent paymentEvent,
                                   Acknowledgment acknowledgment) {
        try {
            log.info("Received payment event: {}", paymentEvent);

            if ("CREATE_PAYMENT".equals(paymentEvent.getEventType())) {
                processCreatePaymentEvent(paymentEvent);
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing payment event: {}", e.getMessage(), e);
        }
    }

    private void processCreatePaymentEvent(PaymentEvent paymentEvent) {
        Long orderId = paymentEvent.getOrderId();

        orderRepository.findById(orderId).ifPresentOrElse(
                order -> {
                    OrderStatus newStatus = "SUCCESS".equals(paymentEvent.getStatus())
                            ? OrderStatus.PAID
                            : OrderStatus.PAYMENT_FAILED;

                    order.setStatus(newStatus);
                    orderRepository.save(order);

                    log.info("Order {} status updated to {} based on payment status {}",
                            orderId, newStatus, paymentEvent.getStatus());
                },
                () -> log.warn("Order with ID {} not found for payment event", orderId)
        );
    }
}