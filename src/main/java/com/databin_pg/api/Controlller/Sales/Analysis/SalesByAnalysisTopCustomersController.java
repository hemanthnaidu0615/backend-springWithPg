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
public class SalesByAnalysisTopCustomersController {

    @Autowired
    private PostgresService postgresService;

    @GetMapping("/customer-order-summary")
    public ResponseEntity<?> getCustomerOrderSummary(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        try {
            String formattedEnterpriseKey = (enterpriseKey == null || enterpriseKey.isBlank())
                    ? "NULL" : "'" + enterpriseKey + "'";

            String formattedStartDate = startDate.split("T")[0];
            String formattedEndDate = endDate.split("T")[0];
            int offset = page * size;

            // Count query
            String countQuery = String.format("""
                SELECT COUNT(*) AS total FROM get_customer_order_summary('%s'::timestamp, '%s'::timestamp, %s)
            """, formattedStartDate, formattedEndDate, formattedEnterpriseKey);

            List<Map<String, Object>> countResult = postgresService.query(countQuery);
            int totalCount = 0;
            if (!countResult.isEmpty() && countResult.get(0).get("total") != null) {
                totalCount = ((Number) countResult.get(0).get("total")).intValue();
            }

            // Paginated data query
            String dataQuery = String.format("""
                SELECT * FROM get_customer_order_summary('%s'::timestamp, '%s'::timestamp, %s)
                OFFSET %d LIMIT %d
            """, formattedStartDate, formattedEndDate, formattedEnterpriseKey, offset, size);

            List<Map<String, Object>> result = postgresService.query(dataQuery);

            List<Map<String, Object>> customerData = new ArrayList<>();
            for (Map<String, Object> row : result) {
                Map<String, Object> customer = Map.of(
                        "customer_name", row.get("customer_name"),
                        "total_orders", row.get("total_orders"),
                        "total_spent", row.get("total_spent")
                );
                customerData.add(customer);
            }

            return ResponseEntity.ok(Map.of(
                    "customers", customerData,
                    "page", page,
                    "size", size,
                    "count", totalCount
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch customer order summary", "details", e.getMessage()));
        }
    }
}