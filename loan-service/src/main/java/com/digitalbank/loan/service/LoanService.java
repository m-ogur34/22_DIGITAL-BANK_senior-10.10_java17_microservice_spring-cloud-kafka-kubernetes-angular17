package com.digitalbank.loan.service;

import com.digitalbank.loan.dto.InstallmentPlanResponse;
import com.digitalbank.loan.dto.LoanApplicationRequest;
import com.digitalbank.loan.dto.LoanApplicationResponse;
import com.digitalbank.loan.entity.Installment;
import com.digitalbank.loan.entity.LoanApplication;
import com.digitalbank.loan.enums.LoanStatus;
import com.digitalbank.loan.kafka.LoanEventProducer;
import com.digitalbank.loan.repository.InstallmentRepository;
import com.digitalbank.loan.repository.LoanApplicationRepository;
import com.digitalbank.loan.service.calculator.*;
import com.digitalbank.common.exception.LoanApplicationException;
import com.digitalbank.common.util.MoneyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Kredi iş mantığı servisi.
 *
 * Decorator Pattern kullanımı:
 * Sigorta istendiyse: DosyaMasrafiDecorator(SigortaDecorator(BaseLoanCalculator))
 * İstenmezse:         DosyaMasrafiDecorator(BaseLoanCalculator)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanApplicationRepository loanRepo;
    private final InstallmentRepository installmentRepo;
    private final CreditScoreService creditScoreService;
    private final LoanEventProducer eventProducer;
    private final BaseLoanCalculator baseLoanCalculator;

    /**
     * Kredi başvurusu işleme.
     * 1. Maksimum tutar kontrolü
     * 2. Aktif kredi sayısı kontrolü
     * 3. Kredi skoru hesaplama
     * 4. Onay/red kararı
     * 5. Taksit planı üretimi
     * 6. Kafka event yayını
     */
    @Transactional
    public LoanApplicationResponse apply(LoanApplicationRequest request, UUID ownerId,
                                          BigDecimal monthlyIncome) {

        // 1. Maksimum tutar kontrolü
        if (request.getAmount().compareTo(request.getLoanType().getMaxAmount()) > 0) {
            throw new LoanApplicationException(
                String.format("Maksimum kredi tutarı: %s TL",
                    request.getLoanType().getMaxAmount().toPlainString())
            );
        }

        // 2. Mevcut kredi sayısı kontrolü (maks 3 aktif kredi)
        long activeLoans = loanRepo.countByOwnerIdAndStatusIn(
                ownerId, List.of(LoanStatus.APPROVED, LoanStatus.DISBURSED));
        if (activeLoans >= 3) {
            throw new LoanApplicationException("Maksimum aktif kredi sayısına ulaşıldı (3)");
        }

        // 3. Kredi skoru hesapla (mevcut aylık taksit bilgisi simüle edildi)
        BigDecimal existingDebt = BigDecimal.ZERO; // Gerçekte KKB'den sorgulanır
        int creditScore = creditScoreService.calculateScore(monthlyIncome, existingDebt, request);

        // LoanApplication entity oluştur
        LoanApplication loan = LoanApplication.builder()
                .ownerId(ownerId)
                .ownerIban(request.getDisbursementIban())
                .loanType(request.getLoanType())
                .requestedAmount(request.getAmount())
                .termMonths(request.getTermMonths())
                .creditScore(creditScore)
                .status(LoanStatus.PENDING)
                .build();

        // 4. Onay/red kararı
        if (!creditScoreService.isEligible(creditScore)) {
            loan.setStatus(LoanStatus.REJECTED);
            loan.setRejectionReason(String.format(
                "Kredi skoru yetersiz: %d/1000 (minimum: 400)", creditScore));
            LoanApplication saved = loanRepo.save(loan);
            eventProducer.publishLoanRejected(saved);
            log.info("Kredi reddedildi: ownerId={}, skor={}", ownerId, creditScore);
            return mapToResponse(saved);
        }

        // 5. Kredi koşullarını hesapla
        BigDecimal annualRate = request.getLoanType().getMonthlyInterestRate()
                .multiply(BigDecimal.valueOf(12));
        BigDecimal monthlyInstallment = MoneyUtils.calculateMonthlyInstallment(
                request.getAmount(), annualRate, request.getTermMonths());

        // Decorator pattern ile toplam maliyet hesapla
        LoanCalculator calculator = buildCalculator(request);
        BigDecimal totalPayment = calculator.hesapla(request);

        loan.setStatus(LoanStatus.APPROVED);
        loan.setApprovedAmount(request.getAmount());
        loan.setAnnualInterestRate(annualRate);
        loan.setMonthlyInstallment(monthlyInstallment);
        loan.setTotalPayment(totalPayment);

        LoanApplication saved = loanRepo.save(loan);

        // 6. Taksit planı üret
        List<Installment> installments = generateInstallmentPlan(saved);
        installmentRepo.saveAll(installments);

        // 7. Kafka event
        eventProducer.publishLoanApproved(saved);

        log.info("Kredi onaylandı: id={}, tutar={}, aylıkTaksit={}",
                saved.getId(), saved.getApprovedAmount(), saved.getMonthlyInstallment());

        return mapToResponse(saved);
    }

    /**
     * Decorator zinciri oluşturur — Decorator Pattern.
     *
     * Decorator Pattern:
     *   Temel hesaplayıcıyı (BaseLoanCalculator) sarmalar.
     *   Her Decorator ek maliyet ekler ve altındaki hesaplamayı çağırır.
     *
     * Zincir örneği (sigorta istendiyse):
     *   DosyaMasrafiDecorator
     *     └── SigortaDecorator
     *           └── BaseLoanCalculator
     *
     * hesapla() çağrıldığında:
     *   DosyaMasrafi.hesapla() → SigortaDecorator.hesapla() → Base.hesapla()
     *   = anaparaxfaiz + sigorta masrafı + dosya masrafı
     *
     * Neden if-else değil Decorator?
     *   if(sigorta && dosya) → ...
     *   if(!sigorta && dosya) → ...
     *   Yeni masraf tipi eklenince her kombinasyon büyür (2^N).
     *   Decorator: Her masraf bağımsız — birbirini etkilemez, kolayca eklenir.
     */
    private LoanCalculator buildCalculator(LoanApplicationRequest request) {
        LoanCalculator calc = baseLoanCalculator;
        if (request.isSigortaIsteniyor()) {
            calc = new SigortaDecorator(calc); // Ana kredi sigortası ekle
        }
        calc = new DosyaMasrafiDecorator(calc); // Her kredide dosya masrafı zorunlu
        return calc;
    }

    /**
     * Aylık eşit taksit planı üretir — Eşit Taksitli Ödeme (Annuity) Yöntemi.
     *
     * Annuity (Eşit Taksit) formülü:
     *   Taksit = Anapara × [r(1+r)^n] / [(1+r)^n - 1]
     *   r = aylık faiz oranı (örn: %1.5 → 0.015)
     *   n = taksit sayısı
     *   Bu MoneyUtils.calculateMonthlyInstallment() ile hesaplanır.
     *
     * Taksit dağılımı (her ay nasıl hesaplanır?):
     *   1. Faiz payı = Kalan anapara × aylık faiz oranı
     *      İlk ay: 100.000 × 0.015 = 1.500 TL faiz
     *   2. Anapara payı = Taksit - Faiz payı
     *      Taksit 4.000 TL ise: 4.000 - 1.500 = 2.500 TL anapara
     *   3. Kalan anapara azalır: 100.000 - 2.500 = 97.500 TL
     *   4. Sonraki ay: 97.500 × 0.015 = 1.462 TL faiz (daha az!)
     *   → İlerleyen taksitlerde faiz payı azalır, anapara payı artar.
     *   → Toplam taksit miktarı sabit kalır.
     *
     * Son taksit yuvarlama düzeltmesi:
     *   BigDecimal hesaplamalarında küçük yuvarlama hataları birikir.
     *   Son taksitte kalan anaparayı tam olarak kullan.
     *   Aksi halde: son ödeme sonrası 0.01-0.02 TL artık bakiye kalabilir.
     */
    private List<Installment> generateInstallmentPlan(LoanApplication loan) {
        List<Installment> plan = new ArrayList<>();
        BigDecimal remainingPrincipal = loan.getApprovedAmount(); // Başlangıç anapara
        // Aylık faiz oranı: %18 yıllık → %1.5 aylık → 0.015
        BigDecimal monthlyRate = loan.getLoanType().getMonthlyInterestRate()
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        for (int i = 1; i <= loan.getTermMonths(); i++) {
            // 1. Faiz payı = Bu ay kalan anaparaya uygulanan faiz
            BigDecimal interestAmount = remainingPrincipal.multiply(monthlyRate)
                    .setScale(2, RoundingMode.HALF_UP);
            // 2. Anapara payı = Sabit taksit - faiz payı
            BigDecimal principalAmount = loan.getMonthlyInstallment().subtract(interestAmount)
                    .setScale(2, RoundingMode.HALF_UP);
            // 3. Son taksit yuvarlama düzeltmesi
            //    BigDecimal kusuratları biriktirince son ayda 0.01-0.02 TL artık kalabilir.
            //    Çözüm: Son taksitte principalAmount = kalan tam anapara.
            if (i == loan.getTermMonths()) {
                principalAmount = remainingPrincipal; // Kalan her kuruşu öde
            }
            // 4. Kalan anapara güncelle (bir sonraki taksit hesabı için)
            remainingPrincipal = remainingPrincipal.subtract(principalAmount)
                    .setScale(2, RoundingMode.HALF_UP);

            Installment installment = Installment.builder()
                    .loanApplication(loan)
                    .installmentNumber(i)
                    .dueDate(LocalDate.now().plusMonths(i)) // Vade tarihi: bugün + i ay
                    .amount(loan.getMonthlyInstallment())   // Sabit taksit tutarı
                    .principalAmount(principalAmount)       // Bu taksitte ödenen anapara
                    .interestAmount(interestAmount)         // Bu taksitte ödenen faiz
                    // max(ZERO): Son taksit yuvarlama sonrası -0.01 olabilir → 0 yap
                    .remainingPrincipal(remainingPrincipal.max(BigDecimal.ZERO))
                    .paid(false)
                    .build();
            plan.add(installment);
        }
        return plan;
    }

    /**
     * Kullanıcının kredi başvurularını listeler.
     */
    @Transactional(readOnly = true)
    public List<LoanApplicationResponse> getMyLoans(UUID ownerId) {
        return loanRepo.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Taksit planını döner.
     */
    @Transactional(readOnly = true)
    public InstallmentPlanResponse getInstallmentPlan(UUID loanId) {
        LoanApplication loan = loanRepo.findById(loanId)
                .orElseThrow(() -> new com.digitalbank.common.exception.AccountNotFoundException(loanId.toString()));

        List<Installment> installments = installmentRepo
                .findByLoanApplicationIdOrderByInstallmentNumber(loanId);

        List<InstallmentPlanResponse.InstallmentDto> dtos = installments.stream()
                .map(inst -> InstallmentPlanResponse.InstallmentDto.builder()
                        .number(inst.getInstallmentNumber())
                        .dueDate(inst.getDueDate())
                        .amount(inst.getAmount())
                        .principalAmount(inst.getPrincipalAmount())
                        .interestAmount(inst.getInterestAmount())
                        .remainingPrincipal(inst.getRemainingPrincipal())
                        .paid(inst.isPaid())
                        .paymentDate(inst.getPaymentDate())
                        .build())
                .collect(Collectors.toList());

        return InstallmentPlanResponse.builder()
                .loanApplicationId(loanId)
                .totalAmount(loan.getTotalPayment())
                .monthlyInstallment(loan.getMonthlyInstallment())
                .termMonths(loan.getTermMonths())
                .installments(dtos)
                .build();
    }

    private LoanApplicationResponse mapToResponse(LoanApplication loan) {
        return LoanApplicationResponse.builder()
                .applicationId(loan.getId())
                .loanType(loan.getLoanType())
                .loanTypeName(loan.getLoanType().getDisplayName())
                .requestedAmount(loan.getRequestedAmount())
                .approvedAmount(loan.getApprovedAmount())
                .termMonths(loan.getTermMonths())
                .annualInterestRate(loan.getAnnualInterestRate())
                .monthlyInstallment(loan.getMonthlyInstallment())
                .totalPayment(loan.getTotalPayment())
                .status(loan.getStatus())
                .statusName(loan.getStatus().getDisplayName())
                .creditScore(loan.getCreditScore())
                .rejectionReason(loan.getRejectionReason())
                .appliedAt(loan.getCreatedAt())
                .build();
    }
}
