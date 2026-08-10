package com.caltalk.backend.notification;

import java.io.IOException;

final class PushSubscriptionExpiredException extends IOException {
    PushSubscriptionExpiredException(String message) {
        super(message);
    }
}
