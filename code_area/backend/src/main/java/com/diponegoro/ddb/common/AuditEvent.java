package com.diponegoro.ddb.common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Satu kejadian pada jejak audit keamanan (NFR-07). */
@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "user_id")
    public String userId;
    public String action;
    public String detail;
    @Column(name = "ip_address")
    public String ipAddress;
    @Column(name = "created_at")
    public Instant createdAt;
}
