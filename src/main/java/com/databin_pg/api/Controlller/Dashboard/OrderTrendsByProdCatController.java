package com.databin_pg.api.Controlller.Dashboard;


import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;

@RestController
@RequestMapping("/api/order-trends-by-category")
@CrossOrigin(origins = "http://localhost:5173")
public class OrderTrendsByProdCatController {

    @Autowired
    private PostgresService postgresService;

    // 📌 API: Get Monthly Order Trends by Product Category (with date filtering)
    @GetMapping
    public ResponseEntity<?> getOrderTrendsByCategory(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate) {
        try {
            // Call the stored procedure for monthly order trends by category
            String query = String.format("""
                SELECT * FROM get_order_trends_by_category('%s'::TIMESTAMP, '%s'::TIMESTAMP)
            """, startDate, endDate);

            // Execute the query using the PostgresService
            List<Map<String, Object>> data = postgresService.query(query);

            // 🧩 Transforming Result into a Nested Map Format
            Map<String, Map<String, Double>> result = new LinkedHashMap<>();

            for (Map<String, Object> row : data) {
                String month = Objects.toString(row.get("month"), "Unknown");
                String category = Objects.toString(row.get("category"), "Unknown");
                double sales = parseDouble(row.get("total_sales"));

                result.computeIfAbsent(month, k -> new LinkedHashMap<>())
                      .put(category, sales);
            }

            return ResponseEntity.ok(Map.of("order_trends", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch order trends data"));
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
