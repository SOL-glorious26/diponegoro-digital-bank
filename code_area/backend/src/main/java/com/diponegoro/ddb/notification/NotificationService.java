package com.diponegoro.ddb.notification;

import com.diponegoro.ddb.auth.AuthRepositories.UserRepository;
import com.diponegoro.ddb.common.ApiException;
import com.diponegoro.ddb.common.Ids;
import com.diponegoro.ddb.common.Money;
import com.diponegoro.ddb.ledger.LedgerModels.Direction;
import com.diponegoro.ddb.ledger.LedgerModels.TransactionPostedEvent;
import com.diponegoro.ddb.ledger.LedgerModels.TransactionStatusChangedEvent;
import com.diponegoro.ddb.ledger.LedgerModels.TxCategory;
import com.diponegoro.ddb.ledger.LedgerModels.TxStatus;
import com.diponegoro.ddb.ledger.LedgerRepositories.TransactionRepository;
import com.diponegoro.ddb.ledger.LedgerTransaction;
import com.diponegoro.ddb.notification.NotificationEntities.Channel;
import com.diponegoro.ddb.notification.NotificationEntities.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * FR-29: notifikasi transaksi masuk & keluar lewat kanal pilihan pengguna (FR-28).
 * Asinkron: ledger hanya menerbitkan event; notifikasi dibuat SETELAH transaksi commit, di thread lain,
 * sehingga transfer tidak menunggu dan notifikasi tidak terkirim untuk transaksi yang dibatalkan.
 *
 * Kanal: IN_APP = kotak notifikasi; PUSH = antrean yang diambil aplikasi lalu ditampilkan sebagai
 * notifikasi Android (FCM bisa ditambahkan kemudian); EMAIL = outbox simulasi.
 */
