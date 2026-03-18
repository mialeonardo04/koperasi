-- ============================================================
-- V3: Create pinjaman table
-- ============================================================

CREATE TABLE IF NOT EXISTS pinjaman (
                                        id                   BIGSERIAL PRIMARY KEY,
                                        user_id              BIGINT          NOT NULL,
                                        no_pinjaman          VARCHAR(50)     NOT NULL UNIQUE,
    jumlah_pinjaman      NUMERIC(15, 2)  NOT NULL,
    bunga_per_bulan      NUMERIC(5, 2)   NOT NULL DEFAULT 1.5,
    tenor_bulan          INTEGER         NOT NULL,
    angsuran_per_bulan   NUMERIC(15, 2)  NOT NULL,
    total_sudah_dibayar  NUMERIC(15, 2)  NOT NULL DEFAULT 0,
    sisa_pinjaman        NUMERIC(15, 2),
    tanggal_pengajuan    DATE,
    tanggal_disetujui    DATE,
    tanggal_jatuh_tempo  DATE,
    tujuan_pinjaman      TEXT,
    status               VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    keterangan_admin     TEXT,
    created_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_pinjaman_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT chk_pinjaman_status
    CHECK (status IN ('PENDING', 'DISETUJUI', 'DITOLAK', 'LUNAS', 'MACET')),
    CONSTRAINT chk_pinjaman_jumlah
    CHECK (jumlah_pinjaman > 0),
    CONSTRAINT chk_pinjaman_tenor
    CHECK (tenor_bulan BETWEEN 1 AND 60),
    CONSTRAINT chk_pinjaman_bunga
    CHECK (bunga_per_bulan >= 0)
    );

CREATE INDEX IF NOT EXISTS idx_pinjaman_user_id     ON pinjaman (user_id);
CREATE INDEX IF NOT EXISTS idx_pinjaman_status      ON pinjaman (status);
CREATE INDEX IF NOT EXISTS idx_pinjaman_no_pinjaman ON pinjaman (no_pinjaman);
CREATE INDEX IF NOT EXISTS idx_pinjaman_user_status ON pinjaman (user_id, status);
CREATE INDEX IF NOT EXISTS idx_pinjaman_created_at  ON pinjaman (created_at DESC);

COMMENT ON TABLE  pinjaman                 IS 'Data pengajuan dan pinjaman aktif anggota';
COMMENT ON COLUMN pinjaman.no_pinjaman     IS 'Nomor pinjaman unik, format PIN-{timestamp}';
COMMENT ON COLUMN pinjaman.bunga_per_bulan IS 'Bunga bulanan dalam persen, contoh: 1.5 = 1.5%';
COMMENT ON COLUMN pinjaman.status          IS 'PENDING | DISETUJUI | DITOLAK | LUNAS | MACET';
COMMENT ON COLUMN pinjaman.sisa_pinjaman   IS 'Sisa pokok yang belum dibayar';