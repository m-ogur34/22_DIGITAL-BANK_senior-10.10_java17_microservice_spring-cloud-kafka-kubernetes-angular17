package com.digitalbank.loan.repository;

import com.digitalbank.loan.entity.LoanApplication;
import com.digitalbank.loan.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, UUID> {

    List<LoanApplication> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    List<LoanApplication> findByOwnerIdAndStatus(UUID ownerId, LoanStatus status);

    // Aktif kredi sayısı kontrolü (çok fazla kredi varsa red)
    long countByOwnerIdAndStatusIn(UUID ownerId, List<LoanStatus> statuses);
}
