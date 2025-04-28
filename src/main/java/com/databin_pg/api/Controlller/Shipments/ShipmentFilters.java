//package com.databin_pg.api.Controlller.Shipments;
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
//@RequestMapping("/api/shipment-filters")
//@CrossOrigin(origins = "http://localhost:5173")
//public class ShipmentFilters {
//
//    @Autowired
//    private PostgresService postgresService;
//
//    // 📌 API: Get unique values for shipment filters (carrier, shipping_method)
//    @GetMapping("/filter-values")
//    public ResponseEntity<?> getShipmentFilterValues() {
//        try {
//            String query = "SELECT * FROM get_shipment_filters()";
//
//            List<Map<String, Object>> results = postgresService.query(query);
//
//            Set<String> carriers = new HashSet<>();
//            Set<String> shippingMethods = new HashSet<>();
//
//            for (Map<String, Object> row : results) {
//                if (row.get("carrier") != null) {
//                    carriers.add(row.get("carrier").toString());
//                }
//                if (row.get("shipping_method") != null) {
//                    shippingMethods.add(row.get("shipping_method").toString());
//                }
//            }
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("carriers", carriers);
//            response.put("shipping_methods", shippingMethods);
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(Collections.singletonMap("error", "Failed to fetch shipment filter options"));
//        }
//    }
//}
