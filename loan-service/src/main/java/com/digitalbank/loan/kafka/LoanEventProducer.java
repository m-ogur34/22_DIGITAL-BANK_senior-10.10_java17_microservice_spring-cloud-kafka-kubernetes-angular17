package com.digitalbank.loan.kafka;

import com.digitalbank.loan.entity.LoanApplication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Kredi event'lerini Kafka'ya yayınlar.
 *
 * Topic: loan-events
 * Consumer: notification-service → "Krediniz onaylandı" bildirimi
 *
 * Mesaj formatı:
 * {
 *   "loanId": "uuid",
 *   "ownerId": "uuid",
 *   "loanType": "IHTIYAC",
 *   "approvedAmount": "50000.00",
 *   "status": "APPROVED",
 *   "monthlyInstallment": "1250.00"
 * }
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoanEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String LOAN_EVENTS_TOPIC = "loan-events";

    /**
     * Kredi onay event'ini yayınlar.
     * @param loan  Onaylanan kredi başvurusu
     */
    public void publishLoanApproved(LoanApplication loan) {
        Map<String, Object> event = new HashMap<>();
        event.put("loanId", loan.getId().toString());
        event.put("ownerId", loan.getOwnerId().toString());
        event.put("loanType", loan.getLoanType().name());
        event.put("loanTypeName", loan.getLoanType().getDisplayName());
        event.put("approvedAmount", loan.getApprovedAmount().toPlainString());
        event.put("monthlyInstallment", loan.getMonthlyInstallment().toPlainString());
        event.put("termMonths", loan.getTermMonths());
        event.put("status", "APPROVED");

        kafkaTemplate.send(LOAN_EVENTS_TOPIC, loan.getOwnerId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) log.info("Kredi onay event'i gönderildi: {}", loan.getId());
                    else log.error("Kredi event gönderilemedi: {}", ex.getMessage());
                });
    }

    /**
     * Kredi red event'ini yayınlar.
     */
    public void publishLoanRejected(LoanApplication loan) {
        Map<String, Object> event = new HashMap<>();
        event.put("loanId", loan.getId().toString());
        event.put("ownerId", loan.getOwnerId().toString());
        event.put("rejectionReason", loan.getRejectionReason());
        event.put("status", "REJECTED");

        kafkaTemplate.send(LOAN_EVENTS_TOPIC, loan.getOwnerId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) log.error("Kredi red event gönderilemedi: {}", ex.getMessage());
                });
    }
}
