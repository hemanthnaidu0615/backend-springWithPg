package com.databin_pg.api.Controlller.Sales.Analysis;
import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;

@RestController
@RequestMapping("/api/analysis")
@CrossOrigin(origins = "http://localhost:5173")

public class SalesByAnalysisShippingBreakdownController {
	  @Autowired
	    private PostgresService postgresService;

	    @GetMapping("/shipment-summary")
	    public ResponseEntity<?> getShipmentSummary(
	            @RequestParam(name = "startDate") String startDate,
	            @RequestParam(name = "endDate") String endDate,
	            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey
	    ) {
	        try {
	            // Format nullable parameter
	            String formattedEnterpriseKey = (enterpriseKey == null || enterpriseKey.isBlank())
	                    ? "NULL" : "'" + enterpriseKey + "'";

	            // SQL query
	            String query = String.format("""
	                SELECT * FROM get_shipment_summary('%s'::timestamp, '%s'::timestamp, %s)
	            """, startDate, endDate, formattedEnterpriseKey);

	            // Execute query
	            List<Map<String, Object>> result = postgresService.query(query);

	            if (result.isEmpty()) {
	                return ResponseEntity.status(HttpStatus.NO_CONTENT)
	                        .body(Map.of("message", "No shipment data found for the given period."));
	            }

	            // Format response
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

	            return ResponseEntity.ok(Map.of("shipments", shipmentData));

	        } catch (Exception e) {
	            e.printStackTrace();
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                    .body(Map.of("error", "Failed to fetch shipment summary", "details", e.getMessage()));
	        }
	    }
}
