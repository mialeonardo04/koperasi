-- ============================================================
-- V7: Create transaksi_pending table
-- Tabel untuk menyimpan pengajuan transaksi yang menunggu persetujuan admin
-- ============================================================

CREATE TABLE IF NOT EXISTS transaksi_pending (
                                                 id                  BIGSERIAL PRIMARY KEY,
                                                 user_id             BIGINT          NOT NULL,
                                                 jenis_transaksi     VARCHAR(30)     NOT NULL,
    jenis_simpanan      VARCHAR(20),
    jumlah              NUMERIC(15, 2),
    keterangan          TEXT,
    angsuran_id         BIGINT,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    catatan_admin       TEXT,
    approved_by         BIGINT,
    tanggal_approval    TIMESTAMP,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_tp_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_tp_angsuran
    FOREIGN KEY (angsuran_id) REFERENCES angsuran_pinjaman (id)
    ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_tp_approved_by
    FOREIGN KEY (approved_by) REFERENCES users (id)
    ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT chk_tp_jenis
    CHECK (jenis_transaksi IN ('SETOR_SIMPANAN', 'TARIK_SIMPANAN', 'BAYAR_ANGSURAN')),

    CONSTRAINT chk_tp_status
    CHECK (status IN ('PENDING', 'DISETUJUI', 'DITOLAK')),

    CONSTRAINT chk_tp_jenis_simpanan
    CHECK (jenis_simpanan IN ('POKOK', 'WAJIB', 'SUKARELA') OR jenis_simpanan IS NULL)
    );

CREATE INDEX IF NOT EXISTS idx_tp_user_id    ON transaksi_pending (user_id);
CREATE INDEX IF NOT EXISTS idx_tp_status     ON transaksi_pending (status);
CREATE INDEX IF NOT EXISTS idx_tp_jenis      ON transaksi_pending (jenis_transaksi);
CREATE INDEX IF NOT EXISTS idx_tp_created_at ON transaksi_pending (created_at DESC);

CREATE OR REPLACE TRIGGER trg_tp_updated_at
    BEFORE UPDATE ON transaksi_pending
                      FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

COMMENT ON TABLE  transaksi_pending                 IS 'Pengajuan transaksi member yang menunggu persetujuan admin';
COMMENT ON COLUMN transaksi_pending.jenis_transaksi IS 'SETOR_SIMPANAN | TARIK_SIMPANAN | BAYAR_ANGSURAN';
COMMENT ON COLUMN transaksi_pending.status          IS 'PENDING | DISETUJUI | DITOLAK';