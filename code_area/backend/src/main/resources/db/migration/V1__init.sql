-- =====================================================================================
-- Diponegoro Digital Bank — skema awal (SRS-DDB-001 v0.1)
-- Dijalankan oleh Flyway. Kompatibel dengan PostgreSQL (produksi) dan H2 mode PostgreSQL (test).
--
-- Prinsip (BR-03, BR-04, BR-05, NFR-07):
--   * Uang disimpan sebagai BIGINT dalam satuan rupiah (tanpa pecahan, tanpa pembulatan).
--   * Ledger double-entry: setiap transaksi = beberapa baris journal_lines yang jumlahnya 0.
--   * journal_lines append-only; pengembalian dana ditulis sebagai baris pembalik.
--   * ledger_accounts.balance = cache saldo yang diperbarui di transaksi DB yang sama dengan
--     penulisan jurnal (dengan kunci baris), sehingga selalu sama dengan SUM(journal_lines).
--   * idempotency_key UNIQUE mencegah debit ganda.
-- =====================================================================================

-- ------------------------------------------------------------------ Authentication (FR-01–FR-05)
CREATE TABLE users (
    id                VARCHAR(64)  PRIMARY KEY,
    username          VARCHAR(30)  NOT NULL,
    email             VARCHAR(120),
    phone_number      VARCHAR(20),
    password_hash     VARCHAR(100) NOT NULL,           -- BCrypt (NFR-06)
    status            VARCHAR(20)  NOT NULL,           -- PENDING_OTP | ACTIVE | FINANCIAL_ENABLED | KYC_REJECTED
    biometric_enabled BOOLEAN      NOT NULL DEFAULT FALSE,
    balance_hidden    BOOLEAN      NOT NULL DEFAULT FALSE, -- FR-06: pilihan tersimpan
    photo_url         VARCHAR(500),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL
);
-- username & email selalu disimpan huruf kecil oleh aplikasi, sehingga UNIQUE biasa sudah cukup.
CREATE UNIQUE INDEX ux_users_username ON users (username);
CREATE UNIQUE INDEX ux_users_email    ON users (email);
CREATE UNIQUE INDEX ux_users_phone    ON users (phone_number);

