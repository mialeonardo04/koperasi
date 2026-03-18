-- ============================================================
-- V10: Tambah kolom telegram_chat_id di tabel users
-- ============================================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS telegram_chat_id VARCHAR(50);

COMMENT ON COLUMN users.telegram_chat_id IS 'Chat ID Telegram untuk kirim notifikasi. Didapat dari bot Telegram.';