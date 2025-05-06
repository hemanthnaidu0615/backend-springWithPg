//package com.databin_pg.api.Controlller.Fulfillment;
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
//@RequestMapping("/api/fulfillment")
//@CrossOrigin(origins = "http://localhost:5173")
//public class OrdersInProcessController {
//
//    @Autowired
//    private PostgresService postgresService;
//
//    // 📌 API: Get Orders In Process with status and ETA
//    @GetMapping("/orders-in-process")
//    public ResponseEntity<?> getOrdersInProcess(
//            @RequestParam(name = "startDate") String startDate,
//            @RequestParam(name = "endDate") String endDate,
//            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey) {
//
//        try {
//            String formattedKey = (enterpriseKey == null || enterpriseKey.isBlank()) ? "NULL" : "'" + enterpriseKey + "'";
//            String formattedStartDate = startDate.split("T")[0];
//            String formattedEndDate = endDate.split("T")[0];
//
//            String query = String.format("""
//                SELECT * FROM get_orders_in_process('%s', '%s', %s)
//            """, formattedStartDate, formattedEndDate, formattedKey);
//
//            List<Map<String, Object>> data = postgresService.query(query);
//            return ResponseEntity.ok(data);
//
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(Map.of("error", "Failed to fetch orders in process", "details", e.getMessage()));
//        }
//    }
//}




package com.databin_pg.api.Controlller.Fulfillment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.databin_pg.api.Service.PostgresService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fulfillment")
@CrossOrigin(origins = "http://localhost:5173")
public class OrdersInProcessController {

    @Autowired
    private PostgresService postgresService;

    // 📌 API: Get Orders In Process with status and ETA
    @GetMapping("/orders-in-process")
    public ResponseEntity<?> getOrdersInProcess(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int size) {

        try {
            String formattedKey = (enterpriseKey == null || enterpriseKey.isBlank()) ? "NULL" : "'" + enterpriseKey + "'";
            String formattedStartDate = startDate.split("T")[0];
            String formattedEndDate = endDate.split("T")[0];

            int offset = page * size;

            String query = String.format("""
                SELECT * FROM get_orders_in_process('%s', '%s', %s)
                OFFSET %d LIMIT %d
            """, formattedStartDate, formattedEndDate, formattedKey, offset, size);

            List<Map<String, Object>> data = postgresService.query(query);

            return ResponseEntity.ok(Map.of(
                "data", data,
                "page", page,
                "size", size,
                "count", data.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch orders in process", "details", e.getMessage()));
        }
    }

}

