package EdTech.DTO;

import java.time.LocalDateTime;

public record PurchaseDTO(Integer transactionId, Integer productId, String productName,
                          String category, String imageUrl, String type, Double amountPaid,
                          LocalDateTime purchasedAt, LocalDateTime rentalExpiry, boolean active) {}
