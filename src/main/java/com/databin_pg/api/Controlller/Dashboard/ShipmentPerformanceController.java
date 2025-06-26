package com.databin_pg.api.Controlller.Dashboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.databin_pg.api.Service.PostgresService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping("/api/shipment-performance")
@CrossOrigin(origins = "*")
@Tag(name = "Dashboard - Shipment Performance", description = "APIs for analyzing shipment performance and carrier shipment details")
public class ShipmentPerformanceController {

    @Autowired
    private PostgresService postgresService;

    @Operation(
            summary = "Get shipment performance summary by carrier",
            description = "Returns shipment counts grouped by carrier and shipment method (standard, expedited, same-day) within the given date range."
        )
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shipment performance data retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to fetch shipment performance data")
        })
    @GetMapping
    public ResponseEntity<?> getShipmentPerformance(
    		@Parameter(description = "Start date in YYYY-MM-DD format", required = true)
            @RequestParam(name = "startDate") String startDate,

            @Parameter(description = "End date in YYYY-MM-DD format", required = true)
            @RequestParam(name = "endDate") String endDate,

            @Parameter(description = "Optional enterprise key for filtering results 'AWW' or 'AWD'")
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey
    ) {
        try {
        	String query = String.format("""
        		    SELECT * FROM get_shipment_performance(
        		        '%s'::TIMESTAMP, 
        		        '%s'::TIMESTAMP, 
        		        %s
        		    )
        		""", startDate, endDate,
        		     enterpriseKey == null ? "NULL" : "'" + enterpriseKey + "'");


            List<Map<String, Object>> result = postgresService.query(query);
            List<Map<String, Object>> shipments = new ArrayList<>();

            for (Map<String, Object> row : result) {
                shipments.add(Map.of(
                        "carrier", Objects.toString(row.get("carrier"), "Unknown"),
                        "standard", parseInteger(row.get("standard_shipments")),
                        "expedited", parseInteger(row.get("expedited_shipments")),
                        "same_day", parseInteger(row.get("same_day_shipments"))
                ));
            }

            return ResponseEntity.ok(Map.of("shipment_performance", shipments));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch shipment performance data"));
        }
    }

    private int parseInteger(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
   
    @GetMapping("/details-grid")
    @Operation(
        summary = "Get detailed shipment records",
        description = "Returns detailed shipment records with pagination, sorting, and filtering by carrier, method, or enterprise key."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shipment detail data retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Failed to fetch shipment detail data")
    })
    public ResponseEntity<?> getShipmentDetails(@RequestParam Map<String, String> allParams) {
        try {
            // Required parameters
            String startDate = allParams.get("startDate");
            String endDate = allParams.get("endDate");
            if (startDate == null || endDate == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required parameters: startDate or endDate"));
            }

            String formattedStartDate = startDate.substring(0, 10);
            String formattedEndDate = endDate.substring(0, 10);

            // Pagination
            int page = Integer.parseInt(allParams.getOrDefault("page", "0"));
            int size = Integer.parseInt(allParams.getOrDefault("size", "10"));
            int offset = page * size;

            // Sorting
            String sortField = allParams.getOrDefault("sortField", "shipment_id");
            String sortOrder = allParams.getOrDefault("sortOrder", "desc");

            // Optional filtering
            String enterpriseKey = allParams.get("enterpriseKey");
            String carrier = allParams.get("carrier");
            String method = allParams.get("method");

            String formattedKey = enterpriseKey == null ? "NULL" : "'" + enterpriseKey.replace("'", "''") + "'";
            String formattedCarrier = carrier == null ? "NULL" : "'" + carrier.replace("'", "''") + "'";
            String formattedMethod = method == null ? "NULL" : "'" + method.replace("'", "''") + "'";

            // Filterable fields
            List<String> allowedFields = List.of(
                "shipment_id", "order_id", "carrier", "tracking_number", "shipment_status",
                "shipment_cost", "shipping_method", "estimated_shipment_date", "actual_shipment_date"
            );

            // Build WHERE clause for client-side filtering
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

            // Safe sorting
            Map<String, String> allowedSortFields = Map.ofEntries(
                Map.entry("shipment_id", "shipment_id"),
                Map.entry("order_id", "order_id"),
                Map.entry("carrier", "carrier"),
                Map.entry("tracking_number", "tracking_number"),
                Map.entry("shipment_status", "shipment_status"),
                Map.entry("shipment_cost", "shipment_cost"),
                Map.entry("shipping_method", "shipping_method"),
                Map.entry("estimated_shipment_date", "estimated_shipment_date"),
                Map.entry("actual_shipment_date", "actual_shipment_date")
            );

            String sortColumn = allowedSortFields.getOrDefault(sortField, "shipment_id");
            String sortDirection = sortOrder.equalsIgnoreCase("desc") ? "DESC" : "ASC";

            // COUNT query
            String countQuery = """
                SELECT COUNT(*) AS total FROM (
                    SELECT * FROM get_shipment_performance_details(
                        '%s'::timestamp, '%s'::timestamp, %s, %s, %s
                    )
                ) AS result %s
            """.formatted(
                formattedStartDate, formattedEndDate,
                formattedCarrier, formattedMethod, formattedKey,
                whereClause
            );

            int totalCount = ((Number) postgresService.query(countQuery).get(0).get("total")).intValue();

            // DATA query
            String dataQuery = """
                SELECT * FROM (
                    SELECT * FROM get_shipment_performance_details(
                        '%s'::timestamp, '%s'::timestamp, %s, %s, %s
                    )
                ) AS result
                %s ORDER BY %s %s OFFSET %d LIMIT %d
            """.formatted(
                formattedStartDate, formattedEndDate,
                formattedCarrier, formattedMethod, formattedKey,
                whereClause,
                sortColumn, sortDirection,
                offset, size
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
                .body(Map.of("error", "Failed to fetch shipment detail data", "details", e.getMessage()));
        }
    }

}
