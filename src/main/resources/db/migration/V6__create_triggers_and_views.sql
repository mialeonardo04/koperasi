-- ============================================================
-- V6: Helper function & trigger — auto update updated_at
-- ============================================================

CREATE OR REPLACE FUNCTION fn_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
                         FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE OR REPLACE TRIGGER trg_simpanan_updated_at
    BEFORE UPDATE ON simpanan
                      FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE OR REPLACE TRIGGER trg_pinjaman_updated_at
    BEFORE UPDATE ON pinjaman
                      FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE OR REPLACE TRIGGER trg_angsuran_updated_at
    BEFORE UPDATE ON angsuran_pinjaman
                      FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- ============================================================
-- View: v_saldo_simpanan
-- ============================================================
CREATE OR REPLACE VIEW v_saldo_simpanan AS
SELECT
    u.id              AS user_id,
    u.nomor_anggota,
    u.nama_lengkap,
    COALESCE(SUM(CASE
                     WHEN s.jenis = 'POKOK'    AND s.tipe = 'SETOR' THEN  s.jumlah
                     WHEN s.jenis = 'POKOK'    AND s.tipe = 'TARIK' THEN -s.jumlah
                     ELSE 0 END), 0)           AS simpanan_pokok,
    COALESCE(SUM(CASE
                     WHEN s.jenis = 'WAJIB'    AND s.tipe = 'SETOR' THEN  s.jumlah
                     WHEN s.jenis = 'WAJIB'    AND s.tipe = 'TARIK' THEN -s.jumlah
                     ELSE 0 END), 0)           AS simpanan_wajib,
    COALESCE(SUM(CASE
                     WHEN s.jenis = 'SUKARELA' AND s.tipe = 'SETOR' THEN  s.jumlah
                     WHEN s.jenis = 'SUKARELA' AND s.tipe = 'TARIK' THEN -s.jumlah
                     ELSE 0 END), 0)           AS simpanan_sukarela,
    COALESCE(SUM(CASE
                     WHEN s.tipe = 'SETOR' THEN  s.jumlah
                     WHEN s.tipe = 'TARIK' THEN -s.jumlah
                     ELSE 0 END), 0)           AS total_simpanan
FROM users u
         LEFT JOIN simpanan s ON s.user_id = u.id
WHERE u.role = 'MEMBER'
GROUP BY u.id, u.nomor_anggota, u.nama_lengkap;

COMMENT ON VIEW v_saldo_simpanan IS
    'Saldo simpanan terkini per anggota per jenis (POKOK, WAJIB, SUKARELA)';

-- ============================================================
-- View: v_ringkasan_pinjaman
-- ============================================================
CREATE OR REPLACE VIEW v_ringkasan_pinjaman AS
SELECT
    u.id                                                AS user_id,
    u.nomor_anggota,
    u.nama_lengkap,
    COUNT(p.id)                                         AS total_pengajuan,
    COUNT(p.id) FILTER (WHERE p.status = 'DISETUJUI')  AS pinjaman_aktif,
    COUNT(p.id) FILTER (WHERE p.status = 'PENDING')    AS pinjaman_pending,
    COUNT(p.id) FILTER (WHERE p.status = 'LUNAS')      AS pinjaman_lunas,
    COALESCE(SUM(p.jumlah_pinjaman)
             FILTER (WHERE p.status = 'DISETUJUI'), 0)       AS total_pinjaman_aktif,
    COALESCE(SUM(p.sisa_pinjaman)
             FILTER (WHERE p.status = 'DISETUJUI'), 0)       AS total_sisa_pinjaman
FROM users u
         LEFT JOIN pinjaman p ON p.user_id = u.id
WHERE u.role = 'MEMBER'
GROUP BY u.id, u.nomor_anggota, u.nama_lengkap;

COMMENT ON VIEW v_ringkasan_pinjaman IS
    'Ringkasan status pinjaman per anggota';

-- ============================================================
-- View: v_angsuran_jatuh_tempo
-- ============================================================
CREATE OR REPLACE VIEW v_angsuran_jatuh_tempo AS
SELECT
    ap.id                                 AS angsuran_id,
    p.id                                  AS pinjaman_id,
    p.no_pinjaman,
    u.id                                  AS user_id,
    u.nomor_anggota,
    u.nama_lengkap,
    u.no_telepon,
    ap.periode_ke,
    ap.jumlah_angsuran,
    ap.tanggal_jatuh_tempo,
    CURRENT_DATE - ap.tanggal_jatuh_tempo AS hari_terlambat
FROM angsuran_pinjaman ap
         JOIN pinjaman p ON p.id = ap.pinjaman_id
         JOIN users    u ON u.id = p.user_id
WHERE ap.status = 'BELUM_BAYAR'
  AND ap.tanggal_jatuh_tempo < CURRENT_DATE
ORDER BY ap.tanggal_jatuh_tempo;

COMMENT ON VIEW v_angsuran_jatuh_tempo IS
    'Angsuran yang belum dibayar dan sudah melewati tanggal jatuh tempo';