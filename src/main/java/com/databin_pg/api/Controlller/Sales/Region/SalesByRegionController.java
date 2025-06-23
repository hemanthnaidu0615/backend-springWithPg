package com.databin_pg.api.Controlller.Sales.Region;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.databin_pg.api.Service.PostgresService;

@RestController
@RequestMapping("/api/sales-by-region")
@CrossOrigin(origins = "*")
@Tag(name = "Sales By Region-")
public class SalesByRegionController {

    @Autowired
    private PostgresService postgresService;

    // 🗺️ State abbreviation to full name map
    private static final Map<String, String> STATE_ABBREVIATIONS = Map.ofEntries(
    	    Map.entry("AL", "Alabama"),
    	    Map.entry("AK", "Alaska"),
    	    Map.entry("AZ", "Arizona"),
    	    Map.entry("AR", "Arkansas"),
    	    Map.entry("CA", "California"),
    	    Map.entry("CO", "Colorado"),
    	    Map.entry("CT", "Connecticut"),
    	    Map.entry("DE", "Delaware"),
    	    Map.entry("FL", "Florida"),
    	    Map.entry("GA", "Georgia"),
    	    Map.entry("HI", "Hawaii"),
    	    Map.entry("ID", "Idaho"),
    	    Map.entry("IL", "Illinois"),
    	    Map.entry("IN", "Indiana"),
    	    Map.entry("IA", "Iowa"),
    	    Map.entry("KS", "Kansas"),
    	    Map.entry("KY", "Kentucky"),
    	    Map.entry("LA", "Louisiana"),
    	    Map.entry("ME", "Maine"),
    	    Map.entry("MD", "Maryland"),
    	    Map.entry("MA", "Massachusetts"),
    	    Map.entry("MI", "Michigan"),
    	    Map.entry("MN", "Minnesota"),
    	    Map.entry("MS", "Mississippi"),
    	    Map.entry("MO", "Missouri"),
    	    Map.entry("MT", "Montana"),
    	    Map.entry("NE", "Nebraska"),
    	    Map.entry("NV", "Nevada"),
    	    Map.entry("NH", "New Hampshire"),
    	    Map.entry("NJ", "New Jersey"),
    	    Map.entry("NM", "New Mexico"),
    	    Map.entry("NY", "New York"),
    	    Map.entry("NC", "North Carolina"),
    	    Map.entry("ND", "North Dakota"),
    	    Map.entry("OH", "Ohio"),
    	    Map.entry("OK", "Oklahoma"),
    	    Map.entry("OR", "Oregon"),
    	    Map.entry("PA", "Pennsylvania"),
    	    Map.entry("RI", "Rhode Island"),
    	    Map.entry("SC", "South Carolina"),
    	    Map.entry("SD", "South Dakota"),
    	    Map.entry("TN", "Tennessee"),
    	    Map.entry("TX", "Texas"),
    	    Map.entry("UT", "Utah"),
    	    Map.entry("VT", "Vermont"),
    	    Map.entry("VA", "Virginia"),
    	    Map.entry("WA", "Washington"),
    	    Map.entry("WV", "West Virginia"),
    	    Map.entry("WI", "Wisconsin"),
    	    Map.entry("WY", "Wyoming"),
    	    Map.entry("DC", "District of Columbia")
    	);


    @GetMapping
    @Operation(summary = "Get sales data by region with filters, sorting and pagination")
    public ResponseEntity<?> getSalesByRegion(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String enterpriseKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam Map<String, String> allParams
    ) {
        try {
            int offset = page * size;

            String sortField = allParams.getOrDefault("sortField", "state_revenue");
            String sortOrder = allParams.getOrDefault("sortOrder", "desc").equalsIgnoreCase("desc") ? "DESC" : "ASC";

            // Only allow specific fields to prevent SQL injection
            List<String> allowedFields = List.of("state_name", "state_revenue");
            String sortColumn = allowedFields.contains(sortField) ? sortField : "state_revenue";

            // 🔧 Filtering on state_name (with match modes)
            StringBuilder whereClause = new StringBuilder("WHERE 1=1");

            for (String field : allowedFields) {
                String value = allParams.get(field + ".value");
                String matchMode = allParams.getOrDefault(field + ".matchMode", "contains");

                if (value != null && !value.isBlank()) {
                    value = value.toLowerCase().replace("'", "''"); // Prevent SQL injection
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

            // Base function query
            String baseQuery = String.format("""
                SELECT * FROM get_sales_by_region(
                    '%s'::TIMESTAMP,
                    '%s'::TIMESTAMP,
                    %s::TEXT
                )
            """, startDate, endDate, enterpriseKey == null ? "NULL" : "'" + enterpriseKey.replace("'", "''") + "'");

            // Final data query
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

            List<Map<String, Object>> result = postgresService.query(dataQuery);
            List<Map<String, Object>> countResult = postgresService.query(countQuery);
            int total = ((Number) countResult.get(0).get("total")).intValue();

            mapStateNames(result);

            return ResponseEntity.ok(Map.of(
                    "data", result,
                    "page", page,
                    "size", size,
                    "count", total
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch sales by region", "details", e.getMessage()));
        }
    }


    @GetMapping("/top5")
    @Operation(summary = "Get top 5 states by revenue")
    public ResponseEntity<?> getTop5RevenueStates(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String enterpriseKey
    ) {
        try {
            String query = String.format("""
                    SELECT * FROM get_top5_states_by_revenue(
                        '%s'::TIMESTAMP,
                        '%s'::TIMESTAMP,
                        %s::TEXT
                    )
                """, startDate, endDate, enterpriseKey == null ? "NULL" : "'" + enterpriseKey + "'");

            List<Map<String, Object>> data = postgresService.query(query);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/countrywide")
    @Operation(summary = "Get countrywide sales data")
    public ResponseEntity<?> getCountrywideSales(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String enterpriseKey
    ) {
        try {
            String query = String.format("""
                    SELECT * FROM get_countrywide_sales(
                        '%s'::TIMESTAMP,
                        '%s'::TIMESTAMP,
                        %s::TEXT
                    )
                """, startDate, endDate, enterpriseKey == null ? "NULL" : "'" + enterpriseKey + "'");

            List<Map<String, Object>> data = postgresService.query(query);
            mapStateNames(data);
            return ResponseEntity.ok(data);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private void mapStateNames(List<Map<String, Object>> data) {
        for (Map<String, Object> row : data) {
            Object abbrevObj = row.get("state_name");
            if (abbrevObj != null) {
                String abbreviation = abbrevObj.toString();
                String fullName = STATE_ABBREVIATIONS.getOrDefault(abbreviation, abbreviation);
                row.put("state_name", fullName);
            }
        }
    }
}

