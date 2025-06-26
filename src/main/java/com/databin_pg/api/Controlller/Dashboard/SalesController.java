package com.databin_pg.api.Controlller.Dashboard;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping("/api/sales")
@CrossOrigin(origins = "*")
@Tag(name = "Dashboard - Sales Metrics", description = "APIs for general sales performance metrics and statewise customer revenue")
public class SalesController {

    @Autowired
    private PostgresService postgresService;
    
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

    @Operation(
            summary = "Get Sales Metrics",
            description = "Returns average order value, number of high spenders, new customers, and returning customers in a given date range."
        )
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved sales metrics"),
            @ApiResponse(responseCode = "500", description = "Failed to fetch sales metrics")
        })
    @GetMapping("/metrics")
    public ResponseEntity<?> getSalesMetrics(
    		 @Parameter(description = "Start date in YYYY-MM-DD format", required = true)
             @RequestParam(name = "startDate") String startDate,

             @Parameter(description = "End date in YYYY-MM-DD format", required = true)
             @RequestParam(name = "endDate") String endDate,

             @Parameter(description = "Optional enterprise key for filtering results 'AWW' or 'AWD'")
             @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey) {
        try {
        	String query = String.format("""
        		    SELECT * FROM get_sales_metrics('%s'::TIMESTAMP, '%s'::TIMESTAMP, %s)
        		""", startDate, endDate,
        		    enterpriseKey == null ? "NULL" : String.format("'%s'", enterpriseKey));


            List<Map<String, Object>> result = postgresService.query(query);

            if (result.isEmpty()) {
                return ResponseEntity.ok(Collections.singletonMap("message", "No data found for given range"));
            }

            Map<String, Object> row = result.get(0);

            Map<String, Object> response = new HashMap<>();
            response.put("avg_order_value", parseDouble(row.get("avg_order_value")));
            response.put("high_spenders", parseInteger(row.get("high_spenders")));
            response.put("new_customers", parseInteger(row.get("new_customers")));
            response.put("returning_customers", parseInteger(row.get("returning_customers")));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to fetch sales metrics"));
        }
    }
    
    @Operation(
            summary = "Get Statewise Revenue Metrics",
            description = "Returns revenue and customer distribution across U.S. states for a specified date range and enterprise key."
        )
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved map metrics"),
            @ApiResponse(responseCode = "500", description = "Failed to fetch map metrics")
        })
    @GetMapping("/map-metrics")
    public ResponseEntity<?> getMapMetrics(
    		 @Parameter(description = "Start date in YYYY-MM-DD format", required = true)
             @RequestParam(name = "startDate") String startDate,

             @Parameter(description = "End date in YYYY-MM-DD format", required = true)
             @RequestParam(name = "endDate") String endDate,

             @Parameter(description = "Optional enterprise key for filtering results 'AWW' or 'AWD'")
             @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey) {
        try {
        	String query = String.format("""
        		    SELECT * FROM get_statewise_customers_revenue('%s'::TIMESTAMP, '%s'::TIMESTAMP, %s)
        		""", startDate, endDate,
        		    enterpriseKey == null ? "NULL" : String.format("'%s'", enterpriseKey));

            List<Map<String, Object>> data = postgresService.query(query);
            mapStateNames(data);

            if (data.isEmpty()) {
                return ResponseEntity.ok(Collections.singletonMap("message", "No data found for given range"));
            }

            return ResponseEntity.ok(data);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to fetch map metrics"));
        }
    }

    private double parseDouble(Object obj) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(obj.toString().trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    private void mapStateNames(List<Map<String, Object>> data) {
        for (Map<String, Object> row : data) {
            Object abbrevObj = row.get("state");
            if (abbrevObj != null) {
                String abbreviation = abbrevObj.toString();
                String fullName = STATE_ABBREVIATIONS.getOrDefault(abbreviation, abbreviation);
                row.put("state", fullName);
            }
        }
    }

    private int parseInteger(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
