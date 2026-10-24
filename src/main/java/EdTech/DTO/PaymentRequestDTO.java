package EdTech.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequestDTO {
    private Integer userId;
    private Integer productId;
    private String transactionType; // "PURCHASE" or "RENT"
    private Double amount;

    // Dummy Payment Fields
    private String cardNumber;
    private String cvv;

    // Getters and Setters
}
