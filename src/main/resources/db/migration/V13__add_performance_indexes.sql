-- V13__add_performance_indexes.sql
-- Tambah index untuk query yang sering dipakai

-- Index untuk filter simpanan per user + jenis (dipakai di getSaldoByUserAndJenis)
CREATE INDEX IF NOT EXISTS idx_simpanan_user_jenis_tipe
    ON simpanan(user_id, jenis, tipe);

-- Index untuk filter transaksi pending per user + status
CREATE INDEX IF NOT EXISTS idx_tp_user_status
    ON transaksi_pending(user_id, status);

-- Index untuk kelompok_anggota lookup by user
CREATE INDEX IF NOT EXISTS idx_ka_user_kelompok
    ON kelompok_anggota(user_id, kelompok_id);

-- Index untuk pinjaman_kelompok per kelompok + status
CREATE INDEX IF NOT EXISTS idx_pk_kelompok_status
    ON pinjaman_kelompok(kelompok_id, status);

-- Index untuk angsuran_kelompok per pinjaman + status
CREATE INDEX IF NOT EXISTS idx_ak_pinjaman_status
    ON angsuran_kelompok(pinjaman_kelompok_id, status);

-- Index untuk pencairan per pinjaman + status
CREATE INDEX IF NOT EXISTS idx_pencairan_pinjaman_status
    ON pencairan_kelompok(pinjaman_kelompok_id, status);

-- Index untuk kelompok status (filter kelompok AKTIF)
CREATE INDEX IF NOT EXISTS idx_kelompok_status
    ON kelompok(status);

-- Partial index: hanya transaksi PENDING (query paling sering)
CREATE INDEX IF NOT EXISTS idx_tp_pending_only
    ON transaksi_pending(created_at DESC)
    WHERE status = 'PENDING';

-- Partial index: angsuran yang belum bayar
CREATE INDEX IF NOT EXISTS idx_ak_belum_bayar
    ON angsuran_kelompok(tanggal_jatuh_tempo)
    WHERE status = 'BELUM_BAYAR';