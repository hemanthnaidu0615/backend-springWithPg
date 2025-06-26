package com.databin_pg.api.Controlller.Fulfillment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.databin_pg.api.Service.PostgresService;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/fulfillment")
@CrossOrigin(origins = "*")
@Tag(name = "Fulfillment - Orders In Process", description = "APIs for managing orders in process and their details")
public class OrdersInProcessController {

    @Autowired
    private PostgresService postgresService;
   
    @GetMapping("/orders-in-process")
    @Operation(
        summary = "Get Orders In Process",
        description = "Fetch paginated list of orders in process with optional filtering, sorting, and pagination"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved orders in process"),
        @ApiResponse(responseCode = "500", description = "Failed to fetch orders in process")
    })
    public ResponseEntity<?> getOrdersInProcess(
        @Parameter(description = "Start date in YYYY-MM-DD or ISO format", required = true)
        @RequestParam(name = "startDate") String startDate,

        @Parameter(description = "End date in YYYY-MM-DD or ISO format", required = true)
        @RequestParam(name = "endDate") String endDate,

        @Parameter(description = "Optional enterprise key to filter results")
        @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey,

        @Parameter(description = "Page number for pagination (default 0)")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "Page size for pagination (default 10)")
        @RequestParam(defaultValue = "10") int size,

        @RequestParam Map<String, String> allParams
    ) {
        try {
            String formattedKey = (enterpriseKey == null || enterpriseKey.isBlank()) ? "NULL" : "'" + enterpriseKey.replace("'", "''") + "'";
            String formattedStartDate = startDate.length() > 10 ? startDate.substring(0, 10) : startDate;
            String formattedEndDate = endDate.length() > 10 ? endDate.substring(0, 10) : endDate;

            int offset = page * size;

            String sortField = allParams.getOrDefault("sortField", "order_id");
            String sortOrder = allParams.getOrDefault("sortOrder", "asc");

            // Define allowed filterable and sortable fields
            List<String> allowedFields = List.of("order_id", "customer_name", "product_name", "shipment_status", "order_type", "unit_price", "order_date");
            Map<String, String> allowedSortFields = Map.of(
                "order_id", "order_id",
                "customer_name", "customer_name",
                "product_name", "product_name",
                "shipment_status", "shipment_status",
                "order_type", "order_type",
                "unit_price", "unit_price",
                "order_date", "order_date"
            );

            // Build dynamic WHERE clause
            StringBuilder whereClause = new StringBuilder("WHERE 1=1");
            for (String field : allowedFields) {
                String value = allParams.get(field + ".value");
                String matchMode = allParams.getOrDefault(field + ".matchMode", "contains");

                if (value != null && !value.isBlank()) {
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

            String sortColumn = allowedSortFields.getOrDefault(sortField, "order_id");
            String sortDirection = sortOrder.equalsIgnoreCase("desc") ? "DESC" : "ASC";

            // Count Query
            String countQuery = """
                SELECT COUNT(*) AS total FROM (
                    SELECT * FROM get_orders_in_process('%s', '%s', %s)
                ) AS result %s
            """.formatted(formattedStartDate, formattedEndDate, formattedKey, whereClause);

            int totalCount = ((Number) postgresService.query(countQuery).get(0).get("total")).intValue();

            // Data Query
            String dataQuery = """
                SELECT * FROM (
                    SELECT * FROM get_orders_in_process('%s', '%s', %s)
                ) AS result
                %s ORDER BY %s %s OFFSET %d LIMIT %d
            """.formatted(
                formattedStartDate, formattedEndDate, formattedKey,
                whereClause, sortColumn, sortDirection,
                offset, size
            );

            List<Map<String, Object>> data = postgresService.query(dataQuery);

            return ResponseEntity.ok(Map.of(
                "data", data,
                "page", page,
                "size", size,
                "count", totalCount
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch orders in process", "details", e.getMessage()));
        }
    }

    @Operation(
            summary = "Get Order Details",
            description = "Retrieve detailed timeline of events for a specific order by orderId."
        )
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved order details"),
            @ApiResponse(responseCode = "500", description = "Failed to fetch order details")
        })
    @GetMapping("/details")
    public ResponseEntity<?> getOrderDetails(@Parameter(description = "Unique identifier of the order", required = true)
    @RequestParam(name = "orderId") int orderId){
        try {
            String query = String.format("SELECT * FROM get_orderInProcess_details(%d)", orderId);

            List<Map<String, Object>> results = postgresService.query(query);

            List<Map<String, Object>> timeline = new ArrayList<>();
            int eventCount = results.size();
            for (int i = 0; i < eventCount; i++) {
                Map<String, Object> row = results.get(i);
                String eventLabel = (i == eventCount - 1) ? "Final Event" : "Event-" + (i + 1);

                Map<String, Object> eventMap = new HashMap<>();
                eventMap.put("event", eventLabel);
                eventMap.put("event_type", row.get("event_type"));
                eventMap.put("eta", row.get("eta").toString());

                timeline.add(eventMap);
            }

            Map<String, Object> response = Map.of(
                    "orderId", orderId,
                    "timeline", timeline);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch order details", "details", e.getMessage()));
        }
    }

}
