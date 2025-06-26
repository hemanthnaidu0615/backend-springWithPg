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
@Tag(name = "Sales Analysis - Shipping Breakdown", description = "APIs for analyzing shipping cost, method, status, and carriers with search, sort, and pagination")
public class SalesByAnalysisShippingBreakdownController {

    @Autowired
    private PostgresService postgresService;

    @Operation(
        summary = "Get Shipment Summary",
        description = "Returns a paginated, searchable, and sortable shipment summary including carrier, method, status, and cost"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shipment summary fetched successfully"),
        @ApiResponse(responseCode = "500", description = "Server error while fetching shipment summary")
    })
    @GetMapping("/shipment-summary")
    public ResponseEntity<?> getShipmentSummary(
            @Parameter(description = "Start date for filtering (YYYY-MM-DD)")
            @RequestParam(name = "startDate") String startDate,

            @Parameter(description = "End date for filtering (YYYY-MM-DD)")
            @RequestParam(name = "endDate") String endDate,

            @Parameter(description = "Enterprise key (optional)")
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey,

            @Parameter(description = "Page number (zero-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "50") int size,

            @Parameter(description = "Dynamic filtering and sorting parameters (e.g., carrier.value, shipment_cost.matchMode, sortField, etc.)")
            @RequestParam Map<String, String> allParams
    ) {
        try {
            String formattedKey = (enterpriseKey == null || enterpriseKey.isBlank())
                    ? "NULL" : "'" + enterpriseKey.replace("'", "''") + "'";

            String formattedStartDate = startDate.split("T")[0];
            String formattedEndDate = endDate.split("T")[0];
            int offset = page * size;

            List<String> allowedFields = List.of("carrier", "shipping_method", "shipment_status", "shipment_cost");

            StringBuilder whereClause = new StringBuilder("WHERE 1=1");
            for (String field : allowedFields) {
                String value = allParams.get(field + ".value");
                String matchMode = allParams.getOrDefault(field + ".matchMode", "contains");

                if (value != null && !value.isBlank()) {
                    value = value.toLowerCase().replace("'", "''");
                    boolean isNumeric = field.equals("shipment_cost");
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

            String sortField = allParams.getOrDefault("sortField", "shipment_cost");
            String sortOrder = allParams.getOrDefault("sortOrder", "desc").equalsIgnoreCase("desc") ? "DESC" : "ASC";
            String sortColumn = allowedFields.contains(sortField) ? "data." + sortField : "data.shipment_cost";

            String baseQuery = String.format("""
                SELECT * FROM get_shipment_summary('%s'::timestamp, '%s'::timestamp, %s)
            """, formattedStartDate, formattedEndDate, formattedKey);

            String dataQuery = String.format("""
                SELECT * FROM (
                    %s
                ) AS data
                %s
                ORDER BY %s %s
                OFFSET %d LIMIT %d
            """, baseQuery, whereClause, sortColumn, sortOrder, offset, size);

            String countQuery = String.format("""
                SELECT COUNT(*) AS total FROM (
                    %s
                ) AS data
                %s
            """, baseQuery, whereClause);

            List<Map<String, Object>> result = postgresService.query(dataQuery);
            List<Map<String, Object>> countResult = postgresService.query(countQuery);

            int totalCount = (!countResult.isEmpty() && countResult.get(0).get("total") != null)
                    ? ((Number) countResult.get(0).get("total")).intValue()
                    : 0;

            List<Map<String, Object>> shipmentData = new ArrayList<>();
            for (Map<String, Object> row : result) {
                Map<String, Object> shipment = Map.of(
                        "carrier", row.get("carrier"),
                        "shipping_method", row.get("shipping_method"),
                        "shipment_status", row.get("shipment_status"),
                        "shipment_cost", row.get("shipment_cost")
                );
                shipmentData.add(shipment);
            }

            return ResponseEntity.ok(Map.of(
                    "shipments", shipmentData,
                    "page", page,
                    "size", size,
                    "count", totalCount
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch shipment summary", "details", e.getMessage()));
        }
    }
}
