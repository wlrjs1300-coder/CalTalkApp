package com.caltalk.backend.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caltalk.backend.user.User;
import com.caltalk.backend.user.UserRepository;

@Service
public class SocialLoginService {

    private final SocialIdentityRepository socialIdentityRepository;
    private final UserRepository userRepository;

    public SocialLoginService(
            SocialIdentityRepository socialIdentityRepository,
            UserRepository userRepository
    ) {
        this.socialIdentityRepository = socialIdentityRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public String login(SocialProfile profile) {
        return socialIdentityRepository
                .findUserEmailByProviderAndProviderSubject(profile.provider(), profile.subject())
                .orElseGet(() -> linkNewIdentity(profile));
    }

    private String linkNewIdentity(SocialProfile profile) {
        User user = userRepository.findByEmail(profile.email())
                .orElseGet(() -> userRepository.save(new User(profile.email(), null)));
        socialIdentityRepository.save(new SocialIdentity(
                user,
                profile.provider(),
                profile.subject(),
                profile.email()
        ));
        return user.getEmail();
    }
}
