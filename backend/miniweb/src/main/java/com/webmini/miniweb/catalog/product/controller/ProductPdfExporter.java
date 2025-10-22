package com.webmini.miniweb.catalog.product.controller;

import com.webmini.miniweb.catalog.product.entity.Product;
import com.webmini.miniweb.catalog.product.repo.ProductRepository;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@RequiredArgsConstructor
public class ProductPdfExporter {

    private final ProductRepository repo;

    public void exportAll(OutputStream os) throws Exception {
        // 1) Gom data (đọc theo trang)
        java.util.List<ProductRow> rows = new java.util.ArrayList<>();
        int page = 0, size = 1000;
        Page<Product> slice;
        do {
            slice = repo.findAllWithCategory(PageRequest.of(page, size));
            for (Product p : slice) {
                rows.add(ProductRow.of(p));
            }
            page++;
        } while (!slice.isLast());

        // 2) Nạp & compile jrxml
        try (InputStream in = new ClassPathResource("reports/products_report.jrxml").getInputStream()) {
            JasperReport rpt = JasperCompileManager.compileReport(in);

            // 3) Tham số (logo, tiêu đề, ngày …)
            Map<String, Object> params = new HashMap<>();
            params.put("REPORT_TITLE", "Product List");
            params.put("REPORT_DATE", java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            // Logo (tuỳ chọn): đặt ở resources/static/logo.png
            try {
                params.put("LOGO", new ClassPathResource("static/logo.png").getInputStream());
            } catch (Exception ignore) { params.put("LOGO", null); }

            // 4) Fill & export
            JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(rows);
            JasperPrint print = JasperFillManager.fillReport(rpt, params, ds);
            JasperExportManager.exportReportToPdfStream(print, os);
        }
    }

    /** Row DTO gọn cho Jasper */
    public static class ProductRow {
        private Long id;
        private String sku;
        private String name;
        private String category;
        private BigDecimal price;
        private Integer stock;
        private String status;
        private String createdAt;

        public static ProductRow of(Product p) {
            ProductRow r = new ProductRow();
            r.id = p.getId();
            r.sku = p.getSku();
            r.name = p.getName();
            r.category = p.getCategory() != null ? p.getCategory().getName() : "";
            r.price = p.getPrice();
            r.stock = p.getStock();
            r.status = String.valueOf(p.getStatus());
            r.createdAt = p.getCreatedAt() != null ? p.getCreatedAt().toString() : "";
            return r;
        }

        // getters cho Jasper
        public Long getId() { return id; }
        public String getSku() { return sku; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public BigDecimal getPrice() { return price; }
        public Integer getStock() { return stock; }
        public String getStatus() { return status; }
        public String getCreatedAt() { return createdAt; }
    }
}
