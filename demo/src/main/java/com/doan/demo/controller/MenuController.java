package com.doan.demo.controller;

import com.doan.demo.model.MenuItem;
import com.doan.demo.repository.MenuItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/menu")
@CrossOrigin(origins = "*")
public class MenuController {

    @Autowired private MenuItemRepository menuItemRepository;

    /** GET /api/menu — App lấy danh sách món đang bán (có imageUrl) */
    @GetMapping
    public List<MenuItem> getActiveMenu() {
        return menuItemRepository.findByActiveTrueOrderByIdAsc();
    }

    /** GET /api/menu/category/{cat} — App lọc theo danh mục */
    @GetMapping("/category/{category}")
    public List<MenuItem> getByCategory(@PathVariable String category) {
        return menuItemRepository.findByCategoryAndActiveTrueOrderByIdAsc(category);
    }

    /** GET /api/menu/all — Admin dùng (cả món tắt) */
    @GetMapping("/all")
    public List<MenuItem> getAll() {
        return menuItemRepository.findAll();
    }

    /** POST /api/menu — Admin thêm món mới (kèm imageUrl) */
    @PostMapping
    public ResponseEntity<MenuItem> createItem(@RequestBody MenuItem item) {
        return ResponseEntity.ok(menuItemRepository.save(item));
    }

    /** PUT /api/menu/{id} — Admin cập nhật toàn bộ thông tin món */
    @PutMapping("/{id}")
    public ResponseEntity<MenuItem> updateItem(@PathVariable Long id,
                                               @RequestBody MenuItem updated) {
        Optional<MenuItem> opt = menuItemRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        MenuItem item = opt.get();
        if (updated.getName()     != null) item.setName(updated.getName());
        if (updated.getPrice()    != null) item.setPrice(updated.getPrice());
        if (updated.getEmoji()    != null) item.setEmoji(updated.getEmoji());
        if (updated.getCategory() != null) item.setCategory(updated.getCategory());
        if (updated.getBadge()    != null) item.setBadge(updated.getBadge());
        if (updated.getImageUrl() != null) item.setImageUrl(updated.getImageUrl());
        item.setActive(updated.isActive());

        return ResponseEntity.ok(menuItemRepository.save(item));
    }

    /** PATCH /api/menu/{id}/image — Chỉ cập nhật URL ảnh */
    @PatchMapping("/{id}/image")
    public ResponseEntity<String> updateImage(@PathVariable Long id,
                                              @RequestBody Map<String, String> body) {
        Optional<MenuItem> opt = menuItemRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        MenuItem item = opt.get();
        item.setImageUrl(body.get("imageUrl"));
        menuItemRepository.save(item);
        return ResponseEntity.ok("Cập nhật ảnh thành công: " + item.getName());
    }

    /** DELETE /api/menu/{id} — Admin xóa món */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteItem(@PathVariable Long id) {
        if (!menuItemRepository.existsById(id)) return ResponseEntity.notFound().build();
        menuItemRepository.deleteById(id);
        return ResponseEntity.ok("Đã xóa món #" + id);
    }
}