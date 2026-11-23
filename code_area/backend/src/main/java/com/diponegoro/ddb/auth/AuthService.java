package com.diponegoro.ddb.auth;

import com.diponegoro.ddb.auth.AuthDtos.AuthTokens;
import com.diponegoro.ddb.auth.AuthDtos.EkycDto;
import com.diponegoro.ddb.auth.AuthDtos.OtpInfo;
import com.diponegoro.ddb.auth.AuthDtos.RecoveryStarted;
import com.diponegoro.ddb.auth.AuthDtos.RecoveryVerified;
import com.diponegoro.ddb.auth.AuthDtos.RegisterRequest;
import com.diponegoro.ddb.auth.AuthDtos.RegisterResponse;
import com.diponegoro.ddb.auth.AuthDtos.SessionDto;
import com.diponegoro.ddb.auth.AuthDtos.UserDto;
import com.diponegoro.ddb.auth.AuthEntities.AccountStatus;
import com.diponegoro.ddb.auth.AuthEntities.AuthMethod;
import com.diponegoro.ddb.auth.AuthEntities.EkycStatus;
import com.diponegoro.ddb.auth.AuthEntities.OtpPurpose;
import com.diponegoro.ddb.auth.AuthEntities.RecoveryType;
import com.diponegoro.ddb.auth.AuthEntities.SessionStatus;
import com.diponegoro.ddb.auth.AuthRepositories.EkycRepository;
import com.diponegoro.ddb.auth.AuthRepositories.SessionRepository;
import com.diponegoro.ddb.auth.AuthRepositories.UserRepository;
import com.diponegoro.ddb.common.ApiException;
import com.diponegoro.ddb.common.AuditService;
import com.diponegoro.ddb.common.DdbProperties;
import com.diponegoro.ddb.common.Hashing;
import com.diponegoro.ddb.common.Ids;
import com.diponegoro.ddb.ledger.LedgerService;
import com.diponegoro.ddb.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

/** Modul Authentication: FR-01 – FR-05. */
@Service
public class AuthService {
    /** Saldo awal rekening nasabah baru: Rp50.000, ketentuan PT Diponegoro Digital Bank. */
    public static final long NEW_USER_OPENING_BALANCE = 50_000L;
    /** Lama simulasi proses e-KYC sebelum diputuskan. */
    public static final Duration EKYC_PROCESSING = Duration.ofSeconds(3);
    static final String GENERIC_LOGIN_ERROR = "Username/email atau password salah";
    static final int MAX_LOGIN_FAILURES = 5;
    static final Duration LOGIN_LOCK_WINDOW = Duration.ofMinutes(15);

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{9,15}$");

    private final UserRepository users;
    private final SessionRepository sessions;
    private final EkycRepository ekycs;
    private final BiometricDevice.Repo biometrics;
    private final OtpService otps;
    private final PinService pins;
    private final LedgerService ledger;
    private final JwtService jwt;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final LoginThrottle throttle;
    private final DdbProperties props;
    private final Clock clock;
    private final String dummyHash;
    private final long openingBalance;

    public AuthService(UserRepository users, SessionRepository sessions, EkycRepository ekycs,
                       BiometricDevice.Repo biometrics, OtpService otps, PinService pins, LedgerService ledger,
                       JwtService jwt, PasswordEncoder encoder, AuditService audit, LoginThrottle throttle,
                       DdbProperties props, Clock clock,
                       @Value("${ddb.opening-balance:#{T(com.diponegoro.ddb.auth.AuthService).NEW_USER_OPENING_BALANCE}}")
                       long openingBalance) {
        this.openingBalance = openingBalance;
        this.users = users;
        this.sessions = sessions;
        this.ekycs = ekycs;
        this.biometrics = biometrics;
        this.otps = otps;
        this.pins = pins;
        this.ledger = ledger;
        this.jwt = jwt;
        this.encoder = encoder;
        this.audit = audit;
        this.throttle = throttle;
        this.props = props;
        this.clock = clock;
        this.dummyHash = encoder.encode("dummy-password-for-timing");
    }

    // ================================================================== registrasi (FR-01)

