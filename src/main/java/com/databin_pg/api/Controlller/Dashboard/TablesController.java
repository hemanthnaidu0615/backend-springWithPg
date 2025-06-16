package com.databin_pg.api.Controlller.Dashboard;


import com.databin_pg.api.Service.PostgresService; // Assume this is the service used to query the Postgres DB
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class TablesController {

    @Autowired
    private PostgresService postgresService;

    // 📌 Optimized API: Get Recent Orders from PostgreSQL (using stored procedure)
    @GetMapping("/recent-orders")
    public ResponseEntity<?> getRecentOrders(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            String formattedKey = (enterpriseKey == null || enterpriseKey.isBlank()) ? "NULL" : "'" + enterpriseKey + "'";
            int offset = page * size;

            // Count query
            String countQuery = String.format("""
                SELECT COUNT(*) as total FROM get_recent_orders('%s'::TIMESTAMP, '%s'::TIMESTAMP, %s)
            """, startDate, endDate, formattedKey);

            List<Map<String, Object>> countResult = postgresService.query(countQuery);
            int totalCount = 0;
            if (!countResult.isEmpty() && countResult.get(0).get("total") != null) {
                totalCount = ((Number) countResult.get(0).get("total")).intValue();
            }

            // Paginated data query
            String dataQuery = String.format("""
                SELECT * FROM get_recent_orders('%s'::TIMESTAMP, '%s'::TIMESTAMP, %s)
                OFFSET %d LIMIT %d
            """, startDate, endDate, formattedKey, offset, size);

            List<Map<String, Object>> data = postgresService.query(dataQuery);

            return ResponseEntity.ok(Map.of(
                    "data", data,
                    "page", page,
                    "size", size,
                    "count", totalCount));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch recent orders", "details", e.getMessage()));
        }
    }

}

