package com.benchmark.report;

import com.benchmark.core.BenchmarkResult;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Pure Java2D/AWT chart generator — zero external dependencies.
 *
 * <p>Produces five PNG charts per benchmark session:
 * <ul>
 *   <li>Throughput (ops/s) vs Concurrency</li>
 *   <li>Average Latency (ms) vs Concurrency</li>
 *   <li>P99 Latency (ms) vs Concurrency</li>
 *   <li>CPU Utilisation (%) vs Concurrency</li>
 *   <li>Peak Memory (MB) vs Concurrency</li>
 * </ul>
 * </p>
 */
public final class ChartGenerator {

    // ── Canvas dimensions ─────────────────────────────────────────────────────
    private static final int W     = 920;
    private static final int H     = 530;
    private static final int PAD_L = 85;
    private static final int PAD_R = 210;
    private static final int PAD_T = 64;
    private static final int PAD_B = 62;

    // ── Colour palette ────────────────────────────────────────────────────────
    private static final Color BG_OUTER  = new Color(0xF0F2F5);
    private static final Color BG_PLOT   = Color.WHITE;
    private static final Color GRID      = new Color(0xE2E8F0);
    private static final Color AXIS_CLR  = new Color(0x64748B);
    private static final Color TITLE_CLR = new Color(0x0F3460);

    private static final Color[] PALETTE = {
        new Color(0x4E79A7), new Color(0xE15759), new Color(0x59A14F),
        new Color(0xF28E2B), new Color(0x76B7B2), new Color(0xB07AA1),
        new Color(0xEDC948), new Color(0xFF9DA7), new Color(0x9C755F)
    };

    private final String outputDir;

