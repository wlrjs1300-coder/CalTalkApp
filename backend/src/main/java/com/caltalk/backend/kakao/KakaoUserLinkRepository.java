package com.caltalk.backend.kakao;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
public interface KakaoUserLinkRepository extends JpaRepository<KakaoUserLink,Long>{
 Optional<KakaoUserLink> findByUserId(Long userId);
 @EntityGraph(attributePaths = "user")
 Optional<KakaoUserLink> findByLogicalBotKeyAndExternalUserHmac(String logicalBotKey,String externalUserHmac);
 void deleteByUserId(Long userId);
}
