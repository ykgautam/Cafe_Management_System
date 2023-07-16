package com.inn.cafe.rest;

import com.inn.cafe.wrapper.ProductWrapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequestMapping(path = "/product")
public interface ProductRest {

    @PostMapping(path = "/add")
    public ResponseEntity<String> addNewProduct(@RequestBody Map<String, String> requestMap);

    @GetMapping(path = "/get")
    public ResponseEntity<List<ProductWrapper>> getAllProduct();

    @PostMapping(path = "/update")
    public ResponseEntity<String> updateProduct(@RequestBody Map<String, String> requestMap);

    @DeleteMapping(path = "/delete/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable Integer id);

    @PostMapping(path = "/updateStatus")
    public ResponseEntity<String> updateProductStatus(@RequestBody Map<String, String> requestMap);

    @GetMapping(path = "/getByCategory/{id}")
    ResponseEntity<List<ProductWrapper>> getProductByCategory(@PathVariable Integer id);

    @GetMapping(path = "/getById/{id}")
    ResponseEntity<List<ProductWrapper>> getProductById(@PathVariable Integer id);

}
