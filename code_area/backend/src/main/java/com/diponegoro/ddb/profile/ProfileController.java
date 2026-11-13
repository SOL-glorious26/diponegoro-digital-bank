package com.diponegoro.ddb.profile;

import com.diponegoro.ddb.auth.AuthDtos.MessageResponse;
import com.diponegoro.ddb.auth.AuthDtos.OtpInfo;
import com.diponegoro.ddb.auth.AuthDtos.UserDto;
import com.diponegoro.ddb.notification.NotificationEntities.Channel;
import com.diponegoro.ddb.notification.NotificationEntities.Type;
import com.diponegoro.ddb.notification.NotificationService;
import com.diponegoro.ddb.notification.NotificationService.EmailDto;
import com.diponegoro.ddb.notification.NotificationService.NotificationDto;
import com.diponegoro.ddb.notification.NotificationService.NotificationPage;
import com.diponegoro.ddb.notification.NotificationService.SettingsDto;
import com.diponegoro.ddb.profile.ProfileService.ProfileDraft;
import com.diponegoro.ddb.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "8. Profile & Notification", description = "FR-27 – FR-29: profil + OTP, keamanan, pengaturan & kotak notifikasi")
public class ProfileController {
    private final ProfileService profile;
    private final NotificationService notifications;

    public ProfileController(ProfileService profile, NotificationService notifications) {
        this.profile = profile;
        this.notifications = notifications;
    }

    public record OtpCodeRequest(@NotBlank(message = "OTP wajib diisi") String code) {}

    public record PhotoRequest(String photoUrl) {}

    public record PasswordChange(@NotBlank String oldPassword, @NotBlank String newPassword) {}

    public record PinChange(@NotBlank String oldPin, @NotBlank String newPin) {}

    public record SettingsRequest(Set<Type> types, Set<Channel> channels) {}

    @GetMapping
    @Operation(summary = "Profil pengguna yang login (FR-27)")
    public UserDto me(@AuthenticationPrincipal AuthUser me) {
        return profile.me(me.userId());
    }

    @PostMapping("/profile-change")
    @Operation(summary = "Ajukan perubahan username/email/telepon → OTP dikirim (FR-27)")
    public OtpInfo requestChange(@AuthenticationPrincipal AuthUser me, @RequestBody ProfileDraft d) {
        return profile.requestChange(me.userId(), d);
    }

    @PostMapping("/profile-change/confirm")
    @Operation(summary = "Konfirmasi perubahan profil dengan OTP")
    public UserDto confirm(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody OtpCodeRequest r) {
        return profile.confirmChange(me.userId(), r.code());
    }

    @PutMapping("/photo")
    public UserDto photo(@AuthenticationPrincipal AuthUser me, @RequestBody PhotoRequest r) {
        return profile.updatePhoto(me.userId(), r.photoUrl());
    }

    @PostMapping("/password")
    @Operation(summary = "Ubah password (FR-28)")
    public MessageResponse password(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody PasswordChange r) {
        profile.changePassword(me.userId(), r.oldPassword(), r.newPassword());
        return new MessageResponse("Password berhasil diubah");
    }

    @PostMapping("/pin")
    @Operation(summary = "Ubah PIN transaksi (FR-28)")
    public MessageResponse pin(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody PinChange r) {
        profile.changePin(me.userId(), r.oldPin(), r.newPin());
        return new MessageResponse("PIN transaksi berhasil diubah");
    }

    @GetMapping("/notification-settings")
    public SettingsDto settings(@AuthenticationPrincipal AuthUser me) {
        return notifications.settings(me.userId());
    }

    @PutMapping("/notification-settings")
    @Operation(summary = "Pilih jenis & kanal notifikasi (FR-28)")
    public SettingsDto updateSettings(@AuthenticationPrincipal AuthUser me, @RequestBody SettingsRequest r) {
        return notifications.updateSettings(me.userId(), r.types(), r.channels());
    }

    @GetMapping("/notifications")
    @Operation(summary = "Kotak notifikasi (FR-29)")
    public NotificationPage notifications(@AuthenticationPrincipal AuthUser me, @RequestParam(defaultValue = "0") int page) {
        return notifications.list(me.userId(), page);
    }

    @GetMapping("/notifications/unread-count")
    public Map<String, Long> unread(@AuthenticationPrincipal AuthUser me) {
        return Map.of("unread", notifications.unreadCount(me.userId()));
    }

    @PostMapping("/notifications/{id}/read")
    public MessageResponse read(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        notifications.markRead(me.userId(), id);
        return new MessageResponse("OK");
    }

    @PostMapping("/notifications/read-all")
    public MessageResponse readAll(@AuthenticationPrincipal AuthUser me) {
        return new MessageResponse(notifications.markAllRead(me.userId()) + " notifikasi ditandai dibaca");
    }

    @GetMapping("/notifications/push")
    @Operation(summary = "Ambil notifikasi push yang belum ditampilkan perangkat (polling; sekali ambil)")
    public List<NotificationDto> push(@AuthenticationPrincipal AuthUser me) {
        return notifications.takePendingPush(me.userId());
    }

    @GetMapping("/emails")
    @Operation(summary = "[Simulator] Kotak keluar email notifikasi")
    public List<EmailDto> emails(@AuthenticationPrincipal AuthUser me) {
        return notifications.emails(me.userId());
    }
}
