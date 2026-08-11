package com.caltalk.backend.chatbot;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.caltalk.backend.kakao.KakaoLinkService;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/v1/kakao")
public class KakaoChatbotController {
    private static final Logger log = LoggerFactory.getLogger(KakaoChatbotController.class);
    static final String SECRET_HEADER = "X-CalTalk-Skill-Secret";
    static final String LINK_REQUIRED_MESSAGE = """
            🔗 CalTalk 계정 연결이 필요합니다.

            1. CalTalk 앱에서 설정을 엽니다.
            2. 8자리 연결 코드를 발급합니다.
            3. 이 채팅방에 코드를 입력합니다.""";
    static final String LINK_COMPLETED_MESSAGE = """
            ✅ CalTalk 계정 연결을 완료했습니다.

            이제 카카오톡에서 일정을
            확인하고 관리할 수 있습니다.""";
    private final boolean enabled;
    private final String configuredSecret;
    private final KakaoLinkService linkService;
    private final KakaoScheduleAssistantService assistantService;
    private final KakaoCallbackService callbackService;

    public KakaoChatbotController(
            @Value("${caltalk.chatbot.kakao.enabled:false}") boolean enabled,
            @Value("${caltalk.chatbot.kakao.skill-secret:}") String configuredSecret,
            KakaoLinkService linkService,
            KakaoScheduleAssistantService assistantService,
            KakaoCallbackService callbackService) {
        this.enabled=enabled; this.configuredSecret=configuredSecret; this.linkService=linkService;
        this.assistantService=assistantService;
        this.callbackService=callbackService;
    }

    @PostMapping("/skill")
    public ResponseEntity<?> skill(@RequestHeader(value=SECRET_HEADER,required=false) String suppliedSecret,
            @RequestBody JsonNode payload) {
        if(!enabled) return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(KakaoSkillResponseFactory.response("CalTalk 카카오톡 연결이 아직 활성화되지 않았어요."));
        if(!validSecret(suppliedSecret)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if(!validPayload(payload)) return ResponseEntity.badRequest()
                .body(KakaoSkillResponseFactory.response("요청 내용을 확인할 수 없어요. 잠시 후 다시 시도해 주세요."));
        JsonNode request=payload.path("userRequest");
        KakaoLinkService.LinkResult result=linkService.consume(
                request.path("user").path("id").asText(), request.path("utterance").asText());
        String callbackUrl=request.path("callbackUrl").asText("");
        String utterance=request.path("utterance").asText();
        boolean callbackSupported=callbackService.supports(callbackUrl);
        log.info("Kakao request intent={}, callback metadata present={}, supported={}",
                payload.path("intent").path("name").asText("<unknown>"),
                !callbackUrl.isBlank(), callbackSupported);
        if(result==KakaoLinkService.LinkResult.ALREADY_CONNECTED && callbackSupported
                && !isFastFollowUp(utterance)){
            var directReply = callbackService.replyWithinOrDispatch(callbackUrl,
                    request.path("user").path("id").asText(), utterance, Duration.ofMillis(3_500));
            if (directReply.isPresent()) {
                return ResponseEntity.ok(KakaoSkillResponseFactory.response(directReply.get()));
            }
            return ResponseEntity.ok(new KakaoCallbackAccepted("2.0", true));
        }
        String message=switch(result){
            case CONNECTED -> LINK_COMPLETED_MESSAGE;
            case ALREADY_CONNECTED -> assistantService.reply(
                    request.path("user").path("id").asText(), request.path("utterance").asText());
            case ACCOUNT_ALREADY_LINKED -> "이 CalTalk 계정은 이미 다른 카카오톡 사용자와 연결되어 있어요. CalTalk 설정에서 기존 연결을 해제해 주세요.";
            case INVALID_OR_EXPIRED -> "연결 코드가 올바르지 않거나 만료됐어요. CalTalk 설정에서 새 코드를 발급해 주세요.";
            case NOT_A_CODE -> LINK_REQUIRED_MESSAGE;
        };
        return ResponseEntity.ok(KakaoSkillResponseFactory.response(message));
    }

    private boolean validSecret(String supplied){
        return !configuredSecret.isBlank() && supplied!=null && MessageDigest.isEqual(
                configuredSecret.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8));
    }
    private static boolean validPayload(JsonNode payload){
        JsonNode request=payload==null?null:payload.get("userRequest");
        return request!=null && request.isObject() && request.path("utterance").isString()
                && request.path("user").path("id").isString();
    }
    static boolean isFastFollowUp(String utterance) {
        String value = utterance == null ? "" : utterance.replaceAll("[\\s.!?]", "");
        if (value.matches("확인|네|응|등록해줘|취소|아니|아니요")) return true;
        if (value.matches("\\d{1,2}(번)?")) return true;
        if (value.matches("(첫|두|세|네|다섯)번째")) return true;
        if (value.matches("(오전|오후)일정")) return true;
        return value.matches("\\d{1,2}:\\d{2}");
    }
    record KakaoCallbackAccepted(String version,boolean useCallback){}
}
