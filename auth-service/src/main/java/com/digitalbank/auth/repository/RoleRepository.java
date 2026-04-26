package com.digitalbank.auth.repository;

import com.digitalbank.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Rol yönetimi için Repository.
 * Roller seed data ile oluşturulur, runtime'da yeni rol eklenmez.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /**
     * Rol adına göre rol entity'sini bulur.
     * Kullanım: roleRepository.findByName(Role.RoleName.ROLE_CUSTOMER)
     */
    Optional<Role> findByName(Role.RoleName name);
}