    public static void requireStrongPassword(String password) {
        if (password == null || password.length() < 8 || password.chars().noneMatch(Character::isLetter)
                || password.chars().noneMatch(Character::isDigit)) {
            throw ApiException.badRequest("WEAK_PASSWORD", "Password minimal 8 karakter dan mengandung huruf serta angka");
        }
    }

    /** Validasi & normalisasi kontak; dipakai juga oleh perubahan profil. */
    public static String[] normalizeContact(String username, String email, String phone) {
        String u = username == null ? "" : username.trim().toLowerCase();
        String e = email == null ? "" : email.trim().toLowerCase();
        String p = phone == null ? "" : phone.trim();
        if (u.length() < 4 || u.length() > 30 || !u.matches("[a-z0-9._]+")) {
            throw ApiException.badRequest("INVALID_USERNAME", "Username 4–30 karakter: huruf, angka, titik, atau garis bawah");
        }
        if (e.isEmpty() && p.isEmpty()) throw ApiException.badRequest("CONTACT_REQUIRED", "Isi email atau nomor telepon");
        if (!e.isEmpty() && !EMAIL.matcher(e).matches()) throw ApiException.badRequest("INVALID_EMAIL", "Format email tidak valid");
        if (!p.isEmpty() && !PHONE.matcher(p).matches()) throw ApiException.badRequest("INVALID_PHONE", "Nomor telepon harus 9-15 digit");
        return new String[]{u, e.isEmpty() ? null : e, p.isEmpty() ? null : p};
    }

    @Transactional
    public RegisterResponse register(RegisterRequest r) {
        String[] c = normalizeContact(r.username(), r.email(), r.phoneNumber());
        requireStrongPassword(r.password());
        if (users.existsByUsername(c[0]) || (c[1] != null && users.existsByEmail(c[1]))
                || (c[2] != null && users.existsByPhoneNumber(c[2]))) {
            throw ApiException.conflict("ALREADY_REGISTERED", "Username, email, atau nomor telepon sudah terdaftar");
        }
        Instant now = clock.instant();
        User u = new User();
        u.id = Ids.newId("user");
        u.username = c[0];
        u.email = c[1];
        u.phoneNumber = c[2];
        u.passwordHash = encoder.encode(r.password());
        u.status = AccountStatus.PENDING_OTP;
        u.biometricEnabled = false;
        u.createdAt = now;
        u.updatedAt = now;
        users.save(u);
        return new RegisterResponse(u.id, sendOtp(u, OtpPurpose.REGISTRATION, null));
    }

    @Transactional
    public OtpInfo resendRegistrationOtp(String userId) {
        User u = user(userId);
        if (u.status != AccountStatus.PENDING_OTP) throw ApiException.conflict("ALREADY_VERIFIED", "Akun sudah diverifikasi");
        return sendOtp(u, OtpPurpose.REGISTRATION, null);
    }

    /** OTP registrasi valid → akun aktif dan langsung login (agar bisa lanjut e-KYC). */
    @Transactional
    public AuthTokens verifyRegistration(String userId, String code, String deviceId, String deviceName) {
        User u = user(userId);
        if (u.status != AccountStatus.PENDING_OTP) throw ApiException.conflict("ALREADY_VERIFIED", "Akun sudah diverifikasi");
        otps.verify(userId, OtpPurpose.REGISTRATION, code);
        u.status = AccountStatus.ACTIVE;
        u.updatedAt = clock.instant();
        audit.record(userId, "REGISTERED", null);
        return newSession(u, deviceId, deviceName, AuthMethod.PASSWORD);
    }

    OtpInfo sendOtp(User u, OtpPurpose purpose, String payload) {
        String target = u.email != null ? u.email : u.phoneNumber;
        OtpService.IssuedOtp otp = otps.issue(u.id, purpose, target, payload);
        return new OtpInfo(mask(target), otp.expiresAt(), props.exposeOtp() ? otp.code() : null);
    }

    // ================================================================== e-KYC (FR-02)

