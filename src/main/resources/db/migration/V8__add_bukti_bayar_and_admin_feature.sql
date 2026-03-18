-- ============================================================
-- V8: Add bukti_bayar to transaksi_pending
--     Add admin role support (allow creating admin users)
-- ============================================================

-- Kolom bukti bayar (path relatif ke file upload)
ALTER TABLE transaksi_pending
    ADD COLUMN IF NOT EXISTS bukti_bayar VARCHAR(500);

COMMENT ON COLUMN transaksi_pending.bukti_bayar IS 'Path relatif file bukti pembayaran (relatif dari app.upload.dir)';