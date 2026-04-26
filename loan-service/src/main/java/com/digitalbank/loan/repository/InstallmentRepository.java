package com.digitalbank.loan.repository;

import com.digitalbank.loan.entity.Installment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface InstallmentRepository extends JpaRepository<Installment, UUID> {

    List<Installment> findByLoanApplicationIdOrderByInstallmentNumber(UUID loanId);

    // Vadesi gelen ve ödenmemiş taksitler (hatırlatma için)
    List<Installment> findByDueDateBeforeAndPaidFalse(LocalDate date);
}
