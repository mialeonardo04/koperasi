# Koperasi Leyangan — Backend API

Dokumentasi lengkap REST API Sistem Simpan Pinjam Digital Koperasi Leyangan.

- **Base URL:** `http://localhost:8080/api`
- **Format Response:** `application/json`
- **Autentikasi:** Bearer Token (JWT)

---

## Setup & Menjalankan

### Prasyarat
- Java 21
- Maven
- PostgreSQL (port 5516)

### Konfigurasi Database

```sql
CREATE DATABASE koperasi;
CREATE USER koperasi WITH PASSWORD 'your_db_password';
GRANT ALL PRIVILEGES ON DATABASE koperasi TO koperasi;
```
### Perhatian!!

```bash
GANTI DB PASSWORD DENGAN PASSWORD ANDA PADA applicatiton.properties
```

### Jalankan

```bash
mvn spring-boot:run
```

### Buka Port Firewall (Windows)

```powershell
netsh advfirewall firewall add rule name="Spring Boot" dir=in action=allow protocol=TCP localport=8080
```

---

## Format Response

```json
{
  "success": true,
  "message": "Berhasil",
  "data": { ... },
  "timestamp": "2026-03-17T10:00:00"
}
```

---

## Autentikasi

### Login
```
POST /auth/login
```
**Body:**
```json
{
  "email": "admin@koperasi.id",
  "password": "admin123"
}
```
**Response:**
```json
{
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": 1,
      "nomorAnggota": "ADM-0001",
      "namaLengkap": "Administrator Koperasi",
      "email": "admin@koperasi.id",
      "role": "ADMIN",
      "status": "AKTIF",
      "totalSimpanan": 0
    }
  }
}
```

Gunakan `accessToken` di header setiap request:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Register Member Baru
```
POST /auth/register
```
**Body:**
```json
{
  "namaLengkap": "Budi Santoso",
  "email": "budi@email.com",
  "password": "password123",
  "noTelepon": "6281234567890",
  "alamat": "Jl. Merdeka No. 1"
}
```

### Lihat Profil
```
GET /auth/profile
Authorization: Bearer <token>
```

### Ganti Password
```
PUT /auth/change-password
Authorization: Bearer <token>
```
**Body:**
```json
{
  "passwordLama": "password123",
  "passwordBaru": "newpassword123"
}
```

---

## Simpanan (Member)

### Lihat Saldo
```
GET /simpanan/saldo
Authorization: Bearer <token>
```
**Response:**
```json
{
  "data": {
    "simpananPokok": 500000,
    "simpananWajib": 1500000,
    "simpananSukarela": 250000,
    "totalSimpanan": 2250000
  }
}
```

### Riwayat Transaksi
```
GET /simpanan/riwayat?page=0&size=10
Authorization: Bearer <token>
```

---

## Pinjaman (Member)

### Ajukan Pinjaman
```
POST /pinjaman/ajukan
Authorization: Bearer <token>
```
**Body:**
```json
{
  "jumlahPinjaman": 5000000,
  "tenorBulan": 12,
  "tujuanPinjaman": "Modal usaha"
}
```

### Riwayat Pinjaman
```
GET /pinjaman/riwayat?page=0&size=10
Authorization: Bearer <token>
```

### Detail Pinjaman
```
GET /pinjaman/{id}
Authorization: Bearer <token>
```

### Jadwal Angsuran
```
GET /pinjaman/{id}/jadwal-angsuran
Authorization: Bearer <token>
```

---

## Pengajuan Transaksi (Member → Approval Admin)

### Ajukan Setor Simpanan
```
POST /pengajuan/setor
Authorization: Bearer <token>
```
**Body:**
```json
{
  "jenisSimpanan": "WAJIB",
  "jumlah": 100000,
  "keterangan": "Simpanan wajib Maret 2026",
  "buktiBayar": "2026/03/abc123.jpg"
}
```
> `jenisSimpanan`: `POKOK` | `WAJIB` | `SUKARELA`

### Ajukan Tarik Simpanan
```
POST /pengajuan/tarik
Authorization: Bearer <token>
```
**Body:**
```json
{
  "jenisSimpanan": "SUKARELA",
  "jumlah": 500000,
  "keterangan": "Keperluan mendesak",
  "buktiBayar": "2026/03/abc123.jpg"
}
```

### Ajukan Bayar Angsuran
```
POST /pengajuan/bayar-angsuran
Authorization: Bearer <token>
```
**Body:**
```json
{
  "angsuranId": 5,
  "keterangan": "Pembayaran angsuran ke-3",
  "buktiBayar": "2026/03/abc123.jpg"
}
```

### Riwayat Pengajuan
```
GET /pengajuan/riwayat?page=0&size=15
Authorization: Bearer <token>
```

---

## Upload Bukti Pembayaran

### Upload File
```
POST /upload/bukti-bayar
Authorization: Bearer <token>
Content-Type: multipart/form-data
```
**Form Data:** `file` → file gambar (JPG/PNG/WebP, maks 1MB)

**Response:**
```json
{
  "data": "2026/03/a1b2c3d4e5f6.jpg"
}
```

### Lihat Foto Bukti (Publik)
```
GET /upload/bukti-bayar/view?path=2026/03/a1b2c3d4e5f6.jpg
```

