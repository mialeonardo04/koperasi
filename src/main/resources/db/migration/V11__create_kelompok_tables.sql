-- ============================================================
-- V11: Sistem Pinjaman Kelompok
-- ============================================================

-- Tabel kelompok
CREATE TABLE kelompok (
                          id               BIGSERIAL PRIMARY KEY,
                          kode_kelompok    VARCHAR(20)  UNIQUE NOT NULL,
                          nama_kelompok    VARCHAR(100) NOT NULL,
                          deskripsi        TEXT,
                          leader_id        BIGINT       NOT NULL REFERENCES users(id),
                          status           VARCHAR(20)  NOT NULL DEFAULT 'AKTIF',
    -- AKTIF = bisa ajukan pinjaman
    -- TERMINATED = pinjaman lunas, kelompok dibubarkan
                          created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
                          updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Tabel anggota kelompok
CREATE TABLE kelompok_anggota (
                                  id           BIGSERIAL PRIMARY KEY,
                                  kelompok_id  BIGINT NOT NULL REFERENCES kelompok(id),
                                  user_id      BIGINT NOT NULL REFERENCES users(id),
                                  joined_at    TIMESTAMP NOT NULL DEFAULT NOW(),
                                  UNIQUE (kelompok_id, user_id)
);

-- Index: cari kelompok aktif per user
CREATE INDEX idx_kelompok_anggota_user ON kelompok_anggota(user_id);

-- Tabel pinjaman kelompok
CREATE TABLE pinjaman_kelompok (
                                   id                  BIGSERIAL PRIMARY KEY,
                                   no_pinjaman         VARCHAR(30) UNIQUE NOT NULL,
                                   kelompok_id         BIGINT      NOT NULL REFERENCES kelompok(id),
                                   pengaju_id          BIGINT      NOT NULL REFERENCES users(id),
                                   jumlah_pinjaman     DECIMAL(15,2) NOT NULL,
                                   bunga_per_bulan     DECIMAL(5,2)  NOT NULL DEFAULT 1.5,
                                   tenor_bulan         INT          NOT NULL,
                                   angsuran_per_bulan  DECIMAL(15,2),
                                   total_sudah_dibayar DECIMAL(15,2) NOT NULL DEFAULT 0,
                                   sisa_pinjaman       DECIMAL(15,2),
                                   tujuan_pinjaman     TEXT,
                                   status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    -- PENDING/DISETUJUI/DITOLAK/LUNAS/MACET
                                   tanggal_pengajuan   DATE         NOT NULL DEFAULT CURRENT_DATE,
                                   tanggal_disetujui   DATE,
                                   tanggal_jatuh_tempo DATE,
                                   keterangan_admin    TEXT,
                                   created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
                                   updated_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Tabel jadwal angsuran kelompok
CREATE TABLE angsuran_kelompok (
                                   id                  BIGSERIAL PRIMARY KEY,
                                   pinjaman_kelompok_id BIGINT       NOT NULL REFERENCES pinjaman_kelompok(id),
                                   periode_ke          INT          NOT NULL,
                                   jumlah_angsuran     DECIMAL(15,2) NOT NULL,
                                   pokok               DECIMAL(15,2) NOT NULL,
                                   bunga               DECIMAL(15,2) NOT NULL,
                                   tanggal_jatuh_tempo DATE         NOT NULL,
                                   tanggal_bayar       DATE,
                                   status              VARCHAR(20)  NOT NULL DEFAULT 'BELUM_BAYAR',
    -- BELUM_BAYAR/SUDAH_BAYAR/TERLAMBAT
                                   dibayar_oleh_user_id BIGINT REFERENCES users(id),
    -- siapa yang bayar (bisa anggota lain)
                                   metode_bayar        VARCHAR(20),
    -- SIMPANAN / TRANSFER
                                   no_referensi_bayar  VARCHAR(50),
                                   created_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Tabel teguran kelompok dari admin
CREATE TABLE teguran_kelompok (
                                  id           BIGSERIAL PRIMARY KEY,
                                  kelompok_id  BIGINT    NOT NULL REFERENCES kelompok(id),
                                  admin_id     BIGINT    NOT NULL REFERENCES users(id),
                                  pesan        TEXT      NOT NULL,
                                  sent_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Tabel transaksi pending untuk pinjaman kelompok
-- (reuse tabel transaksi_pending yang sudah ada dengan tambahan kolom)
ALTER TABLE transaksi_pending
    ADD COLUMN IF NOT EXISTS angsuran_kelompok_id BIGINT REFERENCES angsuran_kelompok(id),
    ADD COLUMN IF NOT EXISTS metode_bayar VARCHAR(20);
-- metode_bayar: SIMPANAN / TRANSFER

-- Sequence kode kelompok
CREATE SEQUENCE IF NOT EXISTS kelompok_seq START 1;