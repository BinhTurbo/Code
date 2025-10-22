package com.webmini.miniweb.catalog.product.controller;

import com.webmini.miniweb.catalog.product.entity.Product;
import com.webmini.miniweb.catalog.product.repo.ProductRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductExportController {

    private final ProductRepository repo;
    private final ProductPdfExporter pdfExporter;

    @GetMapping("/export")
    public void export(
            @RequestParam String format,
            HttpServletResponse resp
    ) throws Exception {

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String baseName = "products_" + ts;

        if ("csv".equalsIgnoreCase(format)) {
            String filename = baseName + ".csv";
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            resp.setContentType("text/csv");
            resp.setHeader("Content-Disposition", contentDisposition(filename));
            exportCsvStreaming(resp);
            return;
        }

        if ("pdf".equalsIgnoreCase(format)) {
            String filename = baseName + ".pdf";
            resp.setContentType(MediaType.APPLICATION_PDF_VALUE);
            resp.setHeader("Content-Disposition", contentDisposition(filename));
            try (OutputStream os = resp.getOutputStream()) {
                pdfExporter.exportAll(os); // tự xử lý phân trang & render
            }
            return;
        }

        // format sai
        resp.sendError(400, "Invalid format. Use csv or pdf.");
    }

    private String contentDisposition(String filename) {
        String enc = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+","%20");
        return "attachment; filename*=UTF-8''" + enc;
    }

    /** CSV streaming: đọc theo trang để tránh OOM */
    private void exportCsvStreaming(HttpServletResponse resp) throws Exception {
        // BOM để Excel mở UTF-8 không lỗi dấu (tuỳ chọn)
        resp.getOutputStream().write(new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF});

        // header
        String header = String.join(",",
                "ID","SKU","Name","Category","Price","Stock","Status","CreatedAt","UpdatedAt") + "\n";
        resp.getOutputStream().write(header.getBytes(StandardCharsets.UTF_8));

        int page = 0, size = 500;
        Page<Product> slice;
        do {
            slice = repo.findAllWithCategory(PageRequest.of(page, size));
            for (Product p : slice) {
                String line = csv(p.getId()) + "," +
                        csv(p.getSku()) + "," +
                        csv(p.getName()) + "," +
                        csv(p.getCategory()!=null ? p.getCategory().getName() : "") + "," +
                        csv(p.getPrice()) + "," +
                        csv(p.getStock()) + "," +
                        csv(p.getStatus()) + "," +
                        csv(p.getCreatedAt()) + "," +
                        csv(p.getUpdatedAt()) + "\n";
                resp.getOutputStream().write(line.getBytes(StandardCharsets.UTF_8));
            }
            resp.flushBuffer(); // đẩy ra client theo dòng
            page++;
        } while (!slice.isLast());
    }

    /** Escape CSV theo RFC4180 (bao quanh bởi " nếu có dấu phẩy/dấu nháy/newline) */
    private String csv(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v);
        boolean needQuote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        if (s.contains("\"")) s = s.replace("\"", "\"\"");
        return needQuote ? "\"" + s + "\"" : s;
    }
}
