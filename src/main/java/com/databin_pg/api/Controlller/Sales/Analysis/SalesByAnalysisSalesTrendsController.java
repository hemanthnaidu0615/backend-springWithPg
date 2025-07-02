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
@CrossOrigin(origins = "*")

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
    
    @GetMapping("/details-sales-trends-grid")
    public ResponseEntity<?> getSalesTrendsGrid(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey,
            @RequestParam(name = "fulfillmentChannel", required = false) String fulfillmentChannel,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortField", defaultValue = "order_id") String sortField,
            @RequestParam(name = "sortOrder", defaultValue = "asc") String sortOrder,
            @RequestParam Map<String, String> allParams
    ) {
        try {
            if (page < 0) page = 0;
            if (size <= 0) size = 20;

            Set<String> allowedFields = Set.of(
                    "order_id", "subtotal", "shipping_fee", "tax_amount",
                    "discount_amount", "total_amount", "fulfillment_channel"
            );

            if (!allowedFields.contains(sortField)) {
                sortField = "order_id";  // fallback to safe default
            }

            String sortDirection = sortOrder.equalsIgnoreCase("desc") ? "DESC" : "ASC";
            int offset = page * size;

            String formattedEnterpriseKey = (enterpriseKey == null || enterpriseKey.isBlank()) ? "NULL" : "'" + enterpriseKey.replace("'", "''") + "'";
            String formattedFulfillmentChannel = (fulfillmentChannel == null || fulfillmentChannel.isBlank()) ? "NULL" : "'" + fulfillmentChannel.replace("'", "''") + "'";

            StringBuilder whereClause = new StringBuilder("WHERE 1=1");

            for (String field : allowedFields) {
                String value = allParams.get(field + ".value");
                String matchMode = allParams.getOrDefault(field + ".matchMode", "contains");

                if (value != null && !value.isBlank()) {
                    value = value.toLowerCase().replace("'", "''");
                    boolean isNumeric = List.of("subtotal", "shipping_fee", "tax_amount", "discount_amount", "total_amount").contains(field);
                    String columnRef = "sub." + field;

                    String condition = switch (matchMode) {
                        case "startsWith" -> "%s::text LIKE '%s%%'".formatted(columnRef, value);
                        case "endsWith" -> "%s::text LIKE '%%%s'".formatted(columnRef, value);
                        case "notContains" -> "LOWER(%s::text) NOT LIKE '%%%s%%'".formatted(columnRef, value);
                        case "equals" -> isNumeric
                                ? "%s = %s".formatted(columnRef, value)
                                : "LOWER(%s::text) = '%s'".formatted(columnRef, value);
                        case "greaterThan" -> isNumeric ? "%s > %s".formatted(columnRef, value) : "1=0";
                        case "lessThan" -> isNumeric ? "%s < %s".formatted(columnRef, value) : "1=0";
                        default -> "LOWER(%s::text) LIKE '%%%s%%'".formatted(columnRef, value); // contains
                    };

                    whereClause.append(" AND ").append(condition);
                }
            }

            // Add static filters
            if (!formattedEnterpriseKey.equals("NULL")) {
                whereClause.append(" AND sub.enterprise_key = ").append(formattedEnterpriseKey);
            }
            if (!formattedFulfillmentChannel.equals("NULL")) {
                whereClause.append(" AND LOWER(sub.fulfillment_channel) = LOWER(").append(formattedFulfillmentChannel).append(")");
            }

            String baseQuery = String.format("""
                SELECT * FROM get_sales_trends_grid(
                    '%s'::timestamp,
                    '%s'::timestamp,
                    %s,
                    %s
                )
            """, startDate, endDate, formattedEnterpriseKey, formattedFulfillmentChannel);

            String countQuery = String.format("""
                SELECT COUNT(*) AS total FROM (
                    %s
                ) AS sub
                %s
            """, baseQuery, whereClause);

            List<Map<String, Object>> countResult = postgresService.query(countQuery);
            int totalRecords = (!countResult.isEmpty() && countResult.get(0).get("total") != null)
                    ? ((Number) countResult.get(0).get("total")).intValue()
                    : 0;

            if (totalRecords == 0) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(Map.of("message", "No sales data found for the given period."));
            }

            String dataQuery = String.format("""
                SELECT * FROM (
                    %s
                ) AS sub
                %s
                ORDER BY %s %s
                LIMIT %d OFFSET %d
            """, baseQuery, whereClause, sortField, sortDirection, size, offset);

            List<Map<String, Object>> data = postgresService.query(dataQuery);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("totalRecords", totalRecords);
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", (int) Math.ceil((double) totalRecords / size));
            response.put("data", data);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch sales trends grid data", "details", e.getMessage()));
        }
    }
}