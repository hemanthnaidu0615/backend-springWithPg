package com.databin_pg.api.Controlller.Shipments;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.databin_pg.api.Service.PostgresService;

@RestController
@RequestMapping("/api/recent-shipments")
@CrossOrigin(origins = "*")
public class RecentShipmentsController {

    @Autowired
    private PostgresService postgresService;

    @GetMapping
    public ResponseEntity<?> getRecentShipments(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "carrier", required = false) String carrier,
            @RequestParam(name = "shippingMethod", required = false) String shippingMethod,
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey,
            @RequestParam(name = "shipmentId", required = false) Integer shipmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            int offset = page * size;

            String formattedCarrier = carrier != null ? "'" + carrier + "'" : "NULL";
            String formattedShippingMethod = shippingMethod != null ? "'" + shippingMethod + "'" : "NULL";
            String formattedEnterpriseKey = enterpriseKey != null ? "'" + enterpriseKey + "'" : "NULL";
            String formattedShipmentId = shipmentId != null ? shipmentId.toString() : "NULL";

            // 🔢 Count query
            String countQuery = String.format("""
                SELECT COUNT(*) as total FROM get_recent_shipments(
                    '%s'::TIMESTAMP,
                    '%s'::TIMESTAMP,
                    %s,
                    %s,
                    %s,
                    %s
                )
            """, startDate, endDate, formattedCarrier, formattedShippingMethod, formattedEnterpriseKey, formattedShipmentId);

            List<Map<String, Object>> countResult = postgresService.query(countQuery);
            int totalCount = 0;
            if (!countResult.isEmpty() && countResult.get(0).get("total") != null) {
                totalCount = ((Number) countResult.get(0).get("total")).intValue();
            }

            // 📦 Data query with OFFSET + LIMIT
            String dataQuery = String.format("""
                SELECT * FROM get_recent_shipments(
                    '%s'::TIMESTAMP,
                    '%s'::TIMESTAMP,
                    %s,
                    %s,
                    %s,
                    %s
                )
                OFFSET %d LIMIT %d
            """, startDate, endDate, formattedCarrier, formattedShippingMethod,
                    formattedEnterpriseKey, formattedShipmentId, offset, size);

            List<Map<String, Object>> data = postgresService.query(dataQuery);

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

    @GetMapping("/details")
    public ResponseEntity<?> getShipmentDetails(@RequestParam(name = "shipmentId") Integer shipmentId) {
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
