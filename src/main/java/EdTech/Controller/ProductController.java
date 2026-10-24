package EdTech.Controller;

import EdTech.DTO.PaymentRequestDTO;
import EdTech.DTO.ProductDTO;
import EdTech.Services.ProductServices;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductServices productServices;

    // 1. Add Product
    @PostMapping("/add")
    public ResponseEntity<ProductDTO> addProduct(@RequestBody ProductDTO dto) {
        ProductDTO savedProduct = productServices.addProduct(dto);
        return new ResponseEntity<>(savedProduct, HttpStatus.CREATED);
    }

    // 2. View All Products
    @GetMapping("/all")
    public ResponseEntity<List<ProductDTO>> viewAllProduct() {
        List<ProductDTO> products = productServices.viewAllProduct();
        return ResponseEntity.ok(products);
    }

    // 3. Search Products by Optional Parameters
    @GetMapping("/search")
    public ResponseEntity<List<ProductDTO>> viewProductByParameter(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {

        List<ProductDTO> products = productServices.viewProductByParameter(name, category, minPrice, maxPrice);
        return ResponseEntity.ok(products);
    }

    // 4. Create Order (Purchase/Rent & Process Dummy Payment)
    @PostMapping("/checkout")
    public ResponseEntity<String> checkout(@RequestBody PaymentRequestDTO request) {
        try {
            productServices.createOrder(request);
            return ResponseEntity.ok("Order processed successfully. Resource added to your dashboard.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // 5. Update Product Details (Owner Protected)
    @PutMapping("/update/{productId}")
    public ResponseEntity<?> updateProduct(
            @PathVariable Integer productId,
            @RequestBody ProductDTO updatedDto,
            @RequestHeader("X-User-Id") Integer requestingUserId) {
        try {
            ProductDTO product = productServices.updateProduct(productId, updatedDto, requestingUserId);
            return ResponseEntity.ok(product);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    // 6. Delete Product (Owner Protected)
    @DeleteMapping("/delete/{productId}")
    public ResponseEntity<String> deleteProduct(
            @PathVariable Integer productId,
            @RequestHeader("X-User-Id") Integer requestingUserId) {
        try {
            productServices.deleteProduct(productId, requestingUserId);
            return ResponseEntity.ok("Product deleted successfully.");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}
