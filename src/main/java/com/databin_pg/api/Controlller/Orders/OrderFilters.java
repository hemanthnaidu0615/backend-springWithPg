package com.databin_pg.api.Controlller.Orders;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;

@RestController
@RequestMapping("/api/order-filters")
@CrossOrigin(origins = "*")
public class OrderFilters {

    @Autowired
    private PostgresService postgresService;

    // 📌 API: Get unique values for order filters (status, type, method, carrier)
    @GetMapping("/filter-values")
    public ResponseEntity<?> getOrderFilterValues() {
        try {
            String query = "SELECT * FROM get_order_filters()";

            List<Map<String, Object>> results = postgresService.query(query);

            // Use sets to get unique values across rows
            Set<String> statuses = new HashSet<>();
            Set<String> types = new HashSet<>();
            Set<String> methods = new HashSet<>();
            Set<String> carriers = new HashSet<>();

            for (Map<String, Object> row : results) {
                if (row.get("status") != null) statuses.add(row.get("status").toString());
                if (row.get("type") != null) types.add(row.get("type").toString());
                if (row.get("method") != null) methods.add(row.get("method").toString());
                if (row.get("carrier") != null) carriers.add(row.get("carrier").toString());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("statuses", statuses);
            response.put("types", types);
            response.put("methods", methods);
            response.put("carriers", carriers);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to fetch filter options"));
        }
    }
}

