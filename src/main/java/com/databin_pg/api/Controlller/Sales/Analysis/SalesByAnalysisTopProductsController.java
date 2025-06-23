package com.databin_pg.api.Controlller.Sales.Analysis;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.*;

@RestController
@RequestMapping("/api/analysis")
@CrossOrigin(origins = "*")
@Tag(name = "Sales Analysis - Top Products", description = "APIs for analyzing top-selling products with search, sort, and pagination")
public class SalesByAnalysisTopProductsController {

    @Autowired
    private PostgresService postgresService;

    @Operation(
        summary = "Get Top Product Sales Summary",
        description = "Returns a paginated, searchable, and sortable summary of product sales"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product sales summary fetched successfully"),
        @ApiResponse(responseCode = "500", description = "Server error while fetching product sales summary")
    })
    @GetMapping("/product-sales-summary")
    public ResponseEntity<?> getProductSalesSummary(
            @Parameter(description = "Start date for filtering (YYYY-MM-DD)") @RequestParam(name = "startDate") String startDate,
            @Parameter(description = "End date for filtering (YYYY-MM-DD)") @RequestParam(name = "endDate") String endDate,
            @Parameter(description = "Enterprise key (optional)") @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey,
            @Parameter(description = "Page number (zero-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "All other dynamic filter/sort parameters (e.g., product_name.value, sortField)") @RequestParam Map<String, String> allParams
    ) {
        try {
            int offset = page * size;
            String formattedEnterpriseKey = (enterpriseKey == null || enterpriseKey.isBlank())
                    ? "NULL" : "'" + enterpriseKey.replace("'", "''") + "'";

            // Allowed fields
            List<String> allowedFields = List.of("product_name", "units_sold", "total_sales");

            StringBuilder whereClause = new StringBuilder("WHERE 1=1");

            for (String field : allowedFields) {
                String value = allParams.get(field + ".value");
                String matchMode = allParams.getOrDefault(field + ".matchMode", "contains");

                if (value != null && !value.isBlank()) {
                    value = value.toLowerCase().replace("'", "''");
                    boolean isNumeric = field.equals("units_sold") || field.equals("total_sales");
                    String columnRef = "data." + field;

                    String condition = switch (matchMode) {
                        case "startsWith" -> "%s::text LIKE '%s%%'".formatted(columnRef, value);
                        case "endsWith" -> "%s::text LIKE '%%%s'".formatted(columnRef, value);
                        case "notContains" -> "LOWER(%s::text) NOT LIKE '%%%s%%'".formatted(columnRef, value);
                        case "equals" -> isNumeric
                                ? "%s = %s".formatted(columnRef, value)
                                : "LOWER(%s::text) = '%s'".formatted(columnRef, value);
                        case "greaterThan" -> isNumeric ? "%s > %s".formatted(columnRef, value) : "1=0";
                        case "lessThan" -> isNumeric ? "%s < %s".formatted(columnRef, value) : "1=0";
                        default -> "LOWER(%s::text) LIKE '%%%s%%'".formatted(columnRef, value);
                    };

                    whereClause.append(" AND ").append(condition);
                }
            }

            String sortField = allParams.getOrDefault("sortField", "total_sales");
            String sortOrder = allParams.getOrDefault("sortOrder", "desc").equalsIgnoreCase("desc") ? "DESC" : "ASC";
            String sortColumn = allowedFields.contains(sortField) ? "data." + sortField : "data.total_sales";

            // Base query
            String baseQuery = String.format("""
                SELECT * FROM get_product_sales_summary('%s'::timestamp, '%s'::timestamp, %s)
            """, startDate, endDate, formattedEnterpriseKey);

            // Data query with alias and prefixed filters
            String dataQuery = String.format("""
                SELECT * FROM (
                    %s
                ) AS data
                %s
                ORDER BY %s %s
                OFFSET %d LIMIT %d
            """, baseQuery, whereClause, sortColumn, sortOrder, offset, size);

            // Count query
            String countQuery = String.format("""
                SELECT COUNT(*) AS total FROM (
                    %s
                ) AS data
                %s
            """, baseQuery, whereClause);

            // Execute queries
            List<Map<String, Object>> result = postgresService.query(dataQuery);
            List<Map<String, Object>> countResult = postgresService.query(countQuery);

            int totalCount = (!countResult.isEmpty() && countResult.get(0).get("total") != null)
                    ? ((Number) countResult.get(0).get("total")).intValue()
                    : 0;

            List<Map<String, Object>> productSales = new ArrayList<>();
            for (Map<String, Object> row : result) {
                Map<String, Object> product = Map.of(
                        "product_name", row.get("product_name"),
                        "units_sold", row.get("units_sold"),
                        "total_sales", row.get("total_sales")
                );
                productSales.add(product);
            }

            return ResponseEntity.ok(Map.of(
                    "products", productSales,
                    "page", page,
                    "size", size,
                    "count", totalCount
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch product sales summary", "details", e.getMessage()));
        }
    }
}
