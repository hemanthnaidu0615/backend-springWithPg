package com.databin_pg.api.Controlller.Orders;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderDetailsController {

    @Autowired
    private PostgresService postgresService;

    // 📌 API: Get Full Order Details by Order ID
    @GetMapping("/{orderId}/details")
    public ResponseEntity<?> getOrderDetails(@PathVariable("orderId") int orderId) {
        try {
            String query = String.format("SELECT get_order_details(%d);", orderId);
            List<Map<String, Object>> result = postgresService.query(query);

            if (result.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("message", "Order not found"));
            }

            // The result is in the form of a single JSON column
            return ResponseEntity.ok(result.get(0).get("get_order_details"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to fetch order details"));
        }
    }
}
