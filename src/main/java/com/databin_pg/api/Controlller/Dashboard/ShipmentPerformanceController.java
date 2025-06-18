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
    @Operation(
            summary = "Get detailed shipment performance by carrier and method",
            description = "Returns detailed shipment records filtered by carrier and shipment method within the given date range."
        )
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shipment detail data retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to fetch shipment detail data")
        })
    @GetMapping("/details")
    public ResponseEntity<?> getShipmentDetails(
    		 @Parameter(description = "Start date in YYYY-MM-DD format", required = true)
             @RequestParam(name = "startDate") String startDate,

             @Parameter(description = "End date in YYYY-MM-DD format", required = true)
             @RequestParam(name = "endDate") String endDate,

             @Parameter(description = "Carrier name to filter the details", required = true)
             @RequestParam(name = "carrier") String carrier,

             @Parameter(description = "Shipment method (e.g., standard, expedited, same_day)", required = true)
             @RequestParam(name = "method") String method,

             @Parameter(description = "Optional enterprise key for filtering results 'AWW' or 'AWD'")
             @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey
    ) {
        try {
            String query = String.format("""
                SELECT * FROM get_shipment_performance_details(
                    '%s'::TIMESTAMP,
                    '%s'::TIMESTAMP,
                    '%s',
                    '%s',
                    %s
                )
            """,
                startDate, endDate,
                carrier, method,
                enterpriseKey == null ? "NULL" : "'" + enterpriseKey + "'"
            );

            List<Map<String, Object>> details = postgresService.query(query);
            return ResponseEntity.ok(Map.of("shipment_details", details));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch shipment detail data"));
        }
    }

}
