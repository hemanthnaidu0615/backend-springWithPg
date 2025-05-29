package com.databin_pg.api.Controlller.Sales.Analysis;


import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/analysis")
@CrossOrigin(origins = "http://localhost:5173")

public class SalesByAnalysisSalesTrendsController {

    @Autowired
    private PostgresService postgresService;

    @GetMapping("/sales-by-date")
    public ResponseEntity<?> getSalesByDate(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey,
            @RequestParam(name = "fulfillmentChannel", required = false) String fulfillmentChannel
    ) {
        try {
            // Parse dates
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            long daysBetween = ChronoUnit.DAYS.between(start, end);

            // Determine aggregation level
            String aggregationLevel;
            if (daysBetween < 7) {
                aggregationLevel = "day";
            } else if (daysBetween < 30) {
                aggregationLevel = "week";
            } else if (daysBetween < 365) {
                aggregationLevel = "month";
            } else {
                aggregationLevel = "year";
            }


            // Format parameters
            String formattedEnterpriseKey = (enterpriseKey == null || enterpriseKey.isBlank()) ? "NULL" : "'" + enterpriseKey + "'";
            String formattedFulfillmentChannel = (fulfillmentChannel == null || fulfillmentChannel.isBlank()) ? "NULL" : "'" + fulfillmentChannel + "'";

            // Build SQL query
            String query = String.format("""
                SELECT * FROM get_sales_by_date('%s'::timestamp, '%s'::timestamp, %s, %s, '%s')
            """, startDate, endDate, formattedEnterpriseKey, formattedFulfillmentChannel, aggregationLevel);

            // Execute
            List<Map<String, Object>> result = postgresService.query(query);

            if (result.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(Map.of("message", "No sales data found for the given period."));
            }

            // Transform
            List<Map<String, Object>> salesData = new ArrayList<>();
            for (Map<String, Object> row : result) {
                salesData.add(Map.of(
                        "period", row.get("period"),
                        "total_amount", row.get("total_amount")
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "aggregation_level", aggregationLevel,
                    "sales", salesData
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch sales data", "details", e.getMessage()));
        }
    }
}