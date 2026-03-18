-- ============================================================
-- V4: Create angsuran_pinjaman table
-- ============================================================

CREATE TABLE IF NOT EXISTS angsuran_pinjaman (
                                                 id                   BIGSERIAL PRIMARY KEY,
                                                 pinjaman_id          BIGINT          NOT NULL,
                                                 periode_ke           INTEGER         NOT NULL,
                                                 jumlah_angsuran      NUMERIC(15, 2)  NOT NULL,
    pokok                NUMERIC(15, 2)  NOT NULL,
    bunga                NUMERIC(15, 2)  NOT NULL,
    tanggal_jatuh_tempo  DATE,
    tanggal_bayar        DATE,
    status               VARCHAR(20)     NOT NULL DEFAULT 'BELUM_BAYAR',
    no_referensi_bayar   VARCHAR(50),
    created_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_angsuran_pinjaman
    FOREIGN KEY (pinjaman_id) REFERENCES pinjaman (id)
    ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT uq_angsuran_periode
    UNIQUE (pinjaman_id, periode_ke),

    CONSTRAINT chk_angsuran_status
    CHECK (status IN ('BELUM_BAYAR', 'SUDAH_BAYAR', 'TERLAMBAT')),
    CONSTRAINT chk_angsuran_jumlah
    CHECK (jumlah_angsuran > 0),
    CONSTRAINT chk_angsuran_periode
    CHECK (periode_ke > 0)
    );

CREATE INDEX IF NOT EXISTS idx_angsuran_pinjaman_id      ON angsuran_pinjaman (pinjaman_id);
CREATE INDEX IF NOT EXISTS idx_angsuran_status           ON angsuran_pinjaman (status);
CREATE INDEX IF NOT EXISTS idx_angsuran_jatuh_tempo      ON angsuran_pinjaman (tanggal_jatuh_tempo);
CREATE INDEX IF NOT EXISTS idx_angsuran_pinjaman_periode ON angsuran_pinjaman (pinjaman_id, periode_ke);

COMMENT ON TABLE  angsuran_pinjaman                    IS 'Jadwal dan riwayat pembayaran angsuran pinjaman';
COMMENT ON COLUMN angsuran_pinjaman.periode_ke         IS 'Urutan angsuran ke-N dari total tenor';
COMMENT ON COLUMN angsuran_pinjaman.pokok              IS 'Komponen pokok dalam satu angsuran';
COMMENT ON COLUMN angsuran_pinjaman.bunga              IS 'Komponen bunga dalam satu angsuran';
COMMENT ON COLUMN angsuran_pinjaman.status             IS 'BELUM_BAYAR | SUDAH_BAYAR | TERLAMBAT';
COMMENT ON COLUMN angsuran_pinjaman.no_referensi_bayar IS 'Nomor referensi pembayaran, format PAY-{timestamp}';