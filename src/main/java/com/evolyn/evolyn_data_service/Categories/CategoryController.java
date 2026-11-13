package com.evolyn.evolyn_data_service.Categories;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.evolyn.evolyn_data_service.Categories.CategoryService.CategoryException;
import com.evolyn.evolyn_data_service.Categories.CategoryService.CategoryInput;
import com.evolyn.evolyn_data_service.Categories.CategoryService.CategoryView;

/**
 * Internal endpoints (only Evolyn-API calls these):
 *   GET    /categories?userId=                        list (creates the defaults on first visit)
 *   POST   /categories                                create
 *   PUT    /categories/{id}                           update
 *   DELETE /categories/{id}?userId=&reassignTo=       delete, moving its transactions first
 */
@RestController
@RequestMapping(value = "/evolyn/api/internal/categories", produces = "application/json")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategoryView> list(@RequestParam("userId") UUID userId) {
        return service.list(userId);
    }

    @PostMapping
    public ResponseEntity<CategoryView> create(@RequestBody CategoryInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(input));
    }

    @PutMapping("/{categoryId}")
    public CategoryView update(@PathVariable UUID categoryId, @RequestBody CategoryInput input) {
        return service.update(categoryId, input);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> delete(@PathVariable UUID categoryId,
                                       @RequestParam("userId") UUID userId,
                                       @RequestParam(value = "reassignTo", required = false) UUID reassignTo) {
        service.delete(categoryId, userId, reassignTo);
        return ResponseEntity.noContent().build();
    }

    // Rule violations from the service become a clean JSON error with the right status
    @ExceptionHandler(CategoryException.class)
    public ResponseEntity<Object> handle(CategoryException e) {
        return ResponseEntity.status(e.status).body(e.body);
    }
}
