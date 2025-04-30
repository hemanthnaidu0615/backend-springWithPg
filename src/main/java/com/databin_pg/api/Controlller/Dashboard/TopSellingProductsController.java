package com.databin_pg.api.Controlller.Dashboard;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;

@RestController
@RequestMapping("/api/top-sellers")
@CrossOrigin(origins = "http://localhost:5173")
public class TopSellingProductsController {

    @Autowired
    private PostgresService postgresService;

    // ✅ API: Get Top 5 Selling Products using PostgreSQL stored procedure
    @GetMapping("/top-products")
    public ResponseEntity<?> getTopSellingProducts(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "enterpriseKey", required=false) String enterpriseKey) {  // Accept enterpriseKey as a parameter

        try {
            // Include enterpriseKey in the query
            String query = String.format(
                "SELECT * FROM get_top_selling_products(TIMESTAMP '%s', TIMESTAMP '%s', '%s')", 
                startDate, endDate, enterpriseKey
            );

            List<Map<String, Object>> data = postgresService.query(query);
            List<Map<String, Object>> topProducts = new ArrayList<>();

            for (Map<String, Object> row : data) {
                String name = Objects.toString(row.get("product_name"), "N/A");
                int quantity = parseInteger(row.get("total_quantity"));
                double percent = parseDouble(row.get("percentage"));
                String description = Objects.toString(row.get("description"), "N/A");
                double price = parseDouble(row.get("price"));

                topProducts.add(Map.of(
                    "product_name", name,
                    "quantity_sold", quantity,
                    "percentage", String.format("%.2f%%", percent),
                    "description", description,
                    "price", price
                ));
            }

            return ResponseEntity.ok(Map.of("top_products", topProducts));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch top selling products"));
        }
    }

    // 🔹 Helper Method: Convert Object to Integer
    private int parseInteger(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid integer format: " + obj, e);
        }
    }

    // 🔹 Helper Method: Convert Object to Double
    private double parseDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(obj.toString());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid double format: " + obj, e);
        }
    }
}
