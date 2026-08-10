package com.caltalk.backend.notification;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.caltalk.backend.user.User;

interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
    Optional<PushSubscription> findByEndpoint(String endpoint);
    List<PushSubscription> findAllByUser(User user);
    long deleteByEndpointAndUser(String endpoint, User user);
}
