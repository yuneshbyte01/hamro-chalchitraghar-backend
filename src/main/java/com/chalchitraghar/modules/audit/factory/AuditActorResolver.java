package com.chalchitraghar.modules.audit.factory;

import com.chalchitraghar.modules.audit.enums.AuditActorType;
import com.chalchitraghar.modules.audit.event.AuditActor;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditActorResolver {
    private final UserRepository users;

    public AuditActor currentUserOrSystem() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return system();
        return users.findByEmail(auth.getName()).map(this::user).orElseGet(this::system);
    }

    public AuditActor user(User user) {
        return new AuditActor(
                AuditActorType.USER, user.getId(), user.getEmail(), user.getRole().name());
    }

    public AuditActor userId(Long id) {
        return id == null ? system() : users.findById(id).map(this::user).orElseGet(this::system);
    }

    public AuditActor system() {
        return new AuditActor(AuditActorType.SYSTEM, null, null, "SYSTEM");
    }

    public AuditActor external() {
        return new AuditActor(AuditActorType.EXTERNAL, null, null, null);
    }

    public AuditActor anonymous(String email) {
        return new AuditActor(AuditActorType.ANONYMOUS, null, email, null);
    }
}
