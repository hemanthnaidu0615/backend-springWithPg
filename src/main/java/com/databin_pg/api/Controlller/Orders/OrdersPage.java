package com.databin_pg.api.Controlller.Orders;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.*;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
@Tag(name = "Orders - Filtered", description = "APIs for fetching filtered and paginated orders")
public class OrdersPage {

    @Autowired
    private PostgresService postgresService;

    @GetMapping("/filtered")
    @Operation(
        summary = "Get filtered, sorted, and paginated orders",
        description = "Fetch orders filtered by fields and search terms, supports dynamic filters, sorting, and pagination"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Filtered orders retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error while fetching filtered orders")
    })
    public ResponseEntity<?> getFilteredOrders(
        @Parameter(description = "Start date for filtering orders (format: yyyy-MM-dd)", required = true)
        @RequestParam(name = "startDate") String startDate,

        @Parameter(description = "End date for filtering orders (format: yyyy-MM-dd)", required = true)
        @RequestParam(name = "endDate") String endDate,

        @Parameter(description = "Order status filter (optional)") @RequestParam(name = "status", required = false) String status,
        @Parameter(description = "Order type filter (optional)") @RequestParam(name = "orderType", required = false) String orderType,
        @Parameter(description = "Payment method filter (optional)") @RequestParam(name = "paymentMethod", required = false) String paymentMethod,
        @Parameter(description = "Carrier filter (optional)") @RequestParam(name = "carrier", required = false) String carrier,
        @Parameter(description = "Customer name search filter (optional)") @RequestParam(name = "searchCustomer", required = false) String searchCustomer,
        @Parameter(description = "Order ID search filter (optional)") @RequestParam(name = "searchOrderId", required = false) String searchOrderId,
        @Parameter(description = "Enterprise key filter (optional)") @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey,
        @Parameter(description = "Page number for pagination (default 0)") @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Page size for pagination (default 10)") @RequestParam(defaultValue = "10") int size,
        @RequestParam Map<String, String> allParams
    ) {
        try {
            int offset = page * size;

            String[] args = new String[] {
                startDate != null ? "TIMESTAMP '" + startDate + "'" : "NULL",
                endDate != null ? "TIMESTAMP '" + endDate + "'" : "NULL",
                status != null ? "'" + status.replace("'", "''") + "'" : "NULL",
                orderType != null ? "'" + orderType.replace("'", "''") + "'" : "NULL",
                paymentMethod != null ? "'" + paymentMethod.replace("'", "''") + "'" : "NULL",
                carrier != null ? "'" + carrier.replace("'", "''") + "'" : "NULL",
                searchCustomer != null ? "'" + searchCustomer.replace("'", "''") + "'" : "NULL",
                searchOrderId != null ? "'" + searchOrderId.replace("'", "''") + "'" : "NULL",
                enterpriseKey != null ? "'" + enterpriseKey.replace("'", "''") + "'" : "NULL"
            };

            String sortField = allParams.getOrDefault("sortField", "order_id");
            String sortOrder = allParams.getOrDefault("sortOrder", "asc");
            String sortDirection = sortOrder.equalsIgnoreCase("desc") ? "DESC" : "ASC";

            List<String> allowedFields = List.of("order_id", "customer_name", "order_type", "status", "payment_method", "carrier", "order_date");

            StringBuilder whereClause = new StringBuilder("WHERE 1=1");
            for (String field : allowedFields) {
                String value = allParams.get(field + ".value");
                String matchMode = allParams.getOrDefault(field + ".matchMode", "contains");

                if (value != null && !value.isBlank()) {
                    value = value.toLowerCase().replace("'", "''");
                    String condition = switch (matchMode) {
                        case "startsWith" -> "LOWER(%s::text) LIKE '%s%%'".formatted(field, value);
                        case "endsWith" -> "LOWER(%s::text) LIKE '%%%s'".formatted(field, value);
                        case "notContains" -> "LOWER(%s::text) NOT LIKE '%%%s%%'".formatted(field, value);
                        case "equals" -> "LOWER(%s::text) = '%s'".formatted(field, value);
                        default -> "LOWER(%s::text) LIKE '%%%s%%'".formatted(field, value);
                    };
                    whereClause.append(" AND ").append(condition);
                }
            }

            String sortColumn = allowedFields.contains(sortField) ? sortField : "order_id";

            // Count query
            String countQuery = """
                SELECT COUNT(*) as total FROM (
                    SELECT * FROM get_filtered_orders(
                        %s, %s, %s, %s, %s, %s, %s, %s, %s
                    )
                ) AS result
                %s
            """.formatted(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], whereClause);

            int totalCount = ((Number) postgresService.query(countQuery).get(0).get("total")).intValue();

            // Data query
            String dataQuery = """
                SELECT * FROM (
                    SELECT * FROM get_filtered_orders(
                        %s, %s, %s, %s, %s, %s, %s, %s, %s
                    )
                ) AS result
                %s
                ORDER BY %s %s
                OFFSET %d LIMIT %d
            """.formatted(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8],
                          whereClause, sortColumn, sortDirection, offset, size);

            List<Map<String, Object>> results = postgresService.query(dataQuery);

            return ResponseEntity.ok(Map.of(
                "data", results,
                "page", page,
                "size", size,
                "count", totalCount
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch filtered orders", "details", e.getMessage()));
        }
    }

}
