package net.urosk.upravnikpredstavnik.service;

import lombok.RequiredArgsConstructor;
import net.urosk.upravnikpredstavnik.data.entity.AuditLog;
import net.urosk.upravnikpredstavnik.data.repository.AuditLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    // AuthenticatedUser tukaj ne potrebujemo več

    @Async
    public void log(String action, Class<?> entityClass, String entityId, String details, String userEmail) {
        AuditLog logEntry = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .userEmail(userEmail != null ? userEmail : "SYSTEM") // Uporabimo posredovan email
                .action(action)
                .entityType(entityClass.getSimpleName())
                .entityId(entityId)
                .details(details)
                .build();
        auditLogRepository.save(logEntry);
    }
}