-- ============================================================
-- V1: Create users table
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
                                     id                BIGSERIAL PRIMARY KEY,
                                     nomor_anggota     VARCHAR(20)     NOT NULL UNIQUE,
    nama_lengkap      VARCHAR(255)    NOT NULL,
    email             VARCHAR(255)    NOT NULL UNIQUE,
    password          VARCHAR(255)    NOT NULL,
    no_telepon        VARCHAR(15),
    alamat            TEXT,
    tanggal_gabung    DATE,
    role              VARCHAR(20)     NOT NULL DEFAULT 'MEMBER',
    status            VARCHAR(20)     NOT NULL DEFAULT 'AKTIF',
    total_simpanan    NUMERIC(15, 2)  NOT NULL DEFAULT 0,
    created_at        TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_users_role   CHECK (role   IN ('ADMIN', 'MEMBER')),
    CONSTRAINT chk_users_status CHECK (status IN ('AKTIF', 'NON_AKTIF', 'SUSPEND'))
    );

CREATE INDEX IF NOT EXISTS idx_users_email         ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_nomor_anggota ON users (nomor_anggota);
CREATE INDEX IF NOT EXISTS idx_users_role          ON users (role);
CREATE INDEX IF NOT EXISTS idx_users_status        ON users (status);

COMMENT ON TABLE  users                 IS 'Tabel master anggota dan admin koperasi';
COMMENT ON COLUMN users.nomor_anggota  IS 'Nomor unik anggota, format ADM-XXXX / MBR-XXXX';
COMMENT ON COLUMN users.role           IS 'ADMIN = pengurus, MEMBER = anggota biasa';
COMMENT ON COLUMN users.status         IS 'AKTIF | NON_AKTIF | SUSPEND';
COMMENT ON COLUMN users.total_simpanan IS 'Cache total simpanan, diupdate setiap transaksi';