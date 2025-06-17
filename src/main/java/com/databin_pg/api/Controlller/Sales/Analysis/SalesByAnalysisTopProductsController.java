package com.databin_pg.api.Controlller.Sales.Analysis;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;

@RestController
@RequestMapping("/api/analysis")
@CrossOrigin(origins = "*")
public class SalesByAnalysisTopProductsController {

    @Autowired
    private PostgresService postgresService;

    @GetMapping("/product-sales-summary")
    public ResponseEntity<?> getProductSalesSummary(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            int offset = page * size;
            String formattedEnterpriseKey = (enterpriseKey == null || enterpriseKey.isBlank())
                    ? "NULL" : "'" + enterpriseKey + "'";

            // 🔢 Count query
            String countQuery = String.format("""
                SELECT COUNT(*) as total FROM get_product_sales_summary(
                    '%s'::timestamp, '%s'::timestamp, %s
                )
            """, startDate, endDate, formattedEnterpriseKey);

            List<Map<String, Object>> countResult = postgresService.query(countQuery);
            int totalCount = 0;
            if (!countResult.isEmpty() && countResult.get(0).get("total") != null) {
                totalCount = ((Number) countResult.get(0).get("total")).intValue();
            }

            // 📦 Data query with OFFSET + LIMIT
            String dataQuery = String.format("""
                SELECT * FROM get_product_sales_summary(
                    '%s'::timestamp, '%s'::timestamp, %s
                )
                OFFSET %d LIMIT %d
            """, startDate, endDate, formattedEnterpriseKey, offset, size);

            List<Map<String, Object>> result = postgresService.query(dataQuery);

            List<Map<String, Object>> productSales = new ArrayList<>();
            for (Map<String, Object> row : result) {
                Map<String, Object> product = Map.of(
                        "product_name", row.get("product_name"),
                        "units_sold", row.get("units_sold"),
                        "total_sales", row.get("total_sales")
                );
                productSales.add(product);
            }

            return ResponseEntity.ok(Map.of(
                    "products", productSales,
                    "page", page,
                    "size", size,
                    "count", totalCount
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch product sales summary", "details", e.getMessage()));
        }
    }
}
