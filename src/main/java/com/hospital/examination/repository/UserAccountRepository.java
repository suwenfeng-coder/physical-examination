package com.hospital.examination.repository;

import com.hospital.examination.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    Optional<UserAccount> findByPhone(String phone);
    Optional<UserAccount> findByUsernameOrPhone(String username, String phone);
    boolean existsByUsername(String username);
    boolean existsByPhone(String phone);
}
