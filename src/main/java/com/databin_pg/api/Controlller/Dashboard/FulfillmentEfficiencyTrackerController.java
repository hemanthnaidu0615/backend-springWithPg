package com.databin_pg.api.Controlller.Dashboard;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.databin_pg.api.Service.PostgresService;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/fulfillment-efficiency")
@CrossOrigin(origins = "*")
@Tag(name = "Dashboard - Fulfillment Efficiency Tracker", description = "APIs for tracking order fulfillment efficiency")
public class FulfillmentEfficiencyTrackerController {

    @Autowired
    private PostgresService postgresService;

    @Operation(
        summary = "Get fulfillment summary (category-wise, date-wise)",
        description = "Returns a summary count of fulfillment events grouped by category (Picked, Packed, Shipped, Delivered) and day. Optionally filter by enterprise key."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved fulfillment summary"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/summary")
    public ResponseEntity<?> getFulfillmentSummary(
        @Parameter(description = "Start date in YYYY-MM-DD format", required = true)
        @RequestParam(name = "startDate") String startDate,

        @Parameter(description = "End date in YYYY-MM-DD format", required = true)
        @RequestParam(name = "endDate") String endDate,

        @Parameter(description = "Optional enterprise key for filtering, e.g. 'AWW' or 'AWD'")
        @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey) {

        try {
            String formattedStartDate = startDate.length() > 10 ? startDate.substring(0, 10) : startDate;
            String formattedEndDate = endDate.length() > 10 ? endDate.substring(0, 10) : endDate;

            String query = String.format("""
                SELECT * FROM get_fulfillment_summary('%s'::date, '%s'::date, %s)
            """, formattedStartDate, formattedEndDate,
                enterpriseKey == null ? "NULL" : String.format("'%s'", enterpriseKey));

            List<Map<String, Object>> rows = postgresService.query(query);
            Map<String, Map<String, Integer>> summary = new LinkedHashMap<>();

            for (Map<String, Object> row : rows) {
                String eventDay = row.get("event_day").toString();
                String category = Objects.toString(row.get("category"), "Unknown");
                int count = ((Number) row.get("event_count")).intValue();

                if (category.equals("Unknown")) continue;

                summary.computeIfAbsent(category, k -> new LinkedHashMap<>());
                summary.get(category).merge(eventDay, count, Integer::sum);
            }

            return ResponseEntity.ok(Map.of("fulfillment_summary", summary));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch fulfillment event data"));
        }
    }

    @Operation(
    	    summary = "Get fulfillment event details (paginated)",
    	    description = "Returns fulfillment event records between start and end date. Supports optional filtering by enterpriseKey and category, with pagination."
    	)
    	@ApiResponses(value = {
    	    @ApiResponse(responseCode = "200", description = "Successfully retrieved fulfillment event details"),
    	    @ApiResponse(responseCode = "500", description = "Internal server error while fetching data")
    	})
    @GetMapping("/details-grid")
    public ResponseEntity<?> getFulfillmentDetails(
        @RequestParam Map<String, String> allParams
    ) {
        try {
            String startDate = allParams.get("startDate");
            String endDate = allParams.get("endDate");
            String formattedStartDate = startDate.length() > 10 ? startDate.substring(0, 10) : startDate;
            String formattedEndDate = endDate.length() > 10 ? endDate.substring(0, 10) : endDate;

            int page = Integer.parseInt(allParams.getOrDefault("page", "0"));
            int size = Integer.parseInt(allParams.getOrDefault("size", "20"));
            int offset = page * size;

            String sortField = allParams.getOrDefault("sortField", "event_time");
            String sortOrder = allParams.getOrDefault("sortOrder", "asc");

            StringBuilder whereClause = new StringBuilder(
                "WHERE event_time BETWEEN '%s' AND '%s'".formatted(formattedStartDate, formattedEndDate)
            );

            List<String> allowedFields = List.of(
                "enterprise_key", "category", "order_id", "event_type", "event_description",
                "event_time", "fulfilment_channel", "fulfillment_city",
                "fulfillment_state", "fulfillment_country", "handler_name"
            );

            for (String field : allowedFields) {
                String value = allParams.get(field + ".value");
                String matchMode = allParams.getOrDefault(field + ".matchMode", "contains");

                if (value != null && !value.trim().isEmpty()) {
                    value = value.toLowerCase().replace("'", "''"); // sanitize input

                    String sqlCondition;
                    switch (matchMode) {
                    case "startsWith" -> sqlCondition = "LOWER(%s::text) LIKE '%s%%'".formatted(field, value);
                    case "endsWith" -> sqlCondition = "LOWER(%s::text) LIKE '%%%s'".formatted(field, value);
                    case "notContains" -> sqlCondition = "LOWER(%s::text) NOT LIKE '%%%s%%'".formatted(field, value);
                    case "equals" -> sqlCondition = "LOWER(%s::text) = '%s'".formatted(field, value);
                    case "contains" -> sqlCondition = "LOWER(%s::text) LIKE '%%%s%%'".formatted(field, value);
                    default -> sqlCondition = "LOWER(%s::text) LIKE '%%%s%%'".formatted(field, value); // fallback
                }


                    whereClause.append(" AND ").append(sqlCondition);
                }
            }

            Map<String, String> allowedSortFields = Map.of(
                "order_id", "order_id",
                "category", "category",
                "event_type", "event_type",
                "event_description", "event_description",
                "event_time", "event_time",
                "fulfilment_channel", "fulfilment_channel",
                "fulfillment_city", "fulfillment_city",
                "fulfillment_state", "fulfillment_state",
                "fulfillment_country", "fulfillment_country",
                "handler_name", "handler_name"
            );

            String sortColumn = allowedSortFields.getOrDefault(sortField, "event_time");
            String sortDirection = sortOrder.equalsIgnoreCase("desc") ? "DESC" : "ASC";

            String countQuery = """
                SELECT COUNT(*) AS total FROM get_fulfillment_details('%s'::date, '%s'::date, NULL, NULL) %s
            """.formatted(formattedStartDate, formattedEndDate, whereClause);

            int totalCount = ((Number) postgresService.query(countQuery).get(0).get("total")).intValue();

            String dataQuery = """
                SELECT * FROM get_fulfillment_details('%s'::date, '%s'::date, NULL, NULL)
                %s ORDER BY %s %s OFFSET %d LIMIT %d
            """.formatted(
                formattedStartDate,
                formattedEndDate,
                whereClause,
                sortColumn,
                sortDirection,
                offset,
                size
            );

            List<Map<String, Object>> rows = postgresService.query(dataQuery);

            return ResponseEntity.ok(Map.of(
                "fulfillment_details", rows,
                "page", page,
                "size", size,
                "count", totalCount
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch fulfillment event details", "details", e.getMessage()));
        }
    }



}
