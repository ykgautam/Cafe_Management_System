package com.inn.cafe.rest;

import com.inn.cafe.POJO.Bill;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequestMapping(path = "/bill")
public interface BillRest {

    @PostMapping(value = "/generateReport")
    public ResponseEntity<String> generateReport(@RequestBody Map<String, Object> requestMap);

    @GetMapping(value = "/getBills")
    public ResponseEntity<List<Bill>> getBills();

    @PostMapping(value = "/getPdf")
    public ResponseEntity<byte[]> getPdf(@RequestBody Map<String, Object> requestMap);

    @PostMapping(value = "/delete/{id}")
    public ResponseEntity<String> deleteBill(@PathVariable Integer id);
}
