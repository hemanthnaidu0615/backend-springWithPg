package com.databin_pg.api.Controlller.Dashboard;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;

@RestController
@RequestMapping("/api/shipment-status")
@CrossOrigin(origins = "http://localhost:5173")
public class ShipmentStatusController {

    @Autowired
    private PostgresService postgresService;

    @GetMapping("/count")
    public ResponseEntity<?> getOrderStatusCount(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "enterpriseKey") String enterpriseKey) {
        try {
            // Formulate the SQL query to call the stored procedure with enterpriseKey
            String query = String.format(""" 
                SELECT * FROM get_shipment_status_counts('%s'::TIMESTAMP, '%s'::TIMESTAMP, '%s'::TEXT)
            """, startDate, endDate, enterpriseKey);

            // Execute the query using the service layer
            List<Map<String, Object>> result = postgresService.query(query);

            // Initialize the status counts map
            Map<String, Integer> statusCounts = new LinkedHashMap<>(Map.of(
                "Delivered", 0,
                "Shipped", 0,
                "Pending", 0,
                "Cancelled", 0,
                "Return Received", 0,
                "Refunded", 0
            ));

            // Process the result set
            int returnReceivedCount = 0;
            for (Map<String, Object> row : result) {
                String status = Objects.toString(row.get("status"), "Unknown");
                int count = parseInteger(row.get("count"));
                statusCounts.put(status, count);
                if ("Return Received".equals(status)) {
                    returnReceivedCount = count;
                }
            }

            // Calculate the refunded status
            statusCounts.put("Refunded", Math.round(returnReceivedCount / 3.0f));

            return ResponseEntity.ok(statusCounts);

        } catch (Exception e) {
            e.printStackTrace();  // Print the stack trace for better debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch shipment status counts"));
        }
    }

    private int parseInteger(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString().trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid integer format: " + obj, e);
        }
    }
}
