package EdTech.Controller;

import EdTech.DTO.PaymentRequestDTO;
import EdTech.DTO.ProductDTO;
import EdTech.DTO.PurchaseDTO;
import EdTech.Entity.ProductEntity;
import EdTech.Services.ProductServices;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/products")
@AllArgsConstructor
public class ProductController {
    private final ProductServices productServices;

    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDTO> addProduct(
            @RequestParam String name,
            @RequestParam String category,
            @RequestParam int quantity,
            @RequestParam double price,
            @RequestParam(required = false) String purpose,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal Integer userId) {
        return new ResponseEntity<>(
                productServices.addProduct(name, category, quantity, price, purpose, image, userId),
                HttpStatus.CREATED);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ProductDTO>> viewAllProduct() {
        return ResponseEntity.ok(productServices.viewAllProduct());
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductDTO>> viewProductByParameter(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {
        return ResponseEntity.ok(productServices.viewProductByParameter(name, category, minPrice, maxPrice));
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable Integer id) {
        ProductEntity p = productServices.getProductWithImage(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(p.getImageContentType()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                .body(p.getImageBytes());
    }

    @PostMapping("/checkout")
    public ResponseEntity<String> checkout(@RequestBody PaymentRequestDTO request,
                                           @AuthenticationPrincipal Integer userId) {
        request.setUserId(userId);               // buyer is whoever the token says, not the body
        productServices.createOrder(request);
        return ResponseEntity.ok("Order processed successfully. Resource added to your dashboard.");
    }

    @PutMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDTO> updateProduct(
            @PathVariable Integer id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer quantity,
            @RequestParam(required = false) Double price,
            @RequestParam(required = false) String purpose,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal Integer userId) {
        return ResponseEntity.ok(
                productServices.updateProduct(id, name, category, quantity, price, purpose, image, userId));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable Integer id,
                                                @AuthenticationPrincipal Integer userId) {
        productServices.deleteProduct(id, userId);
        return ResponseEntity.ok("Product deleted successfully.");
    }
    @GetMapping("/my-purchases")
    public ResponseEntity<List<PurchaseDTO>> myPurchases(@AuthenticationPrincipal Integer userId) {
        return ResponseEntity.ok(productServices.getMyPurchases(userId));
    }
}