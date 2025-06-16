package com.databin_pg.api.Controlller.Fulfillment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.databin_pg.api.Service.PostgresService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fulfillment")
@CrossOrigin(origins = "*")
public class FulfillmentPerformanceController {

    @Autowired
    private PostgresService postgresService;

    // 📌 API: Get Fulfillment Center Performance Summary (with pagination)
    @GetMapping("/fulfillment-performance")
    public ResponseEntity<?> getFulfillmentCenterPerformance(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            String formattedKey = (enterpriseKey == null || enterpriseKey.isBlank())
                    ? "NULL"
                    : "'" + enterpriseKey + "'";
            int offset = page * size;

            // Count query
            String countQuery = String.format("""
                SELECT COUNT(*) as total FROM get_fulfillment_center_performance('%s', '%s', %s)
            """, startDate, endDate, formattedKey);

            List<Map<String, Object>> countResult = postgresService.query(countQuery);
            int totalCount = 0;
            if (!countResult.isEmpty() && countResult.get(0).get("total") != null) {
                totalCount = ((Number) countResult.get(0).get("total")).intValue();
            }

            // Data query with pagination
            String dataQuery = String.format("""
                SELECT * FROM get_fulfillment_center_performance('%s', '%s', %s)
                OFFSET %d LIMIT %d
            """, startDate, endDate, formattedKey, offset, size);

            List<Map<String, Object>> data = postgresService.query(dataQuery);

            return ResponseEntity.ok(Map.of(
                    "data", data,
                    "page", page,
                    "size", size,
                    "count", totalCount
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to fetch fulfillment center performance",
                            "details", e.getMessage()));
        }
    }
}
