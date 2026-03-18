-- ============================================================
-- V2: Create simpanan table
-- ============================================================

CREATE TABLE IF NOT EXISTS simpanan (
                                        id                  BIGSERIAL PRIMARY KEY,
                                        user_id             BIGINT          NOT NULL,
                                        jenis               VARCHAR(20)     NOT NULL,
    jumlah              NUMERIC(15, 2)  NOT NULL,
    tipe                VARCHAR(10)     NOT NULL DEFAULT 'SETOR',
    keterangan          TEXT,
    tanggal_transaksi   TIMESTAMP       NOT NULL DEFAULT NOW(),
    no_referensi        VARCHAR(50)     UNIQUE,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_simpanan_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT chk_simpanan_jenis  CHECK (jenis IN ('POKOK', 'WAJIB', 'SUKARELA')),
    CONSTRAINT chk_simpanan_tipe   CHECK (tipe  IN ('SETOR', 'TARIK')),
    CONSTRAINT chk_simpanan_jumlah CHECK (jumlah > 0)
    );

CREATE INDEX IF NOT EXISTS idx_simpanan_user_id      ON simpanan (user_id);
CREATE INDEX IF NOT EXISTS idx_simpanan_jenis        ON simpanan (jenis);
CREATE INDEX IF NOT EXISTS idx_simpanan_tipe         ON simpanan (tipe);
CREATE INDEX IF NOT EXISTS idx_simpanan_tanggal      ON simpanan (tanggal_transaksi DESC);
CREATE INDEX IF NOT EXISTS idx_simpanan_user_jenis   ON simpanan (user_id, jenis);
CREATE INDEX IF NOT EXISTS idx_simpanan_no_referensi ON simpanan (no_referensi);

COMMENT ON TABLE  simpanan              IS 'Transaksi simpanan anggota (setor dan tarik)';
COMMENT ON COLUMN simpanan.jenis        IS 'POKOK | WAJIB | SUKARELA';
COMMENT ON COLUMN simpanan.tipe         IS 'SETOR = masuk, TARIK = keluar';
COMMENT ON COLUMN simpanan.no_referensi IS 'Nomor referensi unik transaksi, format SMP-{timestamp}';