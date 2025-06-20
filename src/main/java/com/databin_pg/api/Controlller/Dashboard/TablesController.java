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
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
@Tag(name = "Dashboard - Recent Orders", description = "APIs for retrieving paginated recent orders with optional enterprise filtering")
public class TablesController {

    @Autowired
    private PostgresService postgresService;
    @Operation(
            summary = "Get recent orders",
            description = "Retrieves recent orders between the specified start and end dates. Supports pagination and optional enterprise filtering (e.g., 'AWW' or 'AWD')."
        )
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recent orders retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to fetch recent orders")
        })
    @GetMapping("/recent-orders")
    public ResponseEntity<?> getRecentOrders(@RequestParam Map<String, String> allParams) {
        try {
            // Required params
            String startDate = allParams.get("startDate");
            String endDate = allParams.get("endDate");
            if (startDate == null || endDate == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required parameters: startDate or endDate"));
            }

            String formattedStartDate = startDate.length() > 10 ? startDate.substring(0, 10) : startDate;
            String formattedEndDate = endDate.length() > 10 ? endDate.substring(0, 10) : endDate;

            int page = Integer.parseInt(allParams.getOrDefault("page", "0"));
            int size = Integer.parseInt(allParams.getOrDefault("size", "10"));
            int offset = page * size;

            String sortField = allParams.getOrDefault("sortField", "order_date");
            String sortOrder = allParams.getOrDefault("sortOrder", "desc");

            String enterpriseKey = allParams.get("enterpriseKey");
            String formattedKey = (enterpriseKey == null || enterpriseKey.isBlank()) ? "NULL" : "'" + enterpriseKey.replace("'", "''") + "'";

            // Allowed fields for filtering
            List<String> allowedFields = List.of("order_id", "product_name", "category_name", "unit_price", "order_type", "shipment_status");

            StringBuilder whereClause = new StringBuilder("WHERE 1=1");

            for (String field : allowedFields) {
                String value = allParams.get(field + ".value");
                String matchMode = allParams.getOrDefault(field + ".matchMode", "contains");

                if (value != null && !value.trim().isEmpty()) {
                    value = value.toLowerCase().replace("'", "''");
                    String sqlCondition;

                    switch (matchMode) {
                        case "startsWith" -> sqlCondition = "LOWER(%s::text) LIKE '%s%%'".formatted(field, value);
                        case "endsWith" -> sqlCondition = "LOWER(%s::text) LIKE '%%%s'".formatted(field, value);
                        case "notContains" -> sqlCondition = "LOWER(%s::text) NOT LIKE '%%%s%%'".formatted(field, value);
                        case "equals" -> sqlCondition = "LOWER(%s::text) = '%s'".formatted(field, value);
                        default -> sqlCondition = "LOWER(%s::text) LIKE '%%%s%%'".formatted(field, value);
                    }

                    whereClause.append(" AND ").append(sqlCondition);
                }
            }

            // Sortable fields
            Map<String, String> allowedSortFields = Map.of(
                "order_id", "order_id",
                "product_name", "product_name",
                "category_name", "category_name",
                "unit_price", "unit_price",
                "order_type", "order_type",
                "shipment_status", "shipment_status",
                "order_date", "order_date"
            );

            String sortColumn = allowedSortFields.getOrDefault(sortField, "order_date");
            String sortDirection = sortOrder.equalsIgnoreCase("desc") ? "DESC" : "ASC";

            // COUNT query
            String countQuery = """
                SELECT COUNT(*) AS total FROM (
                    SELECT * FROM get_recent_orders('%s'::timestamp, '%s'::timestamp, %s)
                ) AS result %s
            """.formatted(formattedStartDate, formattedEndDate, formattedKey, whereClause);

            int totalCount = ((Number) postgresService.query(countQuery).get(0).get("total")).intValue();

            // DATA query
            String dataQuery = """
                SELECT * FROM (
                    SELECT * FROM get_recent_orders('%s'::timestamp, '%s'::timestamp, %s)
                ) AS result
                %s ORDER BY %s %s OFFSET %d LIMIT %d
            """.formatted(
                formattedStartDate,
                formattedEndDate,
                formattedKey,
                whereClause,
                sortColumn,
                sortDirection,
                offset,
                size
            );

            List<Map<String, Object>> rows = postgresService.query(dataQuery);

            return ResponseEntity.ok(Map.of(
                "data", rows,
                "page", page,
                "size", size,
                "count", totalCount
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch recent orders", "details", e.getMessage()));
        }
    }

}

