package com.caltalk.backend.kakao;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caltalk.backend.user.User;
import java.util.Optional;

@Service
public class KakaoLinkService {
    private static final char[] CODE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final Duration CODE_LIFETIME = Duration.ofMinutes(5);
    private static final Duration ISSUE_WINDOW = Duration.ofMinutes(10);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ConnectionCodeRepository codes;
    private final KakaoUserLinkRepository links;
    private final String hmacSecret;
    private final String logicalBotKey;

    public KakaoLinkService(ConnectionCodeRepository codes, KakaoUserLinkRepository links,
            @Value("${caltalk.chatbot.kakao.identity-secret:}") String hmacSecret,
            @Value("${caltalk.chatbot.kakao.logical-bot-key:caltalk-main}") String logicalBotKey) {
        this.codes = codes;
        this.links = links;
        this.hmacSecret = hmacSecret;
        this.logicalBotKey = logicalBotKey;
    }

    @Transactional(readOnly = true)
    public LinkStatus status(User user) {
        return links.findByUserId(user.getId())
                .map(link -> new LinkStatus(true, link.getLinkedAt()))
                .orElseGet(() -> new LinkStatus(false, null));
    }

    @Transactional
    public IssuedCode issue(User user) {
        requireSecret();
        if (links.findByUserId(user.getId()).isPresent()) throw new AlreadyLinkedException();
        Instant now = Instant.now();
        if (codes.countByUserIdAndIssuedAtAfter(user.getId(), now.minus(ISSUE_WINDOW)) >= 3) {
            throw new LinkCodeRateLimitedException();
        }
        codes.findByUserIdAndUsedAtIsNullAndInvalidatedAtIsNull(user.getId())
                .forEach(code -> code.invalidate(now));
        String raw;
        String digest;
        do {
            raw = randomCode();
            digest = hmac("code:" + raw);
        } while (codes.findByCodeHmac(digest).isPresent());
        Instant expiresAt = now.plus(CODE_LIFETIME);
        codes.save(new ConnectionCode(user, digest, now, expiresAt));
        return new IssuedCode(raw, expiresAt);
    }

    @Transactional
    public LinkResult consume(String externalUserId, String utterance) {
        requireSecret();
        String externalHmac = hmac("user:" + logicalBotKey + ":" + externalUserId);
        if (links.findByLogicalBotKeyAndExternalUserHmac(logicalBotKey, externalHmac).isPresent()) {
            return LinkResult.ALREADY_CONNECTED;
        }
        String normalized = normalizeCode(utterance);
        if (!normalized.matches("[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{8}")) {
            return LinkResult.NOT_A_CODE;
        }
        ConnectionCode code = codes.findByCodeHmac(hmac("code:" + normalized)).orElse(null);
        Instant now = Instant.now();
        if (code == null || !code.isUsable(now)) return LinkResult.INVALID_OR_EXPIRED;
        if (links.findByUserId(code.getUser().getId()).isPresent()) return LinkResult.ACCOUNT_ALREADY_LINKED;
        links.save(new KakaoUserLink(code.getUser(), logicalBotKey, externalHmac, now));
        code.use(now);
        return LinkResult.CONNECTED;
    }

    @Transactional(readOnly = true)
    public Optional<User> linkedUser(String externalUserId) {
        requireSecret();
        String externalHmac = hmac("user:" + logicalBotKey + ":" + externalUserId);
        return links.findByLogicalBotKeyAndExternalUserHmac(logicalBotKey, externalHmac)
                .map(KakaoUserLink::getUser);
    }

    public String protectedExternalUserKey(String externalUserId) {
        requireSecret();
        return hmac("conversation:" + logicalBotKey + ":" + externalUserId);
    }

    @Transactional
    public void revoke(User user) {
        links.deleteByUserId(user.getId());
        Instant now = Instant.now();
        codes.findByUserIdAndUsedAtIsNullAndInvalidatedAtIsNull(user.getId())
                .forEach(code -> code.invalidate(now));
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to protect Kakao identity", exception);
        }
    }

    private void requireSecret() {
        if (hmacSecret.length() < 32) throw new IllegalStateException("Kakao identity secret must be at least 32 characters");
    }
    private static String normalizeCode(String value) { return value == null ? "" : value.replaceAll("[\\s-]", "").toUpperCase(); }
    private static String randomCode() { StringBuilder value=new StringBuilder(8); for(int i=0;i<8;i++) value.append(CODE_ALPHABET[RANDOM.nextInt(CODE_ALPHABET.length)]); return value.toString(); }

    public record LinkStatus(boolean linked, Instant linkedAt) {}
    public record IssuedCode(String code, Instant expiresAt) {}
    public enum LinkResult { CONNECTED, ALREADY_CONNECTED, ACCOUNT_ALREADY_LINKED, INVALID_OR_EXPIRED, NOT_A_CODE }
    public static class AlreadyLinkedException extends RuntimeException {}
    public static class LinkCodeRateLimitedException extends RuntimeException {}
}
