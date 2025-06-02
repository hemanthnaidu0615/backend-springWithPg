package com.databin_pg.api.Controlller.Dashboard;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;

@RestController
@RequestMapping("/api/order-trends-by-category")
@CrossOrigin(origins = "*")
public class OrderTrendsByProdCatController {

    @Autowired
    private PostgresService postgresService;

    @GetMapping
    public ResponseEntity<?> getOrderTrendsByCategory(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "enterpriseKey", required=false) String enterpriseKey) {
        try {
            // Ensure startDate and endDate are formatted properly
            String formattedStartDate = startDate.length() > 10 ? startDate.substring(0, 10) : startDate;
            String formattedEndDate = endDate.length() > 10 ? endDate.substring(0, 10) : endDate;

            // Construct the query, handling NULL for enterpriseKey
            String query = String.format("""
                SELECT * FROM get_order_trends_by_category('%s'::TIMESTAMP, '%s'::TIMESTAMP, %s)
            """, formattedStartDate, formattedEndDate,
                    enterpriseKey == null ? "NULL" : String.format("'%s'", enterpriseKey));

            // Execute the query
            List<Map<String, Object>> data = postgresService.query(query);

            // 🧩 Transform the result into a nested map format
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
            e.printStackTrace(); // Log the exception for debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch order trends data", "details", e.getMessage()));
        }
    }

    // Helper Method: Convert Object to Double
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
