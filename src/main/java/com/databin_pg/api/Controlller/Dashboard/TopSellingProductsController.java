package com.databin_pg.api.Controlller.Dashboard;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/top-sellers")
@CrossOrigin(origins = "*")
@Tag(name = "Dashboard - Top Selling Products", description = "APIs for retrieving top selling products by quantity")
public class TopSellingProductsController {

    @Autowired
    private PostgresService postgresService;

    @GetMapping("/top-products")
    @Operation(summary = "Get top selling products with pagination, sorting, and filtering", description = "Retrieves the top selling products within a date range. Supports filtering by fields using match modes, sorting, and pagination.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Top selling products fetched successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to fetch top selling products")
    })
    public ResponseEntity<?> getTopSellingProducts(@RequestParam Map<String, String> allParams) {
        try {
            String startDate = allParams.get("startDate");
            String endDate = allParams.get("endDate");

            if (startDate == null || endDate == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing startDate or endDate"));
            }

            String enterpriseKey = allParams.get("enterpriseKey");
            String formattedKey = enterpriseKey == null ? "NULL" : "'" + enterpriseKey.replace("'", "''") + "'";

            int page = Integer.parseInt(allParams.getOrDefault("page", "0"));
            int size = Integer.parseInt(allParams.getOrDefault("size", "10"));
            int offset = page * size;

            String sortField = allParams.getOrDefault("sortField", "quantity");
            String sortOrder = allParams.getOrDefault("sortOrder", "desc");

            Map<String, String> fieldColumnMap = Map.of(
                    "product_name", "product_name",
                    "description", "description",
                    "quantity", "total_quantity",
                    "percentage", "percentage",
                    "price", "price");

            String sortColumn = fieldColumnMap.getOrDefault(sortField, "total_quantity");
            String sortDirection = sortOrder.equalsIgnoreCase("asc") ? "ASC" : "DESC";

            // Filterable fields
            List<String> allowedFields = List.of("product_name", "description", "quantity", "percentage", "price");

            StringBuilder whereClause = new StringBuilder("WHERE 1=1");
            for (String field : allowedFields) {
                String value = allParams.get(field + ".value");
                String matchMode = allParams.getOrDefault(field + ".matchMode", "contains");

                if (value != null && !value.trim().isEmpty()) {
                    String column = fieldColumnMap.getOrDefault(field, field); // 👈 use mapped column
                    boolean isNumericField = List.of("quantity", "price", "percentage").contains(field);
                    String sanitizedValue = value.replace("'", "''");
                    String condition;

                    if (isNumericField) {
                        switch (matchMode) {
                            case "equals" -> condition = "%s = %s".formatted(column, sanitizedValue);
                            case "notContains", "notEquals" -> condition = "%s <> %s".formatted(column, sanitizedValue);
                            case "contains", "startsWith", "endsWith" ->
                                condition = "%s::text LIKE '%%%s%%'".formatted(column, sanitizedValue);
                            default -> condition = "%s::text LIKE '%%%s%%'".formatted(column, sanitizedValue);
                        }
                    } else {
                        switch (matchMode) {
                            case "startsWith" -> condition = "LOWER(%s::text) LIKE '%s%%'".formatted(column,
                                    sanitizedValue.toLowerCase());
                            case "endsWith" -> condition = "LOWER(%s::text) LIKE '%%%s'".formatted(column,
                                    sanitizedValue.toLowerCase());
                            case "notContains" -> condition = "LOWER(%s::text) NOT LIKE '%%%s%%'".formatted(column,
                                    sanitizedValue.toLowerCase());
                            case "equals" ->
                                condition = "LOWER(%s::text) = '%s'".formatted(column, sanitizedValue.toLowerCase());
                            default -> condition = "LOWER(%s::text) LIKE '%%%s%%'".formatted(column,
                                    sanitizedValue.toLowerCase());
                        }
                    }

                    whereClause.append(" AND ").append(condition);
                }
            }

            String countQuery = String.format("""
                    SELECT COUNT(*) AS total FROM (
                        SELECT * FROM get_top_selling_products('%s'::TIMESTAMP, '%s'::TIMESTAMP, %s)
                    ) AS result
                    %s
                    """, startDate, endDate, formattedKey, whereClause);

            int totalCount = ((Number) postgresService.query(countQuery).get(0).get("total")).intValue();

            String dataQuery = String.format("""
                    SELECT * FROM (
                        SELECT * FROM get_top_selling_products('%s'::TIMESTAMP, '%s'::TIMESTAMP, %s)
                    ) AS result
                    %s
                    ORDER BY %s %s
                    OFFSET %d LIMIT %d
                    """, startDate, endDate, formattedKey,
                    whereClause, sortColumn, sortDirection, offset, size);

            List<Map<String, Object>> data = postgresService.query(dataQuery);
            List<Map<String, Object>> topProducts = new ArrayList<>();

            for (Map<String, Object> row : data) {
                String name = Objects.toString(row.get("product_name"), "N/A");
                int quantity = parseInteger(row.get("total_quantity"));
                double percent = parseDouble(row.get("percentage"));
                String description = Objects.toString(row.get("description"), "N/A");
                double price = parseDouble(row.get("price"));

                topProducts.add(Map.of(
                        "product_name", name,
                        "quantity", quantity,
                        "percentage", String.format("%.2f%%", percent),
                        "description", description,
                        "price", price));
            }

            return ResponseEntity.ok(Map.of(
                    "data", topProducts,
                    "page", page,
                    "size", size,
                    "count", totalCount));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch top selling products", "details", e.getMessage()));
        }
    }

    private int parseInteger(Object obj) {
        if (obj == null)
            return 0;
        if (obj instanceof Number)
            return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid integer format: " + obj, e);
        }
    }

    private double parseDouble(Object obj) {
        if (obj == null)
            return 0.0;
        if (obj instanceof Number)
            return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(obj.toString());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid double format: " + obj, e);
        }
    }
}
