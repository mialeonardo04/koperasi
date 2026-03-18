-- ============================================================
-- V5: Seed data awal — akun ADMIN dan MEMBER default
-- Password "admin123"  → BCrypt hash
-- Password "member123" → BCrypt hash
-- ============================================================

INSERT INTO users (
    nomor_anggota, nama_lengkap, email, password,
    no_telepon, alamat, tanggal_gabung,
    role, status, total_simpanan, created_at, updated_at
)
VALUES
    (
        'ADM-0001',
        'Administrator Koperasi',
        'admin@koperasi.id',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        '081200000000',
        'Kantor Koperasi',
        CURRENT_DATE,
        'ADMIN', 'AKTIF', 0, NOW(), NOW()
    ),
    (
        'MBR-0001',
        'Budi Santoso',
        'member@koperasi.id',
        '$2a$10$ByIUiNaRfBHER/FoKnMkuu/HrEBnLZ49HYC9DZ3pAWbvBGiLGDHwG',
        '081211111111',
        'Jl. Merdeka No. 1, Jakarta Pusat',
        CURRENT_DATE,
        'MEMBER', 'AKTIF', 0, NOW(), NOW()
    ),
    (
        'MBR-0002',
        'Siti Rahayu',
        'siti@koperasi.id',
        '$2a$10$ByIUiNaRfBHER/FoKnMkuu/HrEBnLZ49HYC9DZ3pAWbvBGiLGDHwG',
        '081222222222',
        'Jl. Sudirman No. 45, Jakarta Selatan',
        CURRENT_DATE,
        'MEMBER', 'AKTIF', 0, NOW(), NOW()
    )
    ON CONFLICT (email) DO NOTHING;