@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    public static final Set<Type> DEFAULT_TYPES = EnumSet.allOf(Type.class);
    public static final Set<Channel> DEFAULT_CHANNELS = EnumSet.of(Channel.PUSH, Channel.IN_APP);

    public record SettingsDto(Set<Type> types, Set<Channel> channels) {}

    public record NotificationDto(String id, Type type, String title, String message, String transactionId,
                                  boolean read, Instant createdAt) {}

    public record NotificationPage(List<NotificationDto> items, long unread, int page, boolean hasMore) {}

    public record EmailDto(String id, String to, String subject, String body, Instant sentAt) {}

    private final Notification.Repo notifications;
    private final NotificationSettings.Repo settings;
    private final EmailMessage.Repo emails;
    private final UserRepository users;
    private final TransactionRepository transactions;
    private final Clock clock;

    public NotificationService(Notification.Repo notifications, NotificationSettings.Repo settings, EmailMessage.Repo emails,
                               UserRepository users, TransactionRepository transactions, Clock clock) {
        this.notifications = notifications;
        this.settings = settings;
        this.emails = emails;
        this.users = users;
        this.transactions = transactions;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ pengaturan (FR-28)

    public SettingsDto settings(String userId) {
        return settings.findById(userId)
                .map(s -> new SettingsDto(parse(s.types, Type.class), parse(s.channels, Channel.class)))
                .orElse(new SettingsDto(DEFAULT_TYPES, DEFAULT_CHANNELS));
    }

    @Transactional
    public SettingsDto updateSettings(String userId, Set<Type> types, Set<Channel> channels) {
        if (channels == null || channels.isEmpty()) {
            throw ApiException.badRequest("CHANNEL_REQUIRED", "Pilih minimal satu kanal notifikasi");
        }
        NotificationSettings s = settings.findById(userId).orElseGet(NotificationSettings::new);
        s.userId = userId;
        s.types = join(types == null ? Set.of() : types);
        s.channels = join(channels);
        settings.save(s);
        return settings(userId);
    }

    // ------------------------------------------------------------------ kotak notifikasi

    public NotificationPage list(String userId, int page) {
        Page<Notification> p = notifications.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(Math.max(page, 0), 30));
        return new NotificationPage(p.getContent().stream().map(NotificationService::toDto).toList(),
                notifications.countByUserIdAndReadFalse(userId), p.getNumber(), p.hasNext());
    }

    public long unreadCount(String userId) {
        return notifications.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(String userId, String id) {
        notifications.findByIdAndUserId(id, userId).ifPresent(n -> n.read = true);
    }

    @Transactional
    public int markAllRead(String userId) {
        return notifications.markAllRead(userId);
    }

    /** Diambil aplikasi (polling) lalu ditampilkan sebagai push notification lokal; sekali ambil. */
    @Transactional
    public List<NotificationDto> takePendingPush(String userId) {
        List<Notification> pending = notifications.findTop20ByUserIdAndPushedFalseOrderByCreatedAt(userId);
        pending.forEach(n -> n.pushed = true);
        return pending.stream().map(NotificationService::toDto).toList();
    }

    public List<EmailDto> emails(String userId) {
        return emails.findTop50ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(e -> new EmailDto(e.id, e.toAddress, e.subject, e.body, e.createdAt)).toList();
    }

    // ------------------------------------------------------------------ pengiriman

    /** Dipakai juga oleh modul lain untuk notifikasi keamanan (ubah password/PIN, profil). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notify(String userId, Type type, String title, String message, String txId) {
        SettingsDto pref = settings(userId);
        if (!pref.types().contains(type)) return;
        Notification n = new Notification();
        n.id = Ids.newId("notif");
        n.userId = userId;
        n.type = type;
        n.title = title;
        n.message = message;
        n.txId = txId;
        // Riwayat selalu tersimpan; kanal IN_APP menentukan apakah ditandai belum dibaca.
        n.read = !pref.channels().contains(Channel.IN_APP);
        n.pushed = !pref.channels().contains(Channel.PUSH);
        n.createdAt = clock.instant();
        notifications.save(n);
        if (pref.channels().contains(Channel.EMAIL)) {
            users.findById(userId).map(u -> u.email).filter(e -> e != null && !e.isBlank()).ifPresent(address -> {
                EmailMessage e = new EmailMessage();
                e.id = Ids.newId("mail");
                e.userId = userId;
                e.toAddress = address;
                e.subject = title;
                e.body = message;
                e.createdAt = clock.instant();
                emails.save(e);
            });
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPosted(TransactionPostedEvent event) {
        handle(event.txId(), false);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(TransactionStatusChangedEvent event) {
        handle(event.txId(), true);
    }

    /**
     * Membuat notifikasi berdasarkan status transaksi SETELAH commit. Event "posted" membawa status akhir
     * transaksi pada request tersebut; event "status berubah" hanya datang untuk perubahan di request lain
     * (mis. cek ulang status transfer yang timeout).
     */
    void handle(String txId, boolean statusChanged) {
        try {
            LedgerTransaction tx = transactions.findById(txId).orElse(null);
            if (tx == null || tx.category == TxCategory.POCKET) return;  // perpindahan antar-kantong sendiri
            if (tx.createdAt.isBefore(clock.instant().minus(java.time.Duration.ofHours(1)))) return; // data historis/seed
            String debit = tx.debitUserId;
            String credit = tx.creditUserId;
            if (debit != null && tx.directionFor(debit) == Direction.OUT) {
                switch (tx.status) {
                    case SUCCESS -> notify(debit, Type.OUTGOING, tx.title + " berhasil",
                            Money.rupiah(tx.total()) + " ke " + tx.toName + ". Ref " + tx.reference, tx.id);
                    case PROCESSING -> notify(debit, Type.OUTGOING, tx.title + " sedang diproses",
                            Money.rupiah(tx.total()) + " ke " + tx.toName + ". Ref " + tx.reference, tx.id);
                    case FAILED -> notify(debit, Type.OUTGOING, tx.title + " gagal",
                            (tx.statusReason == null ? "Transaksi ditolak" : tx.statusReason) + ". Dana "
                                    + Money.rupiah(tx.total()) + " dikembalikan ke rekening Anda.", tx.id);
                }
            }
            if (credit != null && !credit.equals(debit) && tx.status == TxStatus.SUCCESS) {
                notify(credit, Type.INCOMING, "Dana masuk " + Money.rupiah(tx.amount),
                        "Dari " + tx.fromName + " (" + tx.title + "). Ref " + tx.reference, tx.id);
            }
        } catch (RuntimeException e) {
            log.warn("Gagal membuat notifikasi untuk transaksi {}", txId, e);   // notifikasi tidak boleh menggagalkan apa pun
        }
    }

    private static NotificationDto toDto(Notification n) {
        return new NotificationDto(n.id, n.type, n.title, n.message, n.txId, n.read, n.createdAt);
    }

    private static <E extends Enum<E>> Set<E> parse(String csv, Class<E> type) {
        if (csv == null || csv.isBlank()) return EnumSet.noneOf(type);
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .map(s -> Enum.valueOf(type, s)).collect(Collectors.toCollection(() -> EnumSet.noneOf(type)));
    }

    private static String join(Set<? extends Enum<?>> values) {
        return values.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }
}
