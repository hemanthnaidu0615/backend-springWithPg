//package com.databin_pg.api.Controlller.Sales.Dashboard;
//
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.http.HttpStatus;
//import org.springframework.web.bind.annotation.*;
//
//import com.databin_pg.api.Service.PostgresService;
//
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/sales")
//@CrossOrigin(origins = "http://localhost:5173")
//
//public class SalesByDashboardKPIController {
//	@Autowired
//    private PostgresService postgresService;
//
//    @GetMapping("/sales-kpis")
//    public ResponseEntity<?> getSalesKPIs(
//            @RequestParam(name = "startDate") String startDate,
//            @RequestParam(name = "endDate") String endDate) {
//
//        try {
//            String query = String.format("""
//                SELECT * FROM get_sales_by_analysis_kpis('%s'::timestamp, '%s'::timestamp)
//            """, startDate, endDate);
//
//            List<Map<String, Object>> result = postgresService.query(query);
//
//            if (result.isEmpty()) {
//                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(Map.of("message", "No KPI data found."));
//            }
//
//            Map<String, Object> row = result.get(0);
//
//            Map<String, Object> kpiResponse = Map.of(
//                "total_sales", row.get("total_sales"),
//                "total_orders", row.get("total_orders"),
//                "avg_order_value", row.get("avg_order_value"),
//                "total_taxes", row.get("total_taxes"),
//                "total_shipping_fees", row.get("total_shipping_fees")
//            );
//
//            return ResponseEntity.ok(kpiResponse);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(Map.of("error", "Failed to fetch sales KPIs"));
//        }
//    }
//}






package com.databin_pg.api.Controlller.Sales.Dashboard;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;

@RestController
@RequestMapping("/api/sales")
@CrossOrigin(origins = "http://localhost:5173")
public class SalesByDashboardKPIController {

    @Autowired
    private PostgresService postgresService;

    @GetMapping("/sales-kpis")
    public ResponseEntity<?> getSalesKPIs(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey) {

        try {
            // Format optional enterpriseKey
            String formattedKey = (enterpriseKey == null || enterpriseKey.isBlank()) ? "NULL" : "'" + enterpriseKey + "'";

            // Build the query calling the stored procedure with 3 parameters
            String query = String.format("""
                SELECT * FROM get_sales_by_analysis_kpis('%s'::timestamp, '%s'::timestamp, %s)
            """, startDate, endDate, formattedKey);

            List<Map<String, Object>> result = postgresService.query(query);

            if (result.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(Map.of("message", "No KPI data found."));
            }

            Map<String, Object> row = result.get(0);

            Map<String, Object> kpiResponse = Map.of(
                    "total_sales", row.get("total_sales"),
                    "total_orders", row.get("total_orders"),
                    "avg_order_value", row.get("avg_order_value"),
                    "total_taxes", row.get("total_taxes"),
                    "total_shipping_fees", row.get("total_shipping_fees")
            );

            return ResponseEntity.ok(kpiResponse);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch sales KPIs", "details", e.getMessage()));
        }
    }
}


