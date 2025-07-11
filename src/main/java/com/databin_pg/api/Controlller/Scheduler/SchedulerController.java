package com.databin_pg.api.Controlller.Scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.databin_pg.api.DTO.QuerySchedulerRequest;
import com.databin_pg.api.DTO.QueryValidationRequest;
import com.databin_pg.api.DTO.TableSchedulerRequest;
import com.databin_pg.api.Service.SchedulerService;

@RestController
@RequestMapping("/api/schedulers")
@CrossOrigin(origins = "*")
public class SchedulerController {

    @Autowired
    private SchedulerService schedulerService;

    @PostMapping("/create-table-scheduler")
    public ResponseEntity<?> createTableScheduler(@RequestBody TableSchedulerRequest request) {
        schedulerService.saveTableScheduler(request);
        return ResponseEntity.ok("Scheduler created");
    }

    @PostMapping("/create-query-scheduler")
    public ResponseEntity<?> createQueryScheduler(@RequestBody QuerySchedulerRequest request) {
        schedulerService.saveQueryScheduler(request);
        return ResponseEntity.ok("Scheduler created");
    }
    
    @PostMapping("/validate-query")
    public ResponseEntity<?> validateQuery(@RequestBody QueryValidationRequest request) {
        boolean isValid = schedulerService.validateSQLQuery(request.query);
        if (isValid) {
            return ResponseEntity.ok("Query is valid");
        } else {
            return ResponseEntity.badRequest().body("Invalid query: syntax error or missing table/column");
        }
    }


}

