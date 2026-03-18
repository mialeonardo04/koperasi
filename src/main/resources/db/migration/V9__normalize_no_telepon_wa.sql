-- ============================================================
-- V9: Normalisasi no_telepon ke format WhatsApp (628xxxxxxxxxx)
-- ============================================================

-- Fungsi helper: konversi nomor lokal ke format internasional
-- 08xxxxxxxx    → 628xxxxxxxx
-- +628xxxxxxxx  → 628xxxxxxxx
-- 628xxxxxxxx   → tetap
CREATE OR REPLACE FUNCTION normalize_wa_number(phone TEXT)
RETURNS TEXT AS $$
BEGIN
    IF phone IS NULL OR phone = '' THEN
        RETURN NULL;
END IF;

    -- Hapus spasi, strip, dan karakter non-digit kecuali leading +
    phone := regexp_replace(phone, '[\s\-\(\)]', '', 'g');

    -- +628... → 628...
    IF phone LIKE '+62%' THEN
        RETURN substring(phone FROM 2);
END IF;

    -- 08... → 628...
    IF phone LIKE '08%' THEN
        RETURN '62' || substring(phone FROM 2);
END IF;

    -- Sudah 628... → tetap
    IF phone LIKE '628%' THEN
        RETURN phone;
END IF;

    -- Format lain: kembalikan apa adanya (admin harus update manual)
RETURN phone;
END;
$$ LANGUAGE plpgsql;

-- Update semua nomor yang ada ke format WA
UPDATE users
SET no_telepon = normalize_wa_number(no_telepon),
    updated_at = NOW()
WHERE no_telepon IS NOT NULL AND no_telepon != '';

-- Update seed data default ke nomor test yang valid
UPDATE users SET no_telepon = '6281200000000' WHERE email = 'admin@koperasi.id'  AND no_telepon = '6281200000000' IS FALSE;
UPDATE users SET no_telepon = '6281211111111' WHERE email = 'member@koperasi.id' AND no_telepon = '6281211111111' IS FALSE;
UPDATE users SET no_telepon = '6281222222222' WHERE email = 'siti@koperasi.id'   AND no_telepon = '6281222222222' IS FALSE;

-- Tampilkan hasil
SELECT nomor_anggota, nama_lengkap, email, no_telepon FROM users ORDER BY created_at;