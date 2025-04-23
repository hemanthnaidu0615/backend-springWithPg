package com.databin_pg.api.Controlller.Orders;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:5173")
public class OrdersPage {

    @Autowired
    private PostgresService postgresService;

    // 📌 API: Get Orders with all filters
    @GetMapping("/filtered")
    public ResponseEntity<?> getFilteredOrders(
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "orderType", required = false) String orderType,
            @RequestParam(name = "paymentMethod", required = false) String paymentMethod,
            @RequestParam(name = "carrier", required = false) String carrier,
            @RequestParam(name = "searchCustomer", required = false) String searchCustomer,
            @RequestParam(name = "searchOrderId", required = false) String searchOrderId
    ) {
        try {
            // Prepare the SQL to call the stored procedure
            String query = String.format("""
                SELECT * FROM get_filtered_orders(
                    %s, %s, %s, %s, %s, %s, %s, %s
                )
            """,
                startDate != null ? "TIMESTAMP '" + startDate + "'" : "NULL",
                endDate != null ? "TIMESTAMP '" + endDate + "'" : "NULL",
                status != null ? "'" + status + "'" : "NULL",
                orderType != null ? "'" + orderType + "'" : "NULL",
                paymentMethod != null ? "'" + paymentMethod + "'" : "NULL",
                carrier != null ? "'" + carrier + "'" : "NULL",
                searchCustomer != null ? "'" + searchCustomer + "'" : "NULL",
                searchOrderId != null ? "'" + searchOrderId + "'" : "NULL"
            );

            List<Map<String, Object>> results = postgresService.query(query);

            if (results.isEmpty()) {
                return ResponseEntity.ok(Collections.singletonMap("message", "No orders found."));
            }

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to fetch filtered orders"));
        }
    }
}
