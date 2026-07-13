package com.chalchitraghar.modules.audit.entity;

import com.chalchitraghar.modules.audit.enums.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200, unique = true, updatable = false)
    private String eventId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Column(updatable = false)
    private Long actorUserId;

    @Column(length = 320, updatable = false)
    private String actorEmailSnapshot;

    @Column(length = 50, updatable = false)
    private String actorRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private AuditActorType actorType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100, updatable = false)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, updatable = false)
    private AuditCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private AuditSeverity severity;

    @Column(nullable = false, length = 100, updatable = false)
    private String resourceType;

    @Column(updatable = false)
    private Long resourceId;

    @Column(length = 200, updatable = false)
    private String resourceReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private AuditResult result;

    @Column(length = 500, updatable = false)
    private String failureReason;

    @Column(length = 100, updatable = false)
    private String requestId;

    @Column(length = 100, updatable = false)
    private String correlationId;

    @Column(length = 100, updatable = false)
    private String ipAddress;

    @Column(length = 512, updatable = false)
    private String userAgent;

    @Column(length = 16, updatable = false)
    private String httpMethod;

    @Column(length = 500, updatable = false)
    private String requestPath;

    @Lob
    @Column(columnDefinition = "TEXT", updatable = false)
    private String beforeValues;

    @Lob
    @Column(columnDefinition = "TEXT", updatable = false)
    private String afterValues;

    @Lob
    @Column(columnDefinition = "TEXT", updatable = false)
    private String metadata;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
