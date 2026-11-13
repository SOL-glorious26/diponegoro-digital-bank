package com.diponegoro.ddb.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;

/** Pilihan jenis & kanal notifikasi per pengguna (FR-28). Disimpan sebagai daftar dipisah koma. */
@Entity
@Table(name = "notification_settings")
public class NotificationSettings {
    @Id
    @Column(name = "user_id")
    public String userId;
    public String types;
    public String channels;

    public interface Repo extends JpaRepository<NotificationSettings, String> {}
}
