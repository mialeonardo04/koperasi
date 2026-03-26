package com.koperasi.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class JacocoSummary {

    private static final Set<String> INCLUDED_CLASSES = Set.of(
            "com.koperasi.config.OpenApiConfig",
            "com.koperasi.service.AuthService",
            "com.koperasi.service.AuditLogService",
            "com.koperasi.service.UserManagementService",
            "com.koperasi.service.PinjamanService",
            "com.koperasi.service.SimpananService"
    );

    public static void main(String[] args) throws Exception {
        String csvPath = args != null && args.length > 0 ? args[0] : "target/site/jacoco/jacoco.csv";
        Path path = Path.of(csvPath);

        if (!Files.exists(path)) {
            System.out.println("JaCoCo summary: report not found at " + path.toAbsolutePath());
            return;
        }

        Totals totals = new Totals();
        Totals allTotals = new Totals();

        try (BufferedReader br = new BufferedReader(new FileReader(new File(csvPath)))) {
            String header = br.readLine();
            if (header == null) {
                System.out.println("JaCoCo summary: empty report at " + path.toAbsolutePath());
                return;
            }

            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = splitCsv(line);
                if (parts.length < 12) {
                    continue;
                }

                String pkg = parts[1];
                String cls = parts[2];
                String fqn = pkg + "." + cls;

                long branchMissed = parseLong(parts[5]);
                long branchCovered = parseLong(parts[6]);
                long lineMissed = parseLong(parts[7]);
                long lineCovered = parseLong(parts[8]);

                allTotals.add(branchMissed, branchCovered, lineMissed, lineCovered);
                if (isIncluded(fqn)) {
                    totals.add(branchMissed, branchCovered, lineMissed, lineCovered);
                }
            }
        }

        System.out.println(format("JaCoCo Coverage (selected)", totals));
        System.out.println(format("JaCoCo Coverage (all)", allTotals));
    }

    private static boolean isIncluded(String fqn) {
        if (INCLUDED_CLASSES.contains(fqn)) {
            return true;
        }
        for (String included : INCLUDED_CLASSES) {
            if (fqn.startsWith(included + "$")) {
                return true;
            }
        }
        return false;
    }

    private static String format(String label, Totals totals) {
        String linePct = pct(totals.lineCovered, totals.lineMissed);
        String branchPct = pct(totals.branchCovered, totals.branchMissed);
        return String.format(
                Locale.ROOT,
                "%s: LINE %s%% (covered=%d missed=%d) | BRANCH %s%% (covered=%d missed=%d)",
                label,
                linePct,
                totals.lineCovered,
                totals.lineMissed,
                branchPct,
                totals.branchCovered,
                totals.branchMissed
        );
    }

    private static String pct(long covered, long missed) {
        long total = covered + missed;
        if (total <= 0) {
            return "0.00";
        }
        double ratio = (covered * 100.0) / total;
        return String.format(Locale.ROOT, "%.2f", ratio);
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return 0L;
        }
    }

    private static String[] splitCsv(String line) {
        return line.split(",", -1);
    }

    private static final class Totals {
        long branchMissed;
        long branchCovered;
        long lineMissed;
        long lineCovered;

        void add(long bMissed, long bCovered, long lMissed, long lCovered) {
            this.branchMissed += bMissed;
            this.branchCovered += bCovered;
            this.lineMissed += lMissed;
            this.lineCovered += lCovered;
        }
    }
}

