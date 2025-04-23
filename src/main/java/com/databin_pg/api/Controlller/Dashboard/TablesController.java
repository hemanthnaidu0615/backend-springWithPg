package com.databin_pg.api.Controlller.Dashboard;


import com.databin_pg.api.Service.PostgresService; // Assume this is the service used to query the Postgres DB
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:5173")
public class TablesController {

    @Autowired
    private PostgresService postgresService;

    // 📌 Optimized API: Get Recent Orders from PostgreSQL (using stored procedure)
    @GetMapping("/recent-orders")
    public ResponseEntity<?> getRecentOrders(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate) {
        try {
            // Call the stored procedure to get recent orders
            String query = String.format("""
                SELECT * FROM get_recent_orders('%s'::TIMESTAMP, '%s'::TIMESTAMP);
            """, startDate, endDate);

            List<Map<String, Object>> data = postgresService.query(query);  // Assuming query is implemented in your PostgresService class

            if (data.isEmpty()) {
                return ResponseEntity.ok(Collections.singletonMap("message", "No recent orders found."));
            }

            return ResponseEntity.ok(data);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to fetch recent orders"));
        }
    }
}

