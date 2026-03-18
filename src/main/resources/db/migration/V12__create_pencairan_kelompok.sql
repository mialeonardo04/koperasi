-- ============================================================
-- V12: Tabel pencairan dana pinjaman kelompok
-- ============================================================

-- Tabel pencairan dana per anggota dari pool pinjaman kelompok
CREATE TABLE pencairan_kelompok (
                                    id                   BIGSERIAL    PRIMARY KEY,
                                    pinjaman_kelompok_id BIGINT       NOT NULL REFERENCES pinjaman_kelompok(id),
                                    user_id              BIGINT       NOT NULL REFERENCES users(id),
                                    jumlah               DECIMAL(15,2) NOT NULL,
                                    jatah_rata           DECIMAL(15,2) NOT NULL, -- jatah normal user ini
                                    melebihi_jatah       BOOLEAN      NOT NULL DEFAULT FALSE,
    -- Status approval
                                    status               VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    -- PENDING = menunggu leader / DISETUJUI / DITOLAK
                                    disetujui_oleh       BIGINT REFERENCES users(id), -- leader yg approve
                                    catatan              TEXT,
                                    tanggal_approval     TIMESTAMP,
    -- Jika melebihi jatah, wajib bayar minimal 1 angsuran
                                    wajib_bayar_angsuran BOOLEAN      NOT NULL DEFAULT FALSE,
                                    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
                                    updated_at           TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pencairan_pinjaman ON pencairan_kelompok(pinjaman_kelompok_id);
CREATE INDEX idx_pencairan_user     ON pencairan_kelompok(user_id);
CREATE INDEX idx_pencairan_status   ON pencairan_kelompok(status);

-- Update batasan min/max anggota (dihandle di aplikasi, bukan DB)
-- Update constraint jenis_transaksi di transaksi_pending untuk support PENCAIRAN_KELOMPOK
ALTER TABLE transaksi_pending
DROP CONSTRAINT IF EXISTS chk_tp_jenis;

ALTER TABLE transaksi_pending
    ADD CONSTRAINT chk_tp_jenis
        CHECK (jenis_transaksi IN (
                                   'SETOR_SIMPANAN',
                                   'TARIK_SIMPANAN',
                                   'BAYAR_ANGSURAN',
                                   'BAYAR_ANGSURAN_KELOMPOK',
                                   'PENCAIRAN_KELOMPOK'
            ));

COMMENT ON TABLE  pencairan_kelompok IS 'Riwayat pencairan dana pool pinjaman kelompok per anggota';
COMMENT ON COLUMN pencairan_kelompok.melebihi_jatah IS 'TRUE jika ambil lebih dari jatah rata-rata';
COMMENT ON COLUMN pencairan_kelompok.wajib_bayar_angsuran IS 'TRUE jika melebihi jatah — wajib bayar min 1 angsuran';