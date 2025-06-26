package com.databin_pg.api.Controlller.Dashboard;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping("/api/shipment-status")
@CrossOrigin(origins = "*")
@Tag(name = "Dashboard - Shipment Status", description = "API to retrieve counts of shipment statuses like Shipped, Cancelled, and Returned")
public class ShipmentStatusController {

    @Autowired
    private PostgresService postgresService;

    @Operation(
            summary = "Get shipment status counts",
            description = "Retrieves the total number of orders in different shipment statuses (Shipped, Cancelled, Returned) and shipped percentage over a date range."
        )
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shipment status counts fetched successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to fetch shipment status counts")
        })
    @GetMapping("/count")
    public ResponseEntity<?> getOrderStatusCount(
    		@Parameter(description = "Start date in YYYY-MM-DD format", required = true)
            @RequestParam(name = "startDate") String startDate,

            @Parameter(description = "End date in YYYY-MM-DD format", required = true)
            @RequestParam(name = "endDate") String endDate,

            @Parameter(description = "Optional enterprise key for filtering results 'AWW' or 'AWD'")
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey) {
        try {
            String query = String.format("""
                    SELECT * FROM get_shipment_status_counts(
                        '%s'::TIMESTAMP,
                        '%s'::TIMESTAMP,
                        %s
                    )
                    """, startDate, endDate,
                    enterpriseKey == null ? "NULL" : "'" + enterpriseKey + "'");

            List<Map<String, Object>> result = postgresService.query(query);

            Map<String, Object> response = new LinkedHashMap<>();
            Map<String, Integer> statusCounts = new LinkedHashMap<>(Map.of(
                    "Shipped", 0,
                    "Cancelled", 0,
                    "Returned", 0));
            int shippedPercentage = 0;

            for (Map<String, Object> row : result) {
                String status = Objects.toString(row.get("status"), "Unknown");
                int count = parseInteger(row.get("count"));

                if ("Shipped".equals(status) || "Cancelled".equals(status) || "Returned".equals(status)) {
                    statusCounts.put(status, count);
                } else if ("ShippedPercentage".equals(status)) {
                    shippedPercentage = count;
                }
            }

            response.putAll(statusCounts);
            response.put("ShippedPercentage", shippedPercentage);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch shipment status counts"));
        }
    }

    private int parseInteger(Object obj) {
        if (obj == null)
            return 0;
        if (obj instanceof Number)
            return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString().trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid integer format: " + obj, e);
        }
    }
}
