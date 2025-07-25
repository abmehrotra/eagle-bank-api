package com.eaglebank.util;

import com.eaglebank.model.User;
import org.springframework.security.access.AccessDeniedException;

public class AccessValidator {

    public static void validateOwnership(User resourceOwner, User currentUser) {
        if (!resourceOwner.getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Access denied");
        }
    }
}
