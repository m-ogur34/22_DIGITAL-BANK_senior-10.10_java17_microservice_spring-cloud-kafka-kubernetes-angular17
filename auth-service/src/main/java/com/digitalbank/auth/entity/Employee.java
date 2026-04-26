package com.digitalbank.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Banka çalışanını temsil eder.
 * Çalışanlar hesap dondurma/aktifleştirme gibi yönetimsel işlemleri yapabilir.
 */
@Getter
@Setter
@Entity
@Table(name = "employees", schema = "auth_schema")
@DiscriminatorValue("EMPLOYEE")
@PrimaryKeyJoinColumn(name = "id")
public class Employee extends BaseUser {

    // Sicil numarası — şirket içi benzersiz tanımlayıcı
    @Column(name = "sicil_no", nullable = false, unique = true, length = 20)
    private String sicilNo;

    // Çalıştığı departman: MUSTERI_HIZMETLERI, KREDI, BILGI_TEKNOLOJILERI vb.
    @Column(name = "department", nullable = false, length = 50)
    private String department;

    // Yetki seviyesi: JUNIOR, SENIOR, MANAGER
    @Column(name = "authority_level", nullable = false, length = 20)
    private String authorityLevel;
}
