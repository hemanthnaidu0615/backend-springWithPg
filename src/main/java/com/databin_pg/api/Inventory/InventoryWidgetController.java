//package com.databin_pg.api.Inventory;
//
//import com.databin_pg.api.Service.PostgresService;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.http.HttpStatus;
//
//import java.util.*;
//
//@RestController
//@RequestMapping("/api/inventory")
//@CrossOrigin(origins = "[http://localhost:5173](http://localhost:5173)")
//public class InventoryWidgetController {
//
//
//@Autowired
//private PostgresService postgresService;
//
//@GetMapping("/widget-data")
//public ResponseEntity<?> getInventoryWidgetData(
//        @RequestParam(name = "startDate") String startDate,
//        @RequestParam(name = "endDate") String endDate,
//        @RequestParam(name = "searchProduct", required = false) String searchProduct,
//        @RequestParam(name = "statusFilter", required = false) String statusFilter,
//        @RequestParam(name = "categoryFilter", required = false) String categoryFilter) {
//
//    try {
//        String query = String.format("""
//            SELECT * FROM get_inventory_widget_data(
//                '%s'::date,
//                '%s'::date,
//                %s,
//                %s,
//                %s
//            )
//            """,
//            startDate,
//            endDate,
//            searchProduct == null ? "NULL" : "'" + searchProduct + "'",
//            statusFilter == null ? "NULL" : "'" + statusFilter + "'",
//            categoryFilter == null ? "NULL" : "'" + categoryFilter + "'"
//        );
//
//        List<Map<String, Object>> result = postgresService.query(query);
//
//        List<Map<String, Object>> widgetData = new ArrayList<>(result.size());
//        for (Map<String, Object> row : result) {
//            widgetData.add(Map.of(
//                    "product_name", row.get("product_name"),
//                    "category_name", row.get("category_name"),
//                    "warehouse_name", row.get("warehouse_name"),
//                    "warehouse_function", row.get("warehouse_function"),
//                    "warehouse_state", row.get("warehouse_state"),
//                    "inventory_status", row.get("inventory_status")
//            ));
//        }
//
//        return ResponseEntity.ok(widgetData);
//
//    } catch (Exception e) {
//        e.printStackTrace();
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(Map.of("error", "Failed to fetch widget data"));
//    }
//}
//
//}








package com.databin_pg.api.Inventory;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "http://localhost:5173")
public class InventoryWidgetController {

    @Autowired
    private PostgresService postgresService;

    @GetMapping("/widget-data")
    public ResponseEntity<?> getInventoryWidgetData(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "searchProduct", required = false) String searchProduct,
            @RequestParam(name = "statusFilter", required = false) String statusFilter,
            @RequestParam(name = "categoryFilter", required = false) String categoryFilter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        try {
            int offset = page * size;

            String query = String.format("""
                SELECT * FROM get_inventory_widget_data(
                    '%s'::date,
                    '%s'::date,
                    %s,
                    %s,
                    %s
                )
                OFFSET %d LIMIT %d
                """,
                startDate,
                endDate,
                searchProduct == null ? "NULL" : "'" + searchProduct + "'",
                statusFilter == null ? "NULL" : "'" + statusFilter + "'",
                categoryFilter == null ? "NULL" : "'" + categoryFilter + "'",
                offset,
                size
            );

            List<Map<String, Object>> result = postgresService.query(query);

            List<Map<String, Object>> widgetData = new ArrayList<>(result.size());
            for (Map<String, Object> row : result) {
                widgetData.add(Map.of(
                        "product_name", row.get("product_name"),
                        "category_name", row.get("category_name"),
                        "warehouse_name", row.get("warehouse_name"),
                        "warehouse_function", row.get("warehouse_function"),
                        "warehouse_state", row.get("warehouse_state"),
                        "inventory_status", row.get("inventory_status")
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "data", widgetData,
                    "page", page,
                    "size", size,
                    "count", widgetData.size()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch widget data", "details", e.getMessage()));
        }
    }
}