    public ChartGenerator(String outputDir) {
        this.outputDir = outputDir;
        new File(outputDir).mkdirs();
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Generate all standard charts and return label → file-path mappings.
     */
    public Map<String, String> generateAll(Map<String, List<BenchmarkResult>> data)
            throws IOException {

        Map<String, String> out = new LinkedHashMap<>();
        out.put("Throughput (ops/s)",
                render(data, "Throughput vs Concurrency",
                       "Concurrency (threads)", "Throughput (ops/s)",
                       r -> r.getThroughput(), "chart_throughput.png"));

        out.put("Avg Latency (ms)",
                render(data, "Average Latency vs Concurrency",
                       "Concurrency (threads)", "Avg Latency (ms)",
                       r -> r.getAvgLatencyMs(), "chart_latency_avg.png"));

        out.put("P99 Latency (ms)",
                render(data, "P99 Latency vs Concurrency",
                       "Concurrency (threads)", "P99 Latency (ms)",
                       r -> r.getP99LatencyMs(), "chart_latency_p99.png"));

        out.put("CPU Utilisation (%)",
                render(data, "CPU Utilisation vs Concurrency",
                       "Concurrency (threads)", "Avg CPU (%)",
                       r -> r.getAvgCpuPercent(), "chart_cpu.png"));

        out.put("Peak Memory (MB)",
                render(data, "Peak Memory vs Concurrency",
                       "Concurrency (threads)", "Peak Memory (MB)",
                       r -> r.getPeakMemoryMB(), "chart_memory.png"));

        return out;
    }

    // ── Core renderer ──────────────────────────────────────────────────────────

    @FunctionalInterface
    public interface MetricExtractor { double extract(BenchmarkResult r); }

    private String render(Map<String, List<BenchmarkResult>> data,
                          String title, String xLabel, String yLabel,
                          MetricExtractor ex, String filename) throws IOException {

        // ── Determine axis ranges ────────────────────────────────────────
        double xMin = Double.MAX_VALUE, xMax = -Double.MAX_VALUE;
        double yMax = -Double.MAX_VALUE;
        for (List<BenchmarkResult> series : data.values()) {
            for (BenchmarkResult r : series) {
                double x = r.getConcurrencyLevel(), y = ex.extract(r);
                if (x < xMin) xMin = x;
                if (x > xMax) xMax = x;
                if (y > yMax) yMax = y;
            }
        }
        yMax = niceMax(yMax);
        if (xMax <= xMin) xMax = xMin + 1;

        // ── Canvas setup ─────────────────────────────────────────────────
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        applyHints(g);

        // Outer background with rounded corners
        g.setColor(BG_OUTER);
        g.fillRoundRect(0, 0, W, H, 18, 18);

        // Plot area
        int px = PAD_L, py = PAD_T, pw = W - PAD_L - PAD_R, ph = H - PAD_T - PAD_B;
        g.setColor(BG_PLOT);
        g.fillRect(px, py, pw, ph);

        // Drop-shadow effect (subtle)
        for (int s = 3; s >= 1; s--) {
            g.setColor(new Color(0, 0, 0, 12 / s));
            g.drawRect(px - s, py + s, pw + s, ph + s);
        }

        // Plot border
        g.setColor(new Color(0xDDE3EE));
        g.setStroke(new BasicStroke(1f));
        g.drawRect(px, py, pw, ph);

        // ── Horizontal grid lines + Y-axis labels ────────────────────────
        int yTicks = 6;
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        FontMetrics fmSmall = g.getFontMetrics();

        for (int t = 0; t <= yTicks; t++) {
            double fraction = (double) t / yTicks;
            int gy  = py + ph - (int) (ph * fraction);
            double val = yMax * fraction;

            // Grid line
            g.setColor(GRID);
            g.setStroke(dashed(1f));
            g.drawLine(px + 1, gy, px + pw - 1, gy);
            g.setStroke(solid(1f));

            // Y label
            String lbl = formatNum(val);
            g.setColor(AXIS_CLR);
            g.drawString(lbl, px - fmSmall.stringWidth(lbl) - 7,
                         gy + fmSmall.getAscent() / 2);
        }

        // ── X-axis labels ────────────────────────────────────────────────
        TreeSet<Double> xVals = new TreeSet<>();
        for (List<BenchmarkResult> s : data.values())
            for (BenchmarkResult r : s) xVals.add((double) r.getConcurrencyLevel());

        for (double xv : xVals) {
            int gx  = px + toPixX(xv, xMin, xMax, pw);
            String lbl = String.valueOf((int) xv);

            // Tick mark
            g.setColor(AXIS_CLR);
            g.setStroke(solid(1f));
            g.drawLine(gx, py + ph, gx, py + ph + 5);

            // Label
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            fmSmall = g.getFontMetrics();
            g.drawString(lbl, gx - fmSmall.stringWidth(lbl) / 2, py + ph + 18);
        }

        // ── Axis titles ──────────────────────────────────────────────────
        // X title
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(TITLE_CLR);
        FontMetrics fmBold = g.getFontMetrics();
        g.drawString(xLabel, px + (pw - fmBold.stringWidth(xLabel)) / 2, H - 10);

        // Y title (rotated)
        AffineTransform saved = g.getTransform();
        g.rotate(-Math.PI / 2, 14, py + ph / 2.0);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        fmBold = g.getFontMetrics();
        g.setColor(TITLE_CLR);
        g.drawString(yLabel, 14 - fmBold.stringWidth(yLabel) / 2, py + ph / 2 + fmBold.getAscent() / 2);
        g.setTransform(saved);

        // ── Chart title ──────────────────────────────────────────────────
        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        g.setColor(TITLE_CLR);
        FontMetrics fmTitle = g.getFontMetrics();
        g.drawString(title, (W - PAD_R - fmTitle.stringWidth(title)) / 2, 40);

        // ── Series: lines + markers ──────────────────────────────────────
        List<String> legendLabels = new ArrayList<>();
        List<Color>  legendColors = new ArrayList<>();
        int ci = 0;

        for (Map.Entry<String, List<BenchmarkResult>> entry : data.entrySet()) {
            Color c = PALETTE[ci % PALETTE.length];
            legendLabels.add(entry.getKey());
            legendColors.add(c);

            List<BenchmarkResult> series = entry.getValue();
            int[] sxs = new int[series.size()];
            int[] sys = new int[series.size()];
            for (int i = 0; i < series.size(); i++) {
                BenchmarkResult r = series.get(i);
                sxs[i] = px + toPixX(r.getConcurrencyLevel(), xMin, xMax, pw);
                sys[i] = py + ph - toPixY(ex.extract(r), yMax, ph);
            }

            // Line
            g.setColor(c);
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i + 1 < sxs.length; i++)
                g.drawLine(sxs[i], sys[i], sxs[i + 1], sys[i + 1]);

            // Markers
            for (int i = 0; i < sxs.length; i++) {
                // White fill
                g.setColor(Color.WHITE);
                g.fillOval(sxs[i] - 5, sys[i] - 5, 10, 10);
                // Coloured ring
                g.setColor(c);
                g.setStroke(new BasicStroke(2.2f));
                g.drawOval(sxs[i] - 5, sys[i] - 5, 10, 10);
                // Coloured centre
                g.fillOval(sxs[i] - 3, sys[i] - 3, 6, 6);
            }
            ci++;
        }

        // ── Legend ───────────────────────────────────────────────────────
        int legendX = px + pw + 18;
        int legendY = py + 16;
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        FontMetrics fmLeg = g.getFontMetrics();

        for (int i = 0; i < legendLabels.size(); i++) {
            Color c   = legendColors.get(i);
            String lbl = legendLabels.get(i);
            int ly = legendY + i * 38;

            // Coloured swatch line
            g.setColor(c);
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(legendX, ly + 8, legendX + 20, ly + 8);
            g.fillOval(legendX + 7, ly + 4, 8, 8);

            // Label text (word-wrap at ~22 chars wide)
            g.setColor(new Color(0x1A1A2E));
            int maxW = PAD_R - 36;
            drawWrapped(g, lbl, legendX + 26, ly, maxW, fmLeg);
        }

        g.dispose();

        // ── Write PNG ─────────────────────────────────────────────────────
        File outFile = new File(outputDir, filename);
        ImageIO.write(img, "PNG", outFile);
        return outFile.getAbsolutePath();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static void applyHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,    RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,     RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    private static int toPixX(double x, double xMin, double xMax, int pw) {
        return (int) ((x - xMin) / (xMax - xMin) * pw);
    }

    private static int toPixY(double y, double yMax, int ph) {
        return (int) (y / yMax * ph);
    }

    private static double niceMax(double raw) {
        if (raw <= 0) return 1;
        double exp  = Math.pow(10, Math.floor(Math.log10(raw)));
        double frac = raw / exp;
        double nice = frac <= 1 ? 1 : frac <= 2 ? 2 : frac <= 5 ? 5 : 10;
        return nice * exp * 1.1;
    }

    private static String formatNum(double v) {
        if (v == 0)         return "0";
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000);
        if (v >= 1_000)     return String.format("%.1fK", v / 1_000);
        if (v < 10)         return String.format("%.2f",  v);
        if (v < 100)        return String.format("%.1f",  v);
        return String.format("%.0f", v);
    }

    private static void drawWrapped(Graphics2D g, String text,
                                    int x, int y, int maxWidth, FontMetrics fm) {
        if (fm.stringWidth(text) <= maxWidth) {
            g.drawString(text, x, y + fm.getAscent());
            return;
        }
        int mid   = text.length() / 2;
        int space = text.lastIndexOf(' ', mid);
        if (space < 0) space = text.indexOf(' ', mid);
        if (space < 0) {
            g.drawString(text.substring(0, 20) + "…", x, y + fm.getAscent());
            return;
        }
        g.drawString(text.substring(0, space),   x, y + fm.getAscent());
        g.drawString(text.substring(space + 1),  x, y + fm.getAscent() + fm.getHeight() + 1);
    }

    private static Stroke solid(float w) {
        return new BasicStroke(w);
    }

    private static Stroke dashed(float w) {
        return new BasicStroke(w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                               0, new float[]{4, 4}, 0);
    }
}
