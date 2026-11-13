package com.diponegoro.ddb.notification;

/** Enum modul Notification (FR-28, FR-29). */
public final class NotificationEntities {
    private NotificationEntities() {}

    public enum Type {
        INCOMING("Transaksi masuk"), OUTGOING("Transaksi keluar"), SECURITY("Keamanan akun"), INFO("Info & promo");
        public final String label;
        Type(String label) { this.label = label; }
    }

    public enum Channel {
        PUSH("Push notification"), IN_APP("Dalam aplikasi"), EMAIL("Email");
        public final String label;
        Channel(String label) { this.label = label; }
    }
}
