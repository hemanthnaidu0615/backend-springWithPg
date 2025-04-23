package com.databin_pg.api.Controlller;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard-kpi")
@CrossOrigin(origins = "http://localhost:5173")
public class DashboardKPIController {

    @Autowired
    private PostgresService postgresService;

    // 📌 API: Get Total Orders Count (with date filter) using stored procedure
 // 📌 API: Get Total Orders Count (with date filter)
    @GetMapping("/total-orders")
    public ResponseEntity<?> getTotalOrders(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate) {
        try {
            // Call the stored function to get the total orders
            String query = String.format("""
                SELECT get_total_orders('%s', '%s') AS total_orders
            """, startDate, endDate);

            List<Map<String, Object>> data = postgresService.query(query);

            // Extract total orders from the query result
            int totalOrders = data.isEmpty() ? 0 : ((Number) data.get(0).get("total_orders")).intValue();

            return ResponseEntity.ok(Map.of("total_orders", totalOrders));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch total orders"));
        }
    }

    // 📌 API: Get Shipment Status Percentages (with date filter)
    @GetMapping("/shipment-status-percentage")
    public ResponseEntity<?> getShipmentStatusPercentage(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate) {
        try {
            String query = String.format("""
                SELECT * FROM get_shipment_status_data('%s', '%s')
            """, startDate, endDate);

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


    // 📌 API: Get Fulfillment Rate (with date filter)
    @GetMapping("/fulfillment-rate")
    public ResponseEntity<?> getFulfillmentRate(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate) {
        try {
            // Call stored procedure
            String query = String.format("""
                SELECT get_fulfillment_rate('%s', '%s') AS fulfillment_rate
            """, startDate, endDate);

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

    // 📌 API: Get Out-of-Stock Product Count (no date filter)
    @GetMapping("/out-of-stock")
    public ResponseEntity<?> getOutOfStockCount(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate) {
        try {
            String query = String.format("""
                SELECT get_out_of_stock_count('%s', '%s') AS out_of_stock_count
            """, startDate, endDate);

            List<Map<String, Object>> data = postgresService.query(query);
            int count = data.isEmpty() ? 0 : ((Number) data.get(0).get("out_of_stock_count")).intValue();

            return ResponseEntity.ok(Map.of("out_of_stock_count", count));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch out-of-stock count"));
        }
    }

}
