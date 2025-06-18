package com.databin_pg.api.Controlller.Dashboard;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/dashboard-kpi")
@CrossOrigin(origins = "*")
@Tag(name = "Dashboard- KPI", description = "APIs for Dashboard kpis")
public class DashboardKPIController {

    @Autowired
    private PostgresService postgresService;

    @Operation(summary = "Get total orders", description = "Returns the total number of orders between two dates, optionally filtered by enterpriseKey")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved total orders"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    
    @GetMapping("/total-orders")
    public ResponseEntity<?> getTotalOrders(
    		@Parameter(description = "Start date in YYYY-MM-DD format", required = true)
            @RequestParam(name = "startDate") String startDate,
            @Parameter(description = "End date in YYYY-MM-DD format", required = true)
            @RequestParam(name = "endDate") String endDate,
            @Parameter(description = "Optional enterprise key filter 'AWW' or 'AWD' ")
            @RequestParam(name = "enterpriseKey", required=false) String enterpriseKey) {
        try {
        	String query = String.format("""
        		    SELECT get_total_orders('%s', '%s', %s) AS total_orders
        		""", startDate, endDate,
        		        (enterpriseKey == null || enterpriseKey.isBlank()) ? "NULL" : "'" + enterpriseKey + "'");


            List<Map<String, Object>> data = postgresService.query(query);

            int totalOrders = data.isEmpty() ? 0 : ((Number) data.get(0).get("total_orders")).intValue();

            return ResponseEntity.ok(Map.of("total_orders", totalOrders));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch total orders"));
        }
    }


    @Operation(summary = "Get shipment status percentage", description = "Returns shipment delay percentage and in-transit order count")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved shipment status data"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/shipment-status-percentage")
    public ResponseEntity<?> getShipmentStatusPercentage(
    		@Parameter(description = "Start date in YYYY-MM-DD format", required = true)
            @RequestParam(name = "startDate") String startDate,
            @Parameter(description = "End date in YYYY-MM-DD format", required = true)
            @RequestParam(name = "endDate") String endDate,
            @Parameter(description = "Optional enterprise key filter 'AWW' or 'AWD' ")
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey) {
        try {
        	String formattedKey = (enterpriseKey == null || enterpriseKey.isBlank()) ? "NULL" : "'" + enterpriseKey + "'";
        	String query = String.format("""
        	    SELECT * FROM get_shipment_status_data('%s', '%s', %s)
        	""", startDate, endDate, formattedKey);


            List<Map<String, Object>> data = postgresService.query(query);

            if (data.isEmpty()) {
                return ResponseEntity.ok(Map.of("delayed_percentage", "0.00%", "in_transit_orders", 0));
            }

            int totalOrders = ((Number) data.get(0).get("total_orders")).intValue();
            int delayedOrders = ((Number) data.get(0).get("delayed_orders")).intValue();
            int inTransitOrders = ((Number) data.get(0).get("in_transit_orders")).intValue();

            double delayedPercentage = (totalOrders > 0)
                    ? (delayedOrders * 100.0 / totalOrders)
                    : 0.0;

            return ResponseEntity.ok(Map.of(
                    "delayed_percentage", String.format("%.2f%%", delayedPercentage),
                    "in_transit_orders", inTransitOrders
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch shipment data"));
        }
    }


    @Operation(summary = "Get fulfillment rate", description = "Returns the fulfillment rate of orders in percentage")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved fulfillment rate"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/fulfillment-rate")
    public ResponseEntity<?> getFulfillmentRate(
    		 @Parameter(description = "Start date in YYYY-MM-DD format", required = true)
             @RequestParam(name = "startDate") String startDate,
             @Parameter(description = "End date in YYYY-MM-DD format", required = true)
             @RequestParam(name = "endDate") String endDate,
             @Parameter(description = "Optional enterprise key filter 'AWW' or 'AWD'")
             @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey) {
        try {
        	String formattedKey = (enterpriseKey == null || enterpriseKey.isBlank()) ? "NULL" : "'" + enterpriseKey + "'";
        	String query = String.format("""
        	    SELECT get_fulfillment_rate('%s', '%s', %s) AS fulfillment_rate
        	""", startDate, endDate, formattedKey);


            List<Map<String, Object>> data = postgresService.query(query);

            if (data.isEmpty() || data.get(0).get("fulfillment_rate") == null) {
                return ResponseEntity.ok(Map.of("message", "No fulfillment data available."));
            }

            double rate = ((Number) data.get(0).get("fulfillment_rate")).doubleValue();
            return ResponseEntity.ok(Map.of("fulfillment_rate", String.format("%.2f%%", rate)));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch fulfillment rate"));
        }
    }
}