CREATE TABLE otp_codes (
    id          VARCHAR(64) PRIMARY KEY,
    user_id     VARCHAR(64) NOT NULL REFERENCES users (id),
    purpose     VARCHAR(30) NOT NULL,                  -- REGISTRATION | PASSWORD_RECOVERY | PIN_RECOVERY | PROFILE_CHANGE
    code_hash   VARCHAR(100) NOT NULL,
    target      VARCHAR(120),                          -- tujuan pengiriman (email/no. HP)
    payload     TEXT,                                  -- mis. data profil baru yang menunggu OTP (JSON)
    attempts    INT         NOT NULL DEFAULT 0,
    status      VARCHAR(20) NOT NULL,                  -- PENDING | VERIFIED | USED | EXPIRED
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_otp_user_purpose ON otp_codes (user_id, purpose, status);

CREATE TABLE ekyc_verifications (
    id               VARCHAR(64) PRIMARY KEY,
    user_id          VARCHAR(64) NOT NULL UNIQUE REFERENCES users (id),
    nik_masked       VARCHAR(20) NOT NULL,              -- hanya 4 digit terakhir yang terlihat (UU PDP)
    nik_hash         VARCHAR(100) NOT NULL,
    full_name        VARCHAR(100),
    selfie_reference VARCHAR(500) NOT NULL,
    status           VARCHAR(20) NOT NULL,              -- PROCESSING | APPROVED | REJECTED
    reason           VARCHAR(200),
    submitted_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    decided_at       TIMESTAMP WITH TIME ZONE
);

CREATE TABLE transaction_pins (
    user_id         VARCHAR(64) PRIMARY KEY REFERENCES users (id),
    pin_hash        VARCHAR(100) NOT NULL,              -- BCrypt, terpisah dari password (FR-04)
    failed_attempts INT         NOT NULL DEFAULT 0,
    blocked_until   TIMESTAMP WITH TIME ZONE,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE sessions (
    id                 VARCHAR(64) PRIMARY KEY,
    user_id            VARCHAR(64) NOT NULL REFERENCES users (id),
    device_id          VARCHAR(100) NOT NULL,
    device_name        VARCHAR(100),
    auth_method        VARCHAR(20) NOT NULL,            -- PASSWORD | BIOMETRIC
    refresh_token_hash VARCHAR(100) NOT NULL,
    status             VARCHAR(20) NOT NULL,            -- ACTIVE | TERMINATED | EXPIRED
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    last_seen_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at         TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_sessions_user ON sessions (user_id, status);

-- Login biometrik (FR-03): perangkat yang sudah diaktifkan menyimpan token perangkat di keystore,
-- yang hanya dibuka setelah sidik jari/wajah lolos di HP. Server menyimpan hash-nya saja.
CREATE TABLE biometric_devices (
    id         VARCHAR(64)  PRIMARY KEY,
    user_id    VARCHAR(64)  NOT NULL REFERENCES users (id),
    device_id  VARCHAR(100) NOT NULL,
    token_hash VARCHAR(100) NOT NULL UNIQUE,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ux_biometric_device UNIQUE (user_id, device_id)
);

-- ------------------------------------------------------------------ Ledger core (BR-03–05, NFR-07)
CREATE TABLE ledger_accounts (
    id            VARCHAR(64)  PRIMARY KEY,
    owner_user_id VARCHAR(64)  REFERENCES users (id),   -- NULL untuk rekening eksternal
    kind          VARCHAR(20)  NOT NULL,                -- MAIN | POCKET | EXTERNAL
    number        VARCHAR(30)  NOT NULL UNIQUE,
    name          VARCHAR(100) NOT NULL,
    balance       BIGINT       NOT NULL DEFAULT 0,
    closed        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_balance_non_negative CHECK (kind = 'EXTERNAL' OR balance >= 0)
);
CREATE INDEX ix_accounts_owner ON ledger_accounts (owner_user_id, kind);

CREATE TABLE ledger_transactions (
    id              VARCHAR(64)  PRIMARY KEY,
    reference       VARCHAR(30)  NOT NULL UNIQUE,       -- ID transaksi yang dilihat pengguna (DDByymmddXXXXXXXX)
    idempotency_key VARCHAR(120) NOT NULL UNIQUE,       -- BR-04
    category        VARCHAR(20)  NOT NULL,              -- DEPOSIT | TRANSFER | QRIS | TOPUP | POCKET | CARD
    status          VARCHAR(20)  NOT NULL,              -- SUCCESS | PROCESSING | FAILED
    amount          BIGINT       NOT NULL CHECK (amount > 0),
    fee             BIGINT       NOT NULL DEFAULT 0 CHECK (fee >= 0),
    title           VARCHAR(120) NOT NULL,
    debit_user_id   VARCHAR(64)  REFERENCES users (id),
    credit_user_id  VARCHAR(64)  REFERENCES users (id),
    from_name       VARCHAR(100) NOT NULL,
    from_detail     VARCHAR(120) NOT NULL,
    to_name         VARCHAR(100) NOT NULL,
    to_detail       VARCHAR(120) NOT NULL,
    note            VARCHAR(100) NOT NULL DEFAULT '',
    status_reason   VARCHAR(200),
    extra           TEXT,                               -- detail tambahan (JSON), mis. token PLN
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_tx_debit_user  ON ledger_transactions (debit_user_id, created_at);
CREATE INDEX ix_tx_credit_user ON ledger_transactions (credit_user_id, created_at);

CREATE TABLE journal_lines (
    id         BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    tx_id      VARCHAR(64)  NOT NULL REFERENCES ledger_transactions (id),
    account_id VARCHAR(64)  NOT NULL REFERENCES ledger_accounts (id),
    amount     BIGINT       NOT NULL CHECK (amount <> 0),
    memo       VARCHAR(120) NOT NULL,
    posted_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_journal_account ON journal_lines (account_id, posted_at);
CREATE INDEX ix_journal_tx      ON journal_lines (tx_id);

-- ------------------------------------------------------------------ Account: kantong (FR-08, FR-09)
CREATE TABLE pockets (
    account_id VARCHAR(64) PRIMARY KEY REFERENCES ledger_accounts (id),
    user_id    VARCHAR(64) NOT NULL REFERENCES users (id),
    purpose    VARCHAR(60) NOT NULL DEFAULT '',
    target     BIGINT CHECK (target IS NULL OR target > 0),
    lock_type  VARCHAR(10) NOT NULL DEFAULT 'NONE',     -- NONE | DATE | TARGET
    lock_until DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- ------------------------------------------------------------------ Transfer (FR-13)
CREATE TABLE favorite_recipients (
    id             VARCHAR(64) PRIMARY KEY,
    user_id        VARCHAR(64) NOT NULL REFERENCES users (id),
    bank_code      VARCHAR(10) NOT NULL,
    account_number VARCHAR(30) NOT NULL,
    name           VARCHAR(100) NOT NULL,
    added_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ux_favorite UNIQUE (user_id, bank_code, account_number)
);

-- ------------------------------------------------------------------ Top-up (FR-20: cegah tagihan dibayar dua kali)
CREATE TABLE paid_bills (
    bill_key VARCHAR(120) PRIMARY KEY,                  -- biller:nomor:periode
    tx_id    VARCHAR(64)  NOT NULL REFERENCES ledger_transactions (id),
    paid_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

-- ------------------------------------------------------------------ Virtual Card (FR-21, FR-22)
CREATE TABLE virtual_cards (
    id                VARCHAR(64)  PRIMARY KEY,
    user_id           VARCHAR(64)  NOT NULL REFERENCES users (id),
    cardholder_name   VARCHAR(100) NOT NULL,
    card_number_enc   VARCHAR(200) NOT NULL,            -- AES-GCM (NFR-06, prinsip PCI DSS)
    last4             VARCHAR(4)   NOT NULL,
    cvv_enc           VARCHAR(200) NOT NULL,
    expiry_month      INT          NOT NULL,
    expiry_year       INT          NOT NULL,
    status            VARCHAR(10)  NOT NULL,            -- ACTIVE | FROZEN
    usage_scope       VARCHAR(15)  NOT NULL,            -- DOMESTIC | INTERNATIONAL
    transaction_limit BIGINT       NOT NULL CHECK (transaction_limit > 0),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_cards_user ON virtual_cards (user_id);

-- ------------------------------------------------------------------ Notification (FR-28, FR-29)
CREATE TABLE notification_settings (
    user_id  VARCHAR(64) PRIMARY KEY REFERENCES users (id),
    types    VARCHAR(100) NOT NULL,                     -- daftar dipisah koma: INCOMING,OUTGOING,SECURITY,INFO
    channels VARCHAR(100) NOT NULL                      -- PUSH,IN_APP,EMAIL
);

CREATE TABLE notifications (
    id         VARCHAR(64)  PRIMARY KEY,
    user_id    VARCHAR(64)  NOT NULL REFERENCES users (id),
    type       VARCHAR(20)  NOT NULL,
    title      VARCHAR(120) NOT NULL,
    message    VARCHAR(500) NOT NULL,
    tx_id      VARCHAR(64)  REFERENCES ledger_transactions (id),
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    pushed     BOOLEAN      NOT NULL DEFAULT FALSE,     -- sudah diambil perangkat untuk push
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_notifications_user ON notifications (user_id, created_at);

CREATE TABLE email_outbox (
    id         VARCHAR(64)  PRIMARY KEY,
    user_id    VARCHAR(64)  NOT NULL REFERENCES users (id),
    to_address VARCHAR(120) NOT NULL,
    subject    VARCHAR(200) NOT NULL,
    body       TEXT         NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- ------------------------------------------------------------------ Jejak audit (NFR-07)
CREATE TABLE audit_events (
    id         BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id    VARCHAR(64),
    action     VARCHAR(50)  NOT NULL,                   -- LOGIN_SUCCESS, LOGIN_FAILED, PIN_BLOCKED, ...
    detail     VARCHAR(500),
    ip_address VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_audit_user ON audit_events (user_id, created_at);

-- ------------------------------------------------------------------ Rekening eksternal (simulator)
INSERT INTO ledger_accounts (id, owner_user_id, kind, number, name, balance, closed, created_at) VALUES
 ('ext:interbank', NULL, 'EXTERNAL', 'ext:interbank', 'Jaringan antarbank (simulator)',     0, FALSE, CURRENT_TIMESTAMP),
 ('ext:merchant',  NULL, 'EXTERNAL', 'ext:merchant',  'Merchant QRIS (simulator)',          0, FALSE, CURRENT_TIMESTAMP),
 ('ext:biller',    NULL, 'EXTERNAL', 'ext:biller',    'Biller top-up & tagihan (simulator)', 0, FALSE, CURRENT_TIMESTAMP),
 ('ext:card',      NULL, 'EXTERNAL', 'ext:card',      'Merchant kartu virtual (simulator)',  0, FALSE, CURRENT_TIMESTAMP),
 ('ext:fee',       NULL, 'EXTERNAL', 'ext:fee',       'Pendapatan biaya',                   0, FALSE, CURRENT_TIMESTAMP),
 ('ext:funding',   NULL, 'EXTERNAL', 'ext:funding',   'Dana masuk eksternal',               0, FALSE, CURRENT_TIMESTAMP);
