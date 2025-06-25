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
@RequestMapping("/api/revenue")
@CrossOrigin(origins = "*")
@Tag(name = "Dashboard - Revenue Per Customer", description = "APIs for Revenue Analytics by Customer")
public class RevenuePerCustomerController {

    @Autowired
    private PostgresService postgresService;

    @GetMapping("/top-customers")
    @Operation(
        summary = "Get Top Customers by Revenue with pagination and field-level filtering",
        description = "Fetches customers based on total revenue within the specified date range, supports pagination, sorting, and dynamic match-mode filtering on customer_id and customer_name."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved top customers"),
        @ApiResponse(responseCode = "500", description = "Failed to fetch top customers by revenue")
    })
    public ResponseEntity<?> getTopCustomersByRevenue(
        @RequestParam Map<String, String> allParams
    ) {
        try {
            String startDate = allParams.get("startDate");
            String endDate = allParams.get("endDate");

            if (startDate == null || endDate == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing startDate or endDate"));
            }

            String formattedStartDate = startDate.length() > 10 ? startDate.substring(0, 10) : startDate;
            String formattedEndDate = endDate.length() > 10 ? endDate.substring(0, 10) : endDate;

            String enterpriseKey = allParams.get("enterpriseKey");
            String formattedKey = enterpriseKey == null ? "NULL" : "'" + enterpriseKey.replace("'", "''") + "'";

            int page = Integer.parseInt(allParams.getOrDefault("page", "0"));
            int size = Integer.parseInt(allParams.getOrDefault("size", "10"));
            int offset = page * size;

            String sortField = allParams.getOrDefault("sortField", "revenue");
            String sortOrder = allParams.getOrDefault("sortOrder", "desc");

            Map<String, String> sortFields = Map.of(
                "customer_id", "customer_id",
                "customer_name", "customer_name",
                "revenue", "total_revenue"
            );
            String sortColumn = sortFields.getOrDefault(sortField, "total_revenue");
            String sortDirection = sortOrder.equalsIgnoreCase("asc") ? "ASC" : "DESC";

            // Allowed filterable fields
            List<String> allowedFields = List.of("customer_id", "customer_name");

            // Build WHERE clause with dynamic filtering
            StringBuilder whereClause = new StringBuilder("WHERE 1=1");
            for (String field : allowedFields) {
                String value = allParams.get(field + ".value");
                String matchMode = allParams.getOrDefault(field + ".matchMode", "contains");

                if (value != null && !value.trim().isEmpty()) {
                    value = value.toLowerCase().replace("'", "''");
                    String condition;
                    switch (matchMode) {
                        case "startsWith" -> condition = "LOWER(%s::text) LIKE '%s%%'".formatted(field, value);
                        case "endsWith" -> condition = "LOWER(%s::text) LIKE '%%%s'".formatted(field, value);
                        case "notContains" -> condition = "LOWER(%s::text) NOT LIKE '%%%s%%'".formatted(field, value);
                        case "equals" -> condition = "LOWER(%s::text) = '%s'".formatted(field, value);
                        default -> condition = "LOWER(%s::text) LIKE '%%%s%%'".formatted(field, value);
                    }
                    whereClause.append(" AND ").append(condition);
                }
            }

            // Count Query
            String countQuery = String.format("""
                SELECT COUNT(*) AS total FROM (
                    SELECT * FROM get_top_customers_by_revenue('%s'::TIMESTAMP, '%s'::TIMESTAMP, %s)
                ) AS result
                %s
            """, formattedStartDate, formattedEndDate, formattedKey, whereClause);

            int totalCount = ((Number) postgresService.query(countQuery).get(0).get("total")).intValue();

            // Data Query
            String dataQuery = String.format("""
                SELECT * FROM (
                    SELECT * FROM get_top_customers_by_revenue('%s'::TIMESTAMP, '%s'::TIMESTAMP, %s)
                ) AS result
                %s
                ORDER BY %s %s
                OFFSET %d LIMIT %d
            """, formattedStartDate, formattedEndDate, formattedKey,
                    whereClause, sortColumn, sortDirection, offset, size);

            List<Map<String, Object>> data = postgresService.query(dataQuery);
            List<Map<String, Object>> topCustomers = new ArrayList<>();

            for (Map<String, Object> row : data) {
                topCustomers.add(Map.of(
                    "customer_id", Objects.toString(row.get("customer_id"), "N/A"),
                    "customer_name", Objects.toString(row.get("customer_name"), "N/A"),
                    "revenue", parseDouble(row.get("total_revenue"))
                ));
            }

            return ResponseEntity.ok(Map.of(
                "data", topCustomers,
                "page", page,
                "size", size,
                "count", totalCount
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch top customers by revenue", "details", e.getMessage()));
        }
    }

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