    @Transactional
    public EkycDto submitEkyc(String userId, String nik, String fullName, String selfieReference) {
        User u = user(userId);
        if (u.status == AccountStatus.PENDING_OTP) throw ApiException.forbidden("OTP_REQUIRED", "Akun belum diverifikasi OTP");
        if (u.status == AccountStatus.FINANCIAL_ENABLED) throw ApiException.conflict("EKYC_DONE", "e-KYC sudah disetujui");
        if (nik == null || !nik.matches("\\d{16}")) throw ApiException.badRequest("INVALID_NIK", "NIK harus 16 digit angka");
        EkycVerification e = ekycs.findByUserId(userId).orElseGet(EkycVerification::new);
        if (e.id == null) e.id = Ids.newId("ekyc");
        e.userId = userId;
        e.nikMasked = "************" + nik.substring(12);
        e.nikHash = encoder.encode(nik);
        e.fullName = fullName;
        e.selfieReference = selfieReference;
        e.status = EkycStatus.PROCESSING;
        e.reason = null;
        e.submittedAt = clock.instant();
        e.decidedAt = null;
        ekycs.save(e);
        return toDto(e);
    }

    /**
     * Status e-KYC. Simulasi verifikator: setelah {@link #EKYC_PROCESSING}, NIK berakhiran 0000 ditolak,
     * selainnya disetujui. Saat disetujui, rekening utama dibuka dengan saldo awal simulasi.
     */
    @Transactional
    public EkycDto ekycStatus(String userId) {
        EkycVerification e = ekycs.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("Data e-KYC belum dikirim"));
        Instant now = clock.instant();
        if (e.status == EkycStatus.PROCESSING && !e.submittedAt.plus(EKYC_PROCESSING).isAfter(now)) {
            boolean rejected = e.nikMasked.endsWith("0000");
            e.status = rejected ? EkycStatus.REJECTED : EkycStatus.APPROVED;
            e.reason = rejected ? "Data KTP tidak cocok dengan data kependudukan (simulasi)" : null;
            e.decidedAt = now;
            User u = user(userId);
            u.status = rejected ? AccountStatus.KYC_REJECTED : AccountStatus.FINANCIAL_ENABLED;
            u.updatedAt = now;
            if (!rejected) {
                ledger.openMainAccount(userId, displayName(u, e), openingBalance, null, now);
            }
            audit.record(userId, rejected ? "EKYC_REJECTED" : "EKYC_APPROVED", null);
        }
        return toDto(e);
    }

    private static String displayName(User u, EkycVerification e) {
        return e.fullName != null && !e.fullName.isBlank() ? e.fullName.trim() : u.username;
    }

    // ================================================================== login & sesi (FR-03, FR-05)

    @Transactional(noRollbackFor = ApiException.class)
    public AuthTokens login(String identifier, String password, String deviceId, String deviceName) {
        User u = users.findByIdentifier(identifier).orElse(null);
        if (u == null) {
            encoder.matches(password, dummyHash); // samakan waktu respons agar keberadaan akun tidak bocor
            throw ApiException.unauthorized("INVALID_CREDENTIALS", GENERIC_LOGIN_ERROR);
        }
        throttle.check(u.id);
        if (!encoder.matches(password, u.passwordHash)) {
            audit.record(u.id, "LOGIN_FAILED", deviceId);
            throw ApiException.unauthorized("INVALID_CREDENTIALS", GENERIC_LOGIN_ERROR);
        }
        if (u.status == AccountStatus.PENDING_OTP) {
            throw ApiException.forbidden("OTP_REQUIRED", "Akun belum diverifikasi OTP");
        }
        audit.record(u.id, "LOGIN_SUCCESS", deviceId);
        return newSession(u, deviceId, deviceName, AuthMethod.PASSWORD);
    }

    /** Daftarkan perangkat untuk login biometrik; token disimpan klien di keystore yang dilindungi biometrik. */
    @Transactional
    public String enrollBiometric(String userId, String deviceId) {
        User u = user(userId);
        BiometricDevice d = biometrics.findByUserIdAndDeviceId(userId, deviceId).orElseGet(BiometricDevice::new);
        String token = Ids.randomToken();
        if (d.id == null) d.id = Ids.newId("bio");
        d.userId = userId;
        d.deviceId = deviceId;
        d.tokenHash = Hashing.sha256(token);
        d.revoked = false;
        d.createdAt = clock.instant();
        biometrics.save(d);
        u.biometricEnabled = true;
        u.updatedAt = clock.instant();
        audit.record(userId, "BIOMETRIC_ENABLED", deviceId);
        return token;
    }

    @Transactional
    public void disableBiometric(String userId) {
        User u = user(userId);
        biometrics.findByUserIdAndRevokedFalse(userId).forEach(d -> d.revoked = true);
        u.biometricEnabled = false;
        u.updatedAt = clock.instant();
        audit.record(userId, "BIOMETRIC_DISABLED", null);
    }

    @Transactional
    public AuthTokens biometricLogin(String biometricToken, String deviceId, String deviceName) {
        BiometricDevice d = biometrics.findByTokenHash(Hashing.sha256(biometricToken)).orElse(null);
        if (d == null || d.revoked || !d.deviceId.equals(deviceId)) {
            throw ApiException.unauthorized("BIOMETRIC_INVALID", "Biometrik tidak aktif di perangkat ini. Login dengan password.");
        }
        User u = user(d.userId);
        if (!u.biometricEnabled) throw ApiException.unauthorized("BIOMETRIC_INVALID", "Biometrik tidak aktif");
        audit.record(u.id, "LOGIN_BIOMETRIC", deviceId);
        return newSession(u, deviceId, deviceName, AuthMethod.BIOMETRIC);
    }

    /** Rotasi refresh token: token lama tidak berlaku lagi setelah dipakai. */
    @Transactional
    public AuthTokens refresh(String refreshToken) {
        UserSession s = sessions.findByRefreshTokenHash(Hashing.sha256(refreshToken))
                .orElseThrow(() -> ApiException.unauthorized("SESSION_INVALID", "Sesi berakhir. Silakan login kembali."));
        Instant now = clock.instant();
        if (s.status != SessionStatus.ACTIVE || !s.expiresAt.isAfter(now)) {
            throw ApiException.unauthorized("SESSION_INVALID", "Sesi berakhir. Silakan login kembali.");
        }
        String newRefresh = Ids.randomToken();
        s.refreshTokenHash = Hashing.sha256(newRefresh);
        s.lastSeenAt = now;
        return tokens(user(s.userId), s, newRefresh);
    }

    @Transactional
    public void logout(String sessionId) {
        sessions.findById(sessionId).ifPresent(s -> s.status = SessionStatus.TERMINATED);
    }

    /** FR-05: logout dari semua perangkat membatalkan seluruh token aktif. */
    @Transactional
    public int logoutAll(String userId) {
        List<UserSession> active = sessions.findByUserIdAndStatusOrderByCreatedAtDesc(userId, SessionStatus.ACTIVE);
        active.forEach(s -> s.status = SessionStatus.TERMINATED);
        audit.record(userId, "LOGOUT_ALL", active.size() + " sesi");
        return active.size();
    }

    public List<SessionDto> sessions(String userId, String currentSessionId) {
        Instant now = clock.instant();
        return sessions.findByUserIdAndStatusOrderByCreatedAtDesc(userId, SessionStatus.ACTIVE).stream()
                .filter(s -> s.expiresAt.isAfter(now))
                .map(s -> new SessionDto(s.id, s.deviceId, s.deviceName, s.authMethod, s.createdAt, s.lastSeenAt,
                        s.expiresAt, s.id.equals(currentSessionId)))
                .toList();
    }

    private AuthTokens newSession(User u, String deviceId, String deviceName, AuthMethod method) {
        Instant now = clock.instant();
        String refresh = Ids.randomToken();
        UserSession s = new UserSession();
        s.id = Ids.newId("sess");
        s.userId = u.id;
        s.deviceId = deviceId;
        s.deviceName = deviceName;
        s.authMethod = method;
        s.refreshTokenHash = Hashing.sha256(refresh);
        s.status = SessionStatus.ACTIVE;
        s.createdAt = now;
        s.lastSeenAt = now;
        s.expiresAt = now.plus(Duration.ofDays(props.security().refreshTokenDays()));
        sessions.save(s);
        return tokens(u, s, refresh);
    }

    private AuthTokens tokens(User u, UserSession s, String refresh) {
        JwtService.AccessToken access = jwt.issue(u.id, s.id);
        return new AuthTokens(access.token(), access.expiresAt(), refresh, s.expiresAt, s.id, toDto(u));
    }

    // ================================================================== PIN (FR-04)

    @Transactional
    public void createPin(String userId, String pin) {
        User u = user(userId);
        if (u.status == AccountStatus.PENDING_OTP) throw ApiException.forbidden("OTP_REQUIRED", "Akun belum diverifikasi OTP");
        pins.create(userId, pin);
        audit.record(userId, "PIN_CREATED", null);
    }

    /** Verifikasi PIN mandiri (gerbang layar PIN aplikasi). Hitungan salah & blokir sama dengan transaksi. */
    public void verifyPin(String userId, String pin) {
        pins.verify(userId, pin);
    }

    // ================================================================== pemulihan (FR-05)

    /** Tidak membocorkan apakah akun ada: respons sama untuk identifier yang tidak dikenal. */
    @Transactional
    public RecoveryStarted startRecovery(String identifier, RecoveryType type) {
        User u = users.findByIdentifier(identifier).orElse(null);
        String msg = "Jika akun terdaftar, kode OTP telah dikirim.";
        if (u == null || u.status == AccountStatus.PENDING_OTP) return new RecoveryStarted(msg, null);
        return new RecoveryStarted(msg, sendOtp(u, type.purpose, null));
    }

    @Transactional
    public RecoveryVerified verifyRecovery(String identifier, RecoveryType type, String code) {
        User u = users.findByIdentifier(identifier)
                .orElseThrow(() -> ApiException.unprocessable("OTP_INVALID", "Kode OTP salah."));
        OtpCode o = otps.verify(u.id, type.purpose, code);
        return new RecoveryVerified(o.id);
    }

    /** Reset password/PIN dengan token hasil verifikasi OTP. Reset password mengakhiri semua sesi. */
    @Transactional
    public void resetCredential(String identifier, RecoveryType type, String recoveryToken, String newSecret) {
        User u = users.findByIdentifier(identifier)
                .orElseThrow(() -> ApiException.unprocessable("OTP_REQUIRED", "Verifikasi OTP diperlukan"));
        if (type == RecoveryType.PASSWORD) requireStrongPassword(newSecret); else PinService.requireFormat(newSecret);
        otps.consumeVerified(u.id, type.purpose, recoveryToken);
        if (type == RecoveryType.PASSWORD) {
            u.passwordHash = encoder.encode(newSecret);
            u.updatedAt = clock.instant();
            logoutAll(u.id);
        } else {
            pins.reset(u.id, newSecret);
        }
        audit.record(u.id, type == RecoveryType.PASSWORD ? "PASSWORD_RESET" : "PIN_RESET", null);
    }

    // ================================================================== helper

    public User user(String userId) {
        return users.findById(userId).orElseThrow(() -> ApiException.notFound("Pengguna tidak ditemukan"));
    }

    public UserDto toDto(User u) {
        EkycStatus ekyc = ekycs.findByUserId(u.id).map(e -> e.status).orElse(null);
        return new UserDto(u.id, u.username, u.email, u.phoneNumber, u.status, ekyc, pins.hasPin(u.id),
                u.biometricEnabled, u.photoUrl);
    }

    static EkycDto toDto(EkycVerification e) {
        return new EkycDto(e.id, e.status, e.nikMasked, e.reason, e.submittedAt, e.decidedAt);
    }

    /** "demo@diponegoro.bank" → "de**@diponegoro.bank", "081234567890" → "0812****7890". */
    public static String mask(String target) {
        if (target == null) return null;
        int at = target.indexOf('@');
        if (at > 0) return target.substring(0, Math.min(2, at)) + "**" + target.substring(at);
        if (target.length() > 8) return target.substring(0, 4) + "****" + target.substring(target.length() - 4);
        return target;
    }
}
