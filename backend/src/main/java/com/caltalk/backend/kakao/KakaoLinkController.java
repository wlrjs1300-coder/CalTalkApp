package com.caltalk.backend.kakao;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.caltalk.backend.kakao.KakaoLinkService.AlreadyLinkedException;
import com.caltalk.backend.kakao.KakaoLinkService.LinkCodeRateLimitedException;
import com.caltalk.backend.schedule.CurrentScheduleUserService;
import com.caltalk.backend.user.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v1/kakao")
public class KakaoLinkController {
    private final KakaoLinkService service;
    private final CurrentScheduleUserService currentUser;
    public KakaoLinkController(KakaoLinkService service, CurrentScheduleUserService currentUser){this.service=service;this.currentUser=currentUser;}

    @GetMapping("/link")
    ResponseEntity<KakaoLinkService.LinkStatus> status(Authentication auth,HttpServletRequest req,HttpServletResponse res){
        User user=currentUser.requireCurrentUser(auth,req,res);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.status(user));
    }
    @PostMapping("/link-codes")
    ResponseEntity<?> issue(Authentication auth,HttpServletRequest req,HttpServletResponse res){
        User user=currentUser.requireCurrentUser(auth,req,res);
        try { return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(service.issue(user)); }
        catch(AlreadyLinkedException e){ return ResponseEntity.status(HttpStatus.CONFLICT).body(new LinkError("KAKAO_ALREADY_LINKED","이미 카카오톡과 연결되어 있습니다.")); }
        catch(LinkCodeRateLimitedException e){ return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new LinkError("LINK_CODE_RATE_LIMITED","잠시 후 다시 시도해 주세요.")); }
    }
    @PostMapping("/links/revoke")
    ResponseEntity<Void> revoke(Authentication auth,HttpServletRequest req,HttpServletResponse res){
        service.revoke(currentUser.requireCurrentUser(auth,req,res));
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
    record LinkError(String code,String message){}
}