---

## Dashboard Member
```
GET /report/dashboard
Authorization: Bearer <token>
```

---

## Admin — Manajemen Anggota

> Semua endpoint `/admin/**` memerlukan role `ADMIN`

### Daftar Semua Anggota
```
GET /admin/members?page=0&size=20&search=budi
Authorization: Bearer <token_admin>
```

### Buat User Baru
```
POST /admin/members/create
Authorization: Bearer <token_admin>
```
**Body:**
```json
{
  "namaLengkap": "Siti Rahayu",
  "email": "siti@email.com",
  "password": "password123",
  "noTelepon": "6281234567890",
  "alamat": "Jl. Sudirman No. 5",
  "role": "MEMBER"
}
```
> `role`: `MEMBER` | `ADMIN`

### Ubah Status Anggota
```
PUT /admin/members/{id}/status?status=SUSPEND
Authorization: Bearer <token_admin>
```
> `status`: `AKTIF` | `NON_AKTIF` | `SUSPEND`

### Ubah Role Anggota
```
PUT /admin/members/{id}/role
Authorization: Bearer <token_admin>
```
**Body:**
```json
{ "role": "ADMIN" }
```

---

## Admin — Manajemen Simpanan

### Semua Transaksi Simpanan
```
GET /admin/simpanan?userId=1&page=0&size=15
Authorization: Bearer <token_admin>
```

### Saldo Anggota
```
GET /admin/simpanan/saldo/{userId}
Authorization: Bearer <token_admin>
```

### Catat Setoran Langsung
```
POST /admin/simpanan/setor/{userId}
Authorization: Bearer <token_admin>
```
**Body:**
```json
{
  "jenis": "WAJIB",
  "jumlah": 100000,
  "keterangan": "Simpanan wajib Maret 2026"
}
```

### Catat Penarikan Langsung
```
POST /admin/simpanan/tarik/{userId}
Authorization: Bearer <token_admin>
```

---

## Admin — Manajemen Pinjaman

### Semua Pinjaman
```
GET /admin/pinjaman?userId=1&status=PENDING&page=0&size=15
Authorization: Bearer <token_admin>
```
> `status`: `PENDING` | `DISETUJUI` | `DITOLAK` | `LUNAS` | `MACET`

### Jumlah Pinjaman Pending
```
GET /admin/pinjaman/pending-count
Authorization: Bearer <token_admin>
```

### Proses Persetujuan Pinjaman
```
PUT /admin/pinjaman/{id}/proses
Authorization: Bearer <token_admin>
```
**Body (Setujui):**
```json
{
  "disetujui": true,
  "bungaPerBulan": 1.5,
  "keteranganAdmin": "Disetujui sesuai prosedur"
}
```
**Body (Tolak):**
```json
{
  "disetujui": false,
  "keteranganAdmin": "Tidak memenuhi syarat"
}
```

### Jadwal Angsuran (Admin)
```
GET /admin/pinjaman/{id}/jadwal-angsuran
Authorization: Bearer <token_admin>
```

---

## Admin — Approval Transaksi Pending

### Ringkasan Pending
```
GET /admin/pengajuan/summary
Authorization: Bearer <token_admin>
```

### Semua Transaksi Pending
```
GET /admin/pengajuan/pending
Authorization: Bearer <token_admin>
```

### Semua Transaksi dengan Filter
```
GET /admin/pengajuan?status=PENDING&page=0&size=20
Authorization: Bearer <token_admin>
```

### Proses Approval
```
PUT /admin/pengajuan/{id}/proses
Authorization: Bearer <token_admin>
```
**Body (Setujui):**
```json
{
  "disetujui": true,
  "catatanAdmin": null
}
```
**Body (Tolak):**
```json
{
  "disetujui": false,
  "catatanAdmin": "Bukti pembayaran tidak valid"
}
```

---

## Admin — Report

### Dashboard Admin
```
GET /admin/report/dashboard
Authorization: Bearer <token_admin>
```

### Rekap Simpanan
```
GET /admin/report/rekap-simpanan
Authorization: Bearer <token_admin>
```

### Rekap Pinjaman
```
GET /admin/report/rekap-pinjaman
Authorization: Bearer <token_admin>
```

---

## Admin — Test WhatsApp
```
GET /admin/whatsapp/test?to=6282124977876
Authorization: Bearer <token_admin>
```

---

## Pagination

| Parameter | Default | Keterangan |
|-----------|---------|------------|
| `page` | 0 | Halaman ke- (mulai dari 0) |
| `size` | 10–20 | Jumlah data per halaman |
| `sort` | - | Contoh: `createdAt,desc` |

**Response:**
```json
{
  "data": {
    "content": [...],
    "page": {
      "totalElements": 50,
      "totalPages": 5,
      "number": 0,
      "size": 10
    }
  }
}
```

---

## HTTP Status Code

| Kode | Keterangan |
|------|------------|
| 200 | Berhasil |
| 400 | Request tidak valid |
| 401 | Token tidak ada atau expired |
| 403 | Tidak punya akses |
| 404 | Data tidak ditemukan |
| 500 | Error server |

---

## Akun Default

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@koperasi.id | admin123 |
| Member | member@koperasi.id | member123 |
| Member | siti@koperasi.id | member123 |