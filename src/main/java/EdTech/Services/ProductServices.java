package EdTech.Services;

import EdTech.DTO.PaymentRequestDTO;
import EdTech.DTO.ProductDTO;
import EdTech.DTO.PurchaseDTO;
import EdTech.Entity.ProductEntity;
import EdTech.Entity.TransactionEntity;
import EdTech.Entity.TransactionType;
import EdTech.Repository.ProductRepository;
import EdTech.Repository.TransactionRepository;
import java.io.IOException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.AccessDeniedException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServices {
    private static final long MAX_IMAGE_BYTES = 2L * 1024 * 1024;

    private final ProductRepository repository;
    private final TransactionRepository transactionRepository;

    public ProductDTO addProduct(String name, String category, int quantity, double price,
                                 String purpose, MultipartFile image, Integer ownerId) {
        ProductEntity e = new ProductEntity();
        e.setName(name);
        e.setCategory(category);
        e.setQuantity(quantity);
        e.setPrice(price);
        e.setPurpose(purpose);
        e.setCreatedByUserId(ownerId);          // owner comes from the JWT, never from the client
        applyImage(e, image);
        return toDto(repository.save(e));
    }

    public List<ProductDTO> viewAllProduct() {
        return repository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<ProductDTO> viewProductByParameter(String name, String category,
                                                   Double minPrice, Double maxPrice) {
        return repository.searchProducts(name, category, minPrice, maxPrice)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public ProductEntity getProductWithImage(Integer id) {
        ProductEntity p = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + id));
        if (p.getImageBytes() == null) {
            throw new EntityNotFoundException("This product has no image.");
        }
        return p;
    }

    public ProductDTO updateProduct(Integer id, String name, String category, Integer quantity,
                                    Double price, String purpose, MultipartFile image, Integer userId) {
        ProductEntity e = findOwned(id, userId);
        if (name != null) e.setName(name);
        if (category != null) e.setCategory(category);
        if (quantity != null) e.setQuantity(quantity);
        if (price != null) e.setPrice(price);
        if (purpose != null) e.setPurpose(purpose);
        applyImage(e, image);                    // only replaces the image if a new one was sent
        return toDto(repository.save(e));
    }

    public void deleteProduct(Integer id, Integer userId) {
        repository.delete(findOwned(id, userId));
    }

    // ---- payment (unchanged from your version) ----
    public boolean processPayment(PaymentRequestDTO request) {
        if (request.getCardNumber() == null || request.getCardNumber().length() < 16) {
            throw new IllegalArgumentException("Invalid card details. Payment failed.");
        }
        return true;
    }

    private static final double RENT_RATE = 0.30; // rent for 30 days = 30% of price

    @Transactional
    public void createOrder(PaymentRequestDTO request) {
        processPayment(request);

        if (request.getProductId() == null) {
            throw new IllegalArgumentException("Product is required.");
        }
        ProductEntity product = repository.findById(request.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found."));
        if (Objects.equals(product.getCreatedByUserId(), request.getUserId())) {
            throw new IllegalArgumentException("You can't buy your own listing.");
        }
        if (repository.decrementStock(product.getId()) == 0) {
            throw new IllegalStateException("Sorry, this item just sold out.");
        }

        boolean rent = "RENT".equalsIgnoreCase(request.getTransactionType());
        TransactionEntity t = new TransactionEntity();
        t.setUserId(request.getUserId());
        t.setProductId(product.getId());
        t.setTransactionDate(LocalDateTime.now());
        if (rent) {
            t.setType(TransactionType.RENT);
            t.setAmountPaid(Math.round(product.getPrice() * RENT_RATE * 100) / 100.0); // server decides the price
            t.setRentalExpiryDate(LocalDateTime.now().plusDays(30));
        } else {
            t.setType(TransactionType.PURCHASE);
            t.setAmountPaid(product.getPrice());
        }
        transactionRepository.save(t);   // if this fails, the stock decrement rolls back too
    }

    // ---- helpers ----
    private ProductEntity findOwned(Integer id, Integer userId) {
        ProductEntity e = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + id));
        if (!Objects.equals(e.getCreatedByUserId(), userId)) {   // null-safe, no more NPE
            throw new AccessDeniedException("You do not own this product.");
        }
        return e;
    }
    public List<PurchaseDTO> getMyPurchases(Integer userId) {
        List<TransactionEntity> txs = transactionRepository.findByUserIdOrderByTransactionDateDesc(userId);
        Map<Integer, ProductEntity> products = repository.findAllById(
                        txs.stream().map(TransactionEntity::getProductId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(ProductEntity::getId, p -> p));
        LocalDateTime now = LocalDateTime.now();
        return txs.stream().map(t -> {
            ProductEntity p = products.get(t.getProductId());
            boolean active = t.getType() == TransactionType.PURCHASE
                    || (t.getRentalExpiryDate() != null && t.getRentalExpiryDate().isAfter(now));
            return new PurchaseDTO(t.getId(), t.getProductId(),
                    p != null ? p.getName() : "(listing removed)",
                    p != null ? p.getCategory() : null,
                    p != null && p.getImageBytes() != null ? "/api/products/" + p.getId() + "/image" : null,
                    t.getType().name(), t.getAmountPaid(), t.getTransactionDate(), t.getRentalExpiryDate(), active);
        }).collect(Collectors.toList());
    }
    private void applyImage(ProductEntity e, MultipartFile image) {
        if (image == null || image.isEmpty()) return;
        if (image.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Image must be 2 MB or smaller.");
        }
        try {
            byte[] bytes = image.getBytes();
            String type = detectType(bytes);     // checks real file bytes, not the client's claim
            if (type == null) {
                throw new IllegalArgumentException("Only JPEG, PNG or WebP images are allowed.");
            }
            e.setImageBytes(bytes);
            e.setImageContentType(type);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private String detectType(byte[] b) {
        if (b.length < 12) return null;
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "image/jpeg";
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "image/png";
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "image/webp";
        return null;
    }

    private ProductDTO toDto(ProductEntity e) {
        String url = e.getImageBytes() != null ? "/api/products/" + e.getId() + "/image" : null;
        return new ProductDTO(e.getId(), e.getName(), e.getCategory(), e.getQuantity(),
                e.getPrice(), e.getPurpose(), url, e.getCreatedByUserId());
    }
}