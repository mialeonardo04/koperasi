package com.koperasi.service;

import com.koperasi.entity.*;
import com.koperasi.repository.*;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlipService {

    private final TransaksiPendingRepository pendingRepository;
    private final AngsuranKelompokRepository angsuranKelompokRepository;
    private final UserRepository             userRepository;
    private final SimpananRepository         simpananRepository;
    private final KelompokAnggotaRepository  kelompokAnggotaRepository;

    // ── Warna tema koperasi ──────────────────────────────────────────
    private static final Color CLR_PRIMARY    = new Color(45, 106, 79);   // hijau gelap
    private static final Color CLR_PRIMARY_LT = new Color(209, 236, 220); // hijau muda
    private static final Color CLR_DARK       = new Color(31, 41, 55);
    private static final Color CLR_GRAY       = new Color(107, 114, 128);
    private static final Color CLR_BORDER     = new Color(229, 231, 235);
    private static final Color CLR_WHITE      = Color.WHITE;

    private static final DateTimeFormatter FMT_DATE     = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final DateTimeFormatter FMT_DATETIME = DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm");

    // ── Font helpers ─────────────────────────────────────────────────
    private Font fontTitle()    { return new Font(Font.HELVETICA, 16, Font.BOLD,   CLR_PRIMARY); }
    private Font fontSubtitle() { return new Font(Font.HELVETICA, 9,  Font.NORMAL, CLR_GRAY);    }
    private Font fontHeader()   { return new Font(Font.HELVETICA, 8,  Font.BOLD,   CLR_WHITE);   }
    private Font fontLabel()    { return new Font(Font.HELVETICA, 8,  Font.NORMAL, CLR_GRAY);    }
    private Font fontValue()    { return new Font(Font.HELVETICA, 9,  Font.BOLD,   CLR_DARK);    }
    private Font fontMoney()    { return new Font(Font.HELVETICA, 13, Font.BOLD,   CLR_PRIMARY); }
    private Font fontSmall()    { return new Font(Font.HELVETICA, 7,  Font.NORMAL, CLR_GRAY);    }
    private Font fontBold()     { return new Font(Font.HELVETICA, 9,  Font.BOLD,   CLR_DARK);    }

    // ─────────────────────────────────────────────────────────────────
    // 1. Slip Setoran Simpanan
    // ─────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public byte[] generateSlipSetoran(Long transaksiId) {
        TransaksiPending t = pendingRepository.findById(transaksiId)
                .orElseThrow(() -> new IllegalArgumentException("Transaksi tidak ditemukan"));

        if (t.getStatus() != TransaksiPending.StatusPending.DISETUJUI) {
            throw new IllegalStateException("Slip hanya tersedia untuk transaksi yang sudah disetujui");
        }

        try {
            Document doc = new Document(new Rectangle(400, 580), 28, 28, 28, 28);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, out);
            doc.open();

            addKopSurat(doc, "BUKTI SETORAN SIMPANAN");

            // Info transaksi
            String jenis  = t.getJenisSimpanan() != null ? t.getJenisSimpanan().name() : "-";
            String tipe   = t.getJenisTransaksi().name().contains("TARIK") ? "PENARIKAN" : "SETORAN";
            String noRef  = "TRX-" + t.getId();
            String tgl    = t.getTanggalApproval() != null
                    ? t.getTanggalApproval().format(FMT_DATETIME)
                    : LocalDateTime.now().format(FMT_DATETIME);

            addInfoBox(doc, new String[][]{
                    {"No. Referensi", noRef},
                    {"Tanggal",       tgl},
                    {"Tipe",          tipe},
                    {"Jenis Simpanan", jenis},
                    {"Nama Anggota",  t.getUser().getNamaLengkap()},
                    {"No. Anggota",   t.getUser().getNomorAnggota()},
            });

            addJumlahBox(doc, "JUMLAH " + tipe, t.getJumlah());

            // Saldo setelah transaksi
            if (t.getJenisSimpanan() != null) {
                BigDecimal saldo = simpananRepository.getSaldoByUserAndJenis(
                        t.getUser().getId(), t.getJenisSimpanan());
                addSaldoInfo(doc, "Saldo " + jenis + " saat ini", saldo);
            }

            if (t.getKeterangan() != null && !t.getKeterangan().isBlank()) {
                addKeterangan(doc, t.getKeterangan());
            }

            addFooter(doc);
            doc.close();
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Error generate slip setoran: {}", e.getMessage());
            throw new RuntimeException("Gagal membuat slip PDF");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 2. Slip Angsuran Kelompok
    // ─────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public byte[] generateSlipAngsuran(Long angsuranId, Long requestUserId) {
        AngsuranKelompok angsuran = angsuranKelompokRepository.findById(angsuranId)
                .orElseThrow(() -> new IllegalArgumentException("Angsuran tidak ditemukan"));

        if (angsuran.getStatus() != AngsuranKelompok.StatusAngsuran.SUDAH_BAYAR) {
            throw new IllegalStateException("Slip hanya tersedia untuk angsuran yang sudah dibayar");
        }

        // Validasi: user harus anggota kelompok ini
        Long kelompokId = angsuran.getPinjamanKelompok().getKelompok().getId();
        boolean isAnggota = kelompokAnggotaRepository
                .existsByKelompokIdAndUserId(kelompokId, requestUserId);
        if (!isAnggota) {
            throw new IllegalStateException("Anda bukan anggota kelompok ini");
        }

        PinjamanKelompok pinjaman = angsuran.getPinjamanKelompok();
        Kelompok kelompok         = pinjaman.getKelompok();
        User dibayarOleh          = angsuran.getDibayarOleh();

        try {
            Document doc = new Document(new Rectangle(400, 600), 28, 28, 28, 28);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, out);
            doc.open();

            addKopSurat(doc, "BUKTI BAYAR ANGSURAN KELOMPOK");

            String metode = angsuran.getMetodeBayar() != null
                    ? (angsuran.getMetodeBayar() == AngsuranKelompok.MetodeBayar.TRANSFER ? "Transfer Bank" : "Potong Simpanan")
                    : "-";
            String tglBayar = angsuran.getTanggalBayar() != null
                    ? angsuran.getTanggalBayar().format(FMT_DATE)
                    : "-";
            String dibayarOlehNama = dibayarOleh != null ? dibayarOleh.getNamaLengkap() : "-";

            addInfoBox(doc, new String[][]{
                    {"No. Referensi",  angsuran.getNoReferensiBayar() != null ? angsuran.getNoReferensiBayar() : "PAY-" + angsuranId},
                    {"Tanggal Bayar",  tglBayar},
                    {"Kelompok",       kelompok.getNamaKelompok()},
                    {"No. Pinjaman",   pinjaman.getNoPinjaman()},
                    {"Angsuran Ke-",   String.valueOf(angsuran.getPeriodeKe()) + " dari " + pinjaman.getTenorBulan()},
                    {"Dibayar Oleh",   dibayarOlehNama},
                    {"Metode Bayar",   metode},
            });

            addJumlahBox(doc, "JUMLAH ANGSURAN", angsuran.getJumlahAngsuran());

            // Detail angsuran
            addRincianAngsuran(doc, angsuran.getPokok(), angsuran.getBunga());

            // Sisa pinjaman
            addSaldoInfo(doc, "Sisa Pinjaman Kelompok", pinjaman.getSisaPinjaman());

            addFooter(doc);
            doc.close();
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Error generate slip angsuran: {}", e.getMessage());
            throw new RuntimeException("Gagal membuat slip PDF");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 3. Kartu Simpanan Member
    // ─────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public byte[] generateKartuSimpanan(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        BigDecimal pokok    = simpananRepository.getSaldoByUserAndJenis(userId, Simpanan.JenisSimpanan.POKOK);
        BigDecimal wajib    = simpananRepository.getSaldoByUserAndJenis(userId, Simpanan.JenisSimpanan.WAJIB);
        BigDecimal sukarela = simpananRepository.getSaldoByUserAndJenis(userId, Simpanan.JenisSimpanan.SUKARELA);
        BigDecimal total    = pokok.add(wajib).add(sukarela);

        try {
            Document doc = new Document(new Rectangle(400, 520), 28, 28, 28, 28);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, out);
            doc.open();

            addKopSurat(doc, "KARTU SIMPANAN ANGGOTA");

            addInfoBox(doc, new String[][]{
                    {"No. Anggota",  user.getNomorAnggota()},
                    {"Nama Lengkap", user.getNamaLengkap()},
                    {"Status",       user.getStatus().name()},
                    {"Tgl Gabung",   user.getTanggalGabung() != null ? user.getTanggalGabung().format(FMT_DATE) : "-"},
                    {"Dicetak",      LocalDate.now().format(FMT_DATE)},
            });

            // Tabel saldo
            addTabelSaldo(doc, pokok, wajib, sukarela, total);

            addFooter(doc);
            doc.close();
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Error generate kartu simpanan: {}", e.getMessage());
            throw new RuntimeException("Gagal membuat kartu simpanan PDF");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Helper: Kop Surat
    // ─────────────────────────────────────────────────────────────────
    private void addKopSurat(Document doc, String judul) throws DocumentException {
        // Header box
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(CLR_PRIMARY);
        cell.setPadding(14);
        cell.setBorder(Rectangle.NO_BORDER);

        Paragraph nama = new Paragraph("KOPERASI LEYANGAN", fontTitle());
        nama.getFont().setColor(CLR_WHITE);
        nama.getFont().setSize(14);
        cell.addElement(nama);

        Paragraph sub = new Paragraph("Kabupaten Semarang", fontSubtitle());
        sub.getFont().setColor(new Color(187, 247, 208));
        cell.addElement(sub);

        header.addCell(cell);
        doc.add(header);
        doc.add(Chunk.NEWLINE);

        // Judul slip
        PdfPTable judulTable = new PdfPTable(1);
        judulTable.setWidthPercentage(100);
        PdfPCell judulCell = new PdfPCell(new Phrase(judul, fontBold()));
        judulCell.setBackgroundColor(CLR_PRIMARY_LT);
        judulCell.setPadding(8);
        judulCell.setBorder(Rectangle.NO_BORDER);
        judulCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        judulTable.addCell(judulCell);
        doc.add(judulTable);
        doc.add(Chunk.NEWLINE);
    }

    // ─────────────────────────────────────────────────────────────────
    // Helper: Info Box (label - value rows)
    // ─────────────────────────────────────────────────────────────────
    private void addInfoBox(Document doc, String[][] rows) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{35, 5, 60});
        table.setWidthPercentage(100);

        for (int i = 0; i < rows.length; i++) {
            Color bg = i % 2 == 0 ? CLR_WHITE : new Color(249, 250, 251);

            PdfPCell label = new PdfPCell(new Phrase(rows[i][0], fontLabel()));
            label.setBackgroundColor(bg);
            label.setPadding(6);
            label.setBorderColor(CLR_BORDER);

            PdfPCell sep = new PdfPCell(new Phrase(":", fontLabel()));
            sep.setBackgroundColor(bg);
            sep.setPadding(6);
            sep.setBorderColor(CLR_BORDER);
            sep.setHorizontalAlignment(Element.ALIGN_CENTER);

            PdfPCell value = new PdfPCell(new Phrase(rows[i][1], fontValue()));
            value.setBackgroundColor(bg);
            value.setPadding(6);
            value.setBorderColor(CLR_BORDER);

            table.addCell(label);
            table.addCell(sep);
            table.addCell(value);
        }
        doc.add(table);
        doc.add(Chunk.NEWLINE);
    }

    // ─────────────────────────────────────────────────────────────────
    // Helper: Jumlah Box
    // ─────────────────────────────────────────────────────────────────
    private void addJumlahBox(Document doc, String label, BigDecimal jumlah) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(CLR_PRIMARY);
        cell.setPadding(14);
        cell.setBorder(Rectangle.NO_BORDER);

        Paragraph lbl = new Paragraph(label, fontLabel());
        lbl.getFont().setColor(new Color(187, 247, 208));
        lbl.getFont().setSize(8);
        cell.addElement(lbl);

        Paragraph amount = new Paragraph(formatRupiah(jumlah), fontMoney());
        amount.getFont().setColor(CLR_WHITE);
        amount.getFont().setSize(18);
        cell.addElement(amount);

        table.addCell(cell);
        doc.add(table);
        doc.add(Chunk.NEWLINE);
    }

    // ─────────────────────────────────────────────────────────────────
    // Helper: Saldo Info
    // ─────────────────────────────────────────────────────────────────
    private void addSaldoInfo(Document doc, String label, BigDecimal saldo) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{50, 50});
        table.setWidthPercentage(100);

        PdfPCell lbl = new PdfPCell(new Phrase(label, fontLabel()));
        lbl.setBackgroundColor(CLR_PRIMARY_LT);
        lbl.setPadding(8);
        lbl.setBorder(Rectangle.NO_BORDER);

        Font valFont = fontBold();
        valFont.setColor(CLR_PRIMARY);
        PdfPCell val = new PdfPCell(new Phrase(formatRupiah(saldo), valFont));
        val.setBackgroundColor(CLR_PRIMARY_LT);
        val.setPadding(8);
        val.setBorder(Rectangle.NO_BORDER);
        val.setHorizontalAlignment(Element.ALIGN_RIGHT);

        table.addCell(lbl);
        table.addCell(val);
        doc.add(table);
        doc.add(Chunk.NEWLINE);
    }

    // ─────────────────────────────────────────────────────────────────
    // Helper: Rincian Angsuran
    // ─────────────────────────────────────────────────────────────────
    private void addRincianAngsuran(Document doc, BigDecimal pokok, BigDecimal bunga) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{50, 50});
        table.setWidthPercentage(100);

        // Header
        PdfPCell h1 = new PdfPCell(new Phrase("Rincian Angsuran", fontHeader()));
        h1.setColspan(2);
        h1.setBackgroundColor(CLR_DARK);
        h1.setPadding(6);
        h1.setBorder(Rectangle.NO_BORDER);
        table.addCell(h1);

        addRow(table, "Pokok", formatRupiah(pokok));
        addRow(table, "Bunga", formatRupiah(bunga));

        doc.add(table);
        doc.add(Chunk.NEWLINE);
    }

    // ─────────────────────────────────────────────────────────────────
    // Helper: Tabel Saldo Simpanan
    // ─────────────────────────────────────────────────────────────────
    private void addTabelSaldo(Document doc, BigDecimal pokok, BigDecimal wajib,
                               BigDecimal sukarela, BigDecimal total) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{50, 50});
        table.setWidthPercentage(100);

        PdfPCell header = new PdfPCell(new Phrase("Rekap Saldo Simpanan", fontHeader()));
        header.setColspan(2);
        header.setBackgroundColor(CLR_PRIMARY);
        header.setPadding(8);
        header.setBorder(Rectangle.NO_BORDER);
        table.addCell(header);

        addRow(table, "Simpanan Pokok",    formatRupiah(pokok));
        addRow(table, "Simpanan Wajib",    formatRupiah(wajib));
        addRow(table, "Simpanan Sukarela", formatRupiah(sukarela));

        // Total row
        PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL SIMPANAN", fontBold()));
        totalLabel.setBackgroundColor(CLR_PRIMARY_LT);
        totalLabel.setPadding(8);
        totalLabel.setBorderColor(CLR_BORDER);

        Font totalFont = fontBold();
        totalFont.setColor(CLR_PRIMARY);
        PdfPCell totalVal = new PdfPCell(new Phrase(formatRupiah(total), totalFont));
        totalVal.setBackgroundColor(CLR_PRIMARY_LT);
        totalVal.setPadding(8);
        totalVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalVal.setBorderColor(CLR_BORDER);

        table.addCell(totalLabel);
        table.addCell(totalVal);

        doc.add(table);
        doc.add(Chunk.NEWLINE);
    }

    private void addRow(PdfPTable table, String label, String value) {
        PdfPCell lbl = new PdfPCell(new Phrase(label, fontLabel()));
        lbl.setPadding(6);
        lbl.setBorderColor(CLR_BORDER);

        PdfPCell val = new PdfPCell(new Phrase(value, fontValue()));
        val.setPadding(6);
        val.setHorizontalAlignment(Element.ALIGN_RIGHT);
        val.setBorderColor(CLR_BORDER);

        table.addCell(lbl);
        table.addCell(val);
    }

    private void addKeterangan(Document doc, String ket) throws DocumentException {
        Paragraph p = new Paragraph("Keterangan: " + ket, fontSmall());
        p.setSpacingBefore(4);
        doc.add(p);
        doc.add(Chunk.NEWLINE);
    }

    private void addFooter(Document doc) throws DocumentException {
        doc.add(Chunk.NEWLINE);
        Paragraph garis = new Paragraph("─────────────────────────────────────────", fontSmall());
        doc.add(garis);

        Paragraph footer = new Paragraph(
                "Dokumen ini dicetak secara otomatis oleh Sistem Koperasi Leyangan.\n" +
                        "Dicetak pada: " + LocalDateTime.now().format(FMT_DATETIME),
                fontSmall());
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);
    }

    private String formatRupiah(BigDecimal amount) {
        if (amount == null) return "Rp 0";
        java.text.NumberFormat fmt = java.text.NumberFormat.getInstance(new java.util.Locale("id", "ID"));
        fmt.setMinimumFractionDigits(0);
        fmt.setMaximumFractionDigits(0);
        return "Rp " + fmt.format(amount);
    }
}