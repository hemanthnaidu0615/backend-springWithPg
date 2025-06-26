package com.databin_pg.api.Controlller.Shipments;

import com.databin_pg.api.Service.PostgresService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/recent-shipments")
@CrossOrigin(origins = "*")
@Tag(name = "Inventory-", description = "APIs for managing recent shipments and shipment details")
public class RecentShipmentsController {

    @Autowired
    private PostgresService postgresService;

    @Operation(summary = "Get Recent Shipments", description = "Returns paginated recent shipments with filtering and sorting.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Recent shipments fetched successfully"),
        @ApiResponse(responseCode = "500", description = "Server error while fetching shipments")
    })
    @GetMapping
    public ResponseEntity<?> getRecentShipments(
            @Parameter(description = "Start timestamp (YYYY-MM-DD)") @RequestParam(name = "startDate") String startDate,
            @Parameter(description = "End timestamp (YYYY-MM-DD)") @RequestParam(name = "endDate") String endDate,
            @Parameter(description = "Carrier name") @RequestParam(name = "carrier", required = false) String carrier,
            @Parameter(description = "Shipping method") @RequestParam(name = "shippingMethod", required = false) String shippingMethod,
            @Parameter(description = "Enterprise key") @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey,
            @Parameter(description = "Shipment ID") @RequestParam(name = "shipmentId", required = false) Integer shipmentId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @RequestParam Map<String, String> allParams
    ) {
        try {
            int offset = page * size;

            List<String> allowedFields = List.of("shipment_id", "customer_name", "carrier", "actual_shipment_date", "shipment_status");
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

            // Base parameters
            String baseQuery = """
                SELECT * FROM get_recent_shipments(
                    '%s'::TIMESTAMP,
                    '%s'::TIMESTAMP,
                    %s,
                    %s,
                    %s,
                    %s
                )
            """.formatted(
                startDate,
                endDate,
                carrier == null ? "NULL" : "'" + carrier.replace("'", "''") + "'",
                shippingMethod == null ? "NULL" : "'" + shippingMethod.replace("'", "''") + "'",
                enterpriseKey == null ? "NULL" : "'" + enterpriseKey.replace("'", "''") + "'",
                shipmentId == null ? "NULL" : shipmentId
            );

            // Sorting
            String sortField = allParams.getOrDefault("sortField", "shipment_id");
            String sortOrder = allParams.getOrDefault("sortOrder", "asc").equalsIgnoreCase("desc") ? "DESC" : "ASC";
            String sortColumn = allowedFields.contains(sortField) ? sortField : "shipment_id";

            String dataQuery = """
                SELECT * FROM (
                    %s
                ) AS data
                %s
                ORDER BY %s %s
                OFFSET %d LIMIT %d
            """.formatted(baseQuery, whereClause, sortColumn, sortOrder, offset, size);

            String countQuery = """
                SELECT COUNT(*) as total FROM (
                    %s
                ) AS data
                %s
            """.formatted(baseQuery, whereClause);

            List<Map<String, Object>> data = postgresService.query(dataQuery);
            List<Map<String, Object>> countResult = postgresService.query(countQuery);
            int totalCount = ((Number) countResult.get(0).get("total")).intValue();

            return ResponseEntity.ok(Map.of(
                "data", data,
                "page", page,
                "size", size,
                "count", totalCount
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch recent shipments", "details", e.getMessage()));
        }
    }

    @Operation(summary = "Get Shipment Details", description = "Fetches detailed information about a specific shipment.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shipment details fetched successfully"),
        @ApiResponse(responseCode = "500", description = "Error fetching shipment details")
    })
    @GetMapping("/details")
    public ResponseEntity<?> getShipmentDetails(
            @Parameter(description = "Shipment ID") @RequestParam(name = "shipmentId") Integer shipmentId) {
        try {
            String query = String.format("SELECT * FROM get_shipment_details(%d)", shipmentId);
            List<Map<String, Object>> data = postgresService.query(query);
            return ResponseEntity.ok(data.isEmpty() ? Map.of("message", "No details found") : data.get(0));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch shipment details", "details", e.getMessage()));
        }
    }
}
