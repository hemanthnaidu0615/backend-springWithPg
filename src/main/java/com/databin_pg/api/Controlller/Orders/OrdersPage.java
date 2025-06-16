package com.databin_pg.api.Controlller.Orders;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrdersPage {

    @Autowired
    private PostgresService postgresService;

    // 📌 API: Get Orders with all filters including enterpriseKey + pagination
    @GetMapping("/filtered")
    public ResponseEntity<?> getFilteredOrders(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "orderType", required = false) String orderType,
            @RequestParam(name = "paymentMethod", required = false) String paymentMethod,
            @RequestParam(name = "carrier", required = false) String carrier,
            @RequestParam(name = "searchCustomer", required = false) String searchCustomer,
            @RequestParam(name = "searchOrderId", required = false) String searchOrderId,
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            int offset = page * size;

            // Build filter arguments for query
            String[] args = new String[]{
                startDate != null ? "TIMESTAMP '" + startDate + "'" : "NULL",
                endDate != null ? "TIMESTAMP '" + endDate + "'" : "NULL",
                status != null ? "'" + status + "'" : "NULL",
                orderType != null ? "'" + orderType + "'" : "NULL",
                paymentMethod != null ? "'" + paymentMethod + "'" : "NULL",
                carrier != null ? "'" + carrier + "'" : "NULL",
                searchCustomer != null ? "'" + searchCustomer + "'" : "NULL",
                searchOrderId != null ? "'" + searchOrderId + "'" : "NULL",
                enterpriseKey != null ? "'" + enterpriseKey + "'" : "NULL"
            };

            // Count query
            String countQuery = String.format("""
            	    SELECT COUNT(*) as total FROM get_filtered_orders(
            	        %s, %s, %s, %s, %s, %s, %s, %s, %s
            	    )
            	""", (Object[]) args);

            List<Map<String, Object>> countResult = postgresService.query(countQuery);
            int totalCount = 0;
            if (!countResult.isEmpty() && countResult.get(0).get("total") != null) {
                totalCount = ((Number) countResult.get(0).get("total")).intValue();
            }

            // Paginated data query
            String dataQuery = String.format("""
            	    SELECT * FROM get_filtered_orders(
            	        %s, %s, %s, %s, %s, %s, %s, %s, %s
            	    )
            	    OFFSET %d LIMIT %d
            	""", (Object[]) Arrays.copyOf(args, args.length + 2));

            List<Map<String, Object>> results = postgresService.query(dataQuery);

            return ResponseEntity.ok(Map.of(
                    "data", results,
                    "page", page,
                    "size", size,
                    "count", totalCount
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch filtered orders", "details", e.getMessage()));
        }
    }
}
