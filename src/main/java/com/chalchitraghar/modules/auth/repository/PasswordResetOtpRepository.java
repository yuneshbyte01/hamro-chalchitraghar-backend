package com.chalchitraghar.modules.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chalchitraghar.modules.auth.entity.PasswordResetOtp;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    Optional<PasswordResetOtp> findFirstByUserEmailAndUsedAtIsNullOrderByCreatedAtDesc(String email);

    List<PasswordResetOtp> findByUserIdAndUsedAtIsNull(Long userId);
}
