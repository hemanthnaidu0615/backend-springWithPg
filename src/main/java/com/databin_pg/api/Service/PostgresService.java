package com.databin_pg.api.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PostgresService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Method to execute SQL that doesn't return results (used for calling stored procedures)
    public void execute(String sql) {
        jdbcTemplate.execute(sql);
    }

    // Method to query and return a list of results
    public List<Map<String, Object>> query(String sql) {
        return jdbcTemplate.queryForList(sql);
    }
}
