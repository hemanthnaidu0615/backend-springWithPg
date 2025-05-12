package com.databin_pg.api.DTO;

import java.time.LocalDateTime;

public class QuerySchedulerRequest {
    public String title;
    public String description;
    public LocalDateTime startDate;
    public LocalDateTime endDate;
    public String email;
    public String bcc;
    public String recurrencePattern;
    public String customQuery;
}
