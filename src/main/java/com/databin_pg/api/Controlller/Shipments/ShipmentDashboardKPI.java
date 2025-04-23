package com.databin_pg.api.Controlller.Shipments;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shipment-dashboard-kpi")
@CrossOrigin(origins = "http://localhost:5173")
public class ShipmentDashboardKPI {

    @Autowired
    private PostgresService postgresService;

    // 📌 API: Get Total Shipments (with date filter)
    @GetMapping("/total-shipments")
    public ResponseEntity<?> getTotalShipments(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "carrier", required = false) String carrier,
            @RequestParam(name = "shipmentMethod", required = false) String shipmentMethod
    ) {
        try {
            String carrierParam = (carrier == null || carrier.isBlank()) ? "NULL" : "'" + carrier + "'";
            String methodParam = (shipmentMethod == null || shipmentMethod.isBlank()) ? "NULL" : "'" + shipmentMethod + "'";

            String query = String.format("""
                SELECT get_total_shipments('%s'::TIMESTAMP, '%s'::TIMESTAMP, %s::TEXT, %s::TEXT) AS total_shipments
            """, startDate, endDate, carrierParam, methodParam);

            System.out.println("SQL Query: " + query);
            List<Map<String, Object>> data = postgresService.query(query);
            int totalShipments = data.isEmpty() ? 0 : ((Number) data.get(0).get("total_shipments")).intValue();

            return ResponseEntity.ok(Map.of("total_shipments", totalShipments));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch total shipments", "details", e.getMessage()));
        }
    }



    // 📌 API: Get On-Time Deliveries (with date filter)
    @GetMapping("/on-time-shipments")
    public ResponseEntity<?> getOnTimeShipments(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String carrier,
            @RequestParam(required = false) String shippingMethod) {
        try {
            String query = String.format("""
                SELECT get_on_time_shipments('%s'::TIMESTAMP, '%s'::TIMESTAMP, %s, %s) AS on_time_shipments
            """,
            startDate, endDate,
            carrier != null ? "'" + carrier + "'" : "NULL",
            shippingMethod != null ? "'" + shippingMethod + "'" : "NULL");

            List<Map<String, Object>> data = postgresService.query(query);
            int count = data.isEmpty() ? 0 : ((Number) data.get(0).get("on_time_shipments")).intValue();

            return ResponseEntity.ok(Map.of("on_time_shipments", count));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch on-time shipments"));
        }
    }


    // 📌 API: Get Average Delivery Time (with date filter)
    @GetMapping("/delayed-shipments")
    public ResponseEntity<?> getDelayedShipments(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String carrier,
            @RequestParam(required = false) String shippingMethod) {
        try {
            String query = String.format("""
                SELECT get_delayed_shipments('%s'::TIMESTAMP, '%s'::TIMESTAMP, %s, %s) AS delayed_shipments
            """,
            startDate, endDate,
            carrier != null ? "'" + carrier + "'" : "NULL",
            shippingMethod != null ? "'" + shippingMethod + "'" : "NULL");

            List<Map<String, Object>> data = postgresService.query(query);
            int count = data.isEmpty() ? 0 : ((Number) data.get(0).get("delayed_shipments")).intValue();

            return ResponseEntity.ok(Map.of("delayed_shipments", count));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch delayed shipments"));
        }
    }


    // 📌 API: Get Delayed Shipments (with date filter)
    @GetMapping("/average-delivery-time")
    public ResponseEntity<?> getAverageDeliveryTime(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String carrier,
            @RequestParam(required = false) String shippingMethod) {
        try {
            String query = String.format("""
                SELECT get_average_delivery_time('%s'::TIMESTAMP, '%s'::TIMESTAMP, %s, %s) AS average_delivery_time
            """,
            startDate, endDate,
            carrier != null ? "'" + carrier + "'" : "NULL",
            shippingMethod != null ? "'" + shippingMethod + "'" : "NULL");

            List<Map<String, Object>> data = postgresService.query(query);

            double avgTime = data.isEmpty() || data.get(0).get("average_delivery_time") == null
                    ? 0.0
                    : ((Number) data.get(0).get("average_delivery_time")).doubleValue();

            return ResponseEntity.ok(Map.of("average_delivery_time", String.format("%.2f", avgTime)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch average delivery time"));
        }
    }


}
