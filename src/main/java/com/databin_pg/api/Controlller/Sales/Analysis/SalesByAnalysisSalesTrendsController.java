package com.databin_pg.api.Controlller.Sales.Analysis;


import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

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
            // Format nullable string parameters for SQL
            String formattedEnterpriseKey = (enterpriseKey == null || enterpriseKey.isBlank()) ? "NULL" : "'" + enterpriseKey + "'";
            String formattedFulfillmentChannel = (fulfillmentChannel == null || fulfillmentChannel.isBlank()) ? "NULL" : "'" + fulfillmentChannel + "'";

            // Build the query with all parameters
            String query = String.format("""
                SELECT * FROM get_sales_by_date('%s'::timestamp, '%s'::timestamp, %s, %s)
            """, startDate, endDate, formattedEnterpriseKey, formattedFulfillmentChannel);

            // Execute the query
            List<Map<String, Object>> result = postgresService.query(query);

            if (result.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(Map.of("message", "No sales data found for the given period."));
            }

            // Process results
            List<Map<String, Object>> salesData = new ArrayList<>();
            for (Map<String, Object> row : result) {
                Map<String, Object> sale = Map.of(
                        "order_date", row.get("order_date"),
                        "total_amount", row.get("total_amount")
                );
                salesData.add(sale);
            }

            return ResponseEntity.ok(Map.of("sales", salesData));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch sales data", "details", e.getMessage()));
        }
    }
}
