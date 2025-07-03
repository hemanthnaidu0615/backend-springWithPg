package com.databin_pg.api.Controlller.Dashboard;

import com.databin_pg.api.Service.PostgresService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;

@RestController
@RequestMapping("/api/order-trends-by-category")
@CrossOrigin(origins = "*")
@Tag(name = "Dashboard- Order Trends by Category", description = "APIs for Order Trends by Category")
public class OrderTrendsByProdCatController {

    @Autowired
    private PostgresService postgresService;

    @Operation(
        summary = "Get order trends by category with pagination, filtering, and sorting",
        description = "Returns monthly sales trends grouped by product category, filtered by date range, with optional enterprise key, search, sorting, and pagination."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved order trends"),
        @ApiResponse(responseCode = "500", description = "Failed to fetch order trends data")
    })
    @GetMapping
    public ResponseEntity<?> getOrderTrendsByCategory(@RequestParam Map<String, String> allParams) {
        try {
            // Required dates
            String startDate = allParams.get("startDate");
            String endDate = allParams.get("endDate");

            if (startDate == null || endDate == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing startDate or endDate"));
            }

            String formattedStartDate = startDate.length() > 10 ? startDate.substring(0, 10) : startDate;
            String formattedEndDate = endDate.length() > 10 ? endDate.substring(0, 10) : endDate;

            String enterpriseKey = allParams.get("enterpriseKey");
            String formattedKey = enterpriseKey == null ? "NULL" : "'" + enterpriseKey.replace("'", "''") + "'";

            // Pagination
            int page = Integer.parseInt(allParams.getOrDefault("page", "0"));
            int size = Integer.parseInt(allParams.getOrDefault("size", "10"));
            int offset = page * size;

            // Sorting
            String sortField = allParams.getOrDefault("sortField", "month");
            String sortOrder = allParams.getOrDefault("sortOrder", "asc");

            Map<String, String> allowedSortFields = Map.of(
                "month", "month",
                "category", "category",
                "sales", "total_sales"
            );
            String sortColumn = allowedSortFields.getOrDefault(sortField, "month");
            String sortDirection = sortOrder.equalsIgnoreCase("desc") ? "DESC" : "ASC";

            // Filtering/search
            List<String> filterableFields = List.of("month", "category", "sales");
            StringBuilder whereClause = new StringBuilder("WHERE 1=1");

            for (String field : filterableFields) {
                String value = allParams.get(field + ".value");
                String matchMode = allParams.getOrDefault(field + ".matchMode", "contains");

                if (value != null && !value.isEmpty()) {
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

            // Count query
            String countQuery = String.format("""
                SELECT COUNT(*) AS total FROM (
                    SELECT * FROM get_order_trends_by_category('%s'::TIMESTAMP, '%s'::TIMESTAMP, %s)
                ) AS result
                %s
            """, formattedStartDate, formattedEndDate, formattedKey, whereClause);

            int totalCount = ((Number) postgresService.query(countQuery).get(0).get("total")).intValue();

            // Data query
            String dataQuery = String.format("""
                SELECT * FROM (
                    SELECT * FROM get_order_trends_by_category('%s'::TIMESTAMP, '%s'::TIMESTAMP, %s)
                ) AS result
                %s
                ORDER BY %s %s
                OFFSET %d LIMIT %d
            """, formattedStartDate, formattedEndDate, formattedKey,
                whereClause, sortColumn, sortDirection, offset, size);

            List<Map<String, Object>> data = postgresService.query(dataQuery);

            // Transform result
            List<Map<String, Object>> trends = new ArrayList<>();
            for (Map<String, Object> row : data) {
                String month = Objects.toString(row.get("month"), "Unknown");
                String category = Objects.toString(row.get("category"), "Unknown");
                double sales = parseDouble(row.get("total_sales"));

                trends.add(Map.of(
                    "month", month,
                    "category", category,
                    "sales", sales
                ));
            }

            return ResponseEntity.ok(Map.of(
                "data", trends,
                "page", page,
                "size", size,
                "count", totalCount
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch order trends data", "details", e.getMessage()));
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
