-- ============================================================
-- V14: Create audit_logs table
-- ============================================================

CREATE TABLE IF NOT EXISTS audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id),
    action          VARCHAR(255)    NOT NULL,
    entity_name     VARCHAR(100),
    entity_id       VARCHAR(100),
    old_value       TEXT,
    new_value       TEXT,
    ip_address      VARCHAR(45),
    user_agent      TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id     ON audit_logs (user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action      ON audit_logs (action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity_name ON audit_logs (entity_name);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at  ON audit_logs (created_at);

COMMENT ON TABLE  audit_logs                IS 'Tabel untuk mencatat aktivitas dan perubahan data';
COMMENT ON COLUMN audit_logs.action         IS 'Jenis aksi (CREATE, UPDATE, DELETE, APPROVE, dll)';
COMMENT ON COLUMN audit_logs.entity_name    IS 'Nama tabel atau entitas yang berubah';
COMMENT ON COLUMN audit_logs.entity_id      IS 'ID record yang berubah';
COMMENT ON COLUMN audit_logs.old_value      IS 'Data sebelum perubahan (JSON)';
COMMENT ON COLUMN audit_logs.new_value      IS 'Data setelah perubahan (JSON)';
