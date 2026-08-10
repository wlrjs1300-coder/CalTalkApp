package com.caltalk.backend.kakao;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
public interface ConnectionCodeRepository extends JpaRepository<ConnectionCode,Long>{
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 Optional<ConnectionCode> findByCodeHmac(String codeHmac);
 List<ConnectionCode> findByUserIdAndUsedAtIsNullAndInvalidatedAtIsNull(Long userId);
 long countByUserIdAndIssuedAtAfter(Long userId, Instant since);
}
