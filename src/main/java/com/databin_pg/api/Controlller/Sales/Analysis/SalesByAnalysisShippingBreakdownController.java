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
	            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey,
	            @RequestParam(defaultValue = "0") int page,
	            @RequestParam(defaultValue = "50") int size
	    ) {
	        try {
	            // Prepare values
	            String formattedKey = (enterpriseKey == null || enterpriseKey.isBlank()) ? "NULL" : "'" + enterpriseKey + "'";
	            String formattedStartDate = startDate.split("T")[0];
	            String formattedEndDate = endDate.split("T")[0];
	            int offset = page * size;

	            // Count query
	            String countQuery = String.format("""
	                SELECT COUNT(*) AS total FROM get_shipment_summary('%s'::timestamp, '%s'::timestamp, %s)
	            """, formattedStartDate, formattedEndDate, formattedKey);

	            List<Map<String, Object>> countResult = postgresService.query(countQuery);
	            int totalCount = 0;
	            if (!countResult.isEmpty() && countResult.get(0).get("total") != null) {
	                totalCount = ((Number) countResult.get(0).get("total")).intValue();
	            }

	            // Data query with LIMIT and OFFSET
	            String dataQuery = String.format("""
	                SELECT * FROM get_shipment_summary('%s'::timestamp, '%s'::timestamp, %s)
	                OFFSET %d LIMIT %d
	            """, formattedStartDate, formattedEndDate, formattedKey, offset, size);

	            List<Map<String, Object>> result = postgresService.query(dataQuery);

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
