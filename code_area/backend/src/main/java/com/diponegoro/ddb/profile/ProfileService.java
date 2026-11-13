package com.diponegoro.ddb.profile;

import com.diponegoro.ddb.auth.AuthDtos.OtpInfo;
import com.diponegoro.ddb.auth.AuthDtos.UserDto;
import com.diponegoro.ddb.auth.AuthEntities.OtpPurpose;
import com.diponegoro.ddb.auth.AuthRepositories.UserRepository;
import com.diponegoro.ddb.auth.AuthService;
import com.diponegoro.ddb.auth.OtpCode;
import com.diponegoro.ddb.auth.OtpService;
import com.diponegoro.ddb.auth.PinService;
import com.diponegoro.ddb.auth.User;
import com.diponegoro.ddb.common.ApiException;
import com.diponegoro.ddb.common.AuditService;
import com.diponegoro.ddb.common.DdbProperties;
import com.diponegoro.ddb.notification.NotificationEntities.Type;
import com.diponegoro.ddb.notification.NotificationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.Objects;

/** Modul Profile: FR-27 (profil + OTP) dan FR-28 (keamanan). */
@Service
public class ProfileService {

    public record ProfileDraft(String username, String email, String phoneNumber) {}

    private final UserRepository users;
    private final AuthService auth;
    private final OtpService otps;
    private final PinService pins;
    private final PasswordEncoder encoder;
    private final NotificationService notifications;
    private final AuditService audit;
    private final DdbProperties props;
    private final ObjectMapper mapper;
    private final Clock clock;

    public ProfileService(UserRepository users, AuthService auth, OtpService otps, PinService pins, PasswordEncoder encoder,
                          NotificationService notifications, AuditService audit, DdbProperties props, ObjectMapper mapper,
                          Clock clock) {
        this.users = users;
        this.auth = auth;
        this.otps = otps;
        this.pins = pins;
        this.encoder = encoder;
        this.notifications = notifications;
        this.audit = audit;
        this.props = props;
        this.mapper = mapper;
        this.clock = clock;
    }

    public UserDto me(String userId) {
        return auth.toDto(auth.user(userId));
    }

    /** Langkah 1: validasi & kirim OTP. OTP dikirim ke kontak baru agar kepemilikannya terverifikasi. */
    @Transactional
    public OtpInfo requestChange(String userId, ProfileDraft d) {
        User u = auth.user(userId);
        String[] c = AuthService.normalizeContact(d.username(), d.email(), d.phoneNumber());
        if (c[0].equals(u.username) && Objects.equals(c[1], u.email) && Objects.equals(c[2], u.phoneNumber)) {
            throw ApiException.badRequest("NO_CHANGES", "Tidak ada perubahan data");
        }
        if ((!c[0].equals(u.username) && users.existsByUsername(c[0]))
                || (c[1] != null && !c[1].equals(u.email) && users.existsByEmail(c[1]))
                || (c[2] != null && !c[2].equals(u.phoneNumber) && users.existsByPhoneNumber(c[2]))) {
            throw ApiException.conflict("ALREADY_USED", "Username, email, atau nomor telepon sudah dipakai akun lain");
        }
        String sentTo = c[2] != null && !c[2].equals(u.phoneNumber) ? c[2]
                : c[1] != null && !c[1].equals(u.email) ? c[1]
                : u.phoneNumber != null ? u.phoneNumber : u.email;
        String payload = mapper.writeValueAsString(new ProfileDraft(c[0], c[1], c[2]));
        OtpService.IssuedOtp otp = otps.issue(userId, OtpPurpose.PROFILE_CHANGE, sentTo, payload);
        return new OtpInfo(AuthService.mask(sentTo), otp.expiresAt(), props.exposeOtp() ? otp.code() : null);
    }

    /** Langkah 2: email/nomor telepon baru aktif hanya setelah OTP valid. */
    @Transactional
    public UserDto confirmChange(String userId, String code) {
        OtpCode verified = otps.verify(userId, OtpPurpose.PROFILE_CHANGE, code);
        otps.consumeVerified(userId, OtpPurpose.PROFILE_CHANGE, verified.id);
        ProfileDraft d = mapper.readValue(verified.payload, ProfileDraft.class);
        User u = auth.user(userId);
        u.username = d.username();
        u.email = d.email();
        u.phoneNumber = d.phoneNumber();
        u.updatedAt = clock.instant();
        users.saveAndFlush(u);   // constraint UNIQUE tetap menjadi pengaman terakhir
        audit.record(userId, "PROFILE_UPDATED", null);
        notifications.notify(userId, Type.SECURITY, "Profil diperbarui",
                "Data profil Anda berhasil diubah setelah verifikasi OTP.", null);
        return auth.toDto(u);
    }

    @Transactional
    public UserDto updatePhoto(String userId, String photoUrl) {
        if (photoUrl != null && (photoUrl.length() > 500 || !photoUrl.matches("^https?://.+"))) {
            throw ApiException.badRequest("INVALID_PHOTO", "URL foto tidak valid");
        }
        User u = auth.user(userId);
        u.photoUrl = photoUrl;
        u.updatedAt = clock.instant();
        return auth.toDto(u);
    }

    // ------------------------------------------------------------------ FR-28 keamanan

    @Transactional
    public void changePassword(String userId, String oldPassword, String newPassword) {
        User u = auth.user(userId);
        if (!encoder.matches(oldPassword == null ? "" : oldPassword, u.passwordHash)) {
            throw ApiException.unprocessable("WRONG_PASSWORD", "Password lama salah");
        }
        AuthService.requireStrongPassword(newPassword);
        if (oldPassword.equals(newPassword)) {
            throw ApiException.badRequest("SAME_PASSWORD", "Password baru harus berbeda dari password lama");
        }
        u.passwordHash = encoder.encode(newPassword);
        u.updatedAt = clock.instant();
        audit.record(userId, "PASSWORD_CHANGED", null);
        notifications.notify(userId, Type.SECURITY, "Password diubah",
                "Password akun Anda baru saja diubah. Jika bukan Anda, segera lakukan pemulihan akun.", null);
    }

    @Transactional
    public void changePin(String userId, String oldPin, String newPin) {
        PinService.requireFormat(newPin);
        if (Objects.equals(oldPin, newPin)) throw ApiException.badRequest("SAME_PIN", "PIN baru harus berbeda dari PIN lama");
        pins.verify(userId, oldPin);
        pins.reset(userId, newPin);
        audit.record(userId, "PIN_CHANGED", null);
        notifications.notify(userId, Type.SECURITY, "PIN transaksi diubah", "PIN transaksi Anda baru saja diubah.", null);
    }
}
