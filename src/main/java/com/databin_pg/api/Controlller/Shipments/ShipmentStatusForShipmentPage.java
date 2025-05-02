package com.databin_pg.api.Controlller.Shipments;


import com.databin_pg.api.Service.PostgresService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shipment-status")
@CrossOrigin(origins = "http://localhost:5173")
public class ShipmentStatusForShipmentPage {

    @Autowired
    private PostgresService postgresService;

    @GetMapping("/distribution")
    public ResponseEntity<?> getShipmentStatusDistribution(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "carrier", required = false) String carrier,
            @RequestParam(name = "shippingMethod", required = false) String shippingMethod,
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey
    ) {
        try {
            String query = String.format("""
                SELECT * FROM get_shipment_status_distribution(
                    '%s'::TIMESTAMP,
                    '%s'::TIMESTAMP,
                    %s,
                    %s,
                    %s
                )
            """,
                startDate,
                endDate,
                carrier != null ? "'" + carrier + "'" : "NULL",
                shippingMethod != null ? "'" + shippingMethod + "'" : "NULL",
                enterpriseKey != null ? "'" + enterpriseKey + "'" : "NULL"
            );

            List<Map<String, Object>> data = postgresService.query(query);

            if (data.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "delivered", "0.0%",
                    "in_transit", "0.0%",
                    "delayed", "0.0%",
                    "cancelled", "0.0%"
                ));
            }

            Map<String, Object> row = data.get(0);

            return ResponseEntity.ok(Map.of(
                "delivered", row.get("delivered"),
                "in_transit", row.get("in_transit"),
                "delayed", row.get("delayed"),
                "cancelled", row.get("cancelled")
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch shipment status distribution"));
        }
    }
}