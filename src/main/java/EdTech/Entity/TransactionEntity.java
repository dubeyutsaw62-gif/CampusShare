package EdTech.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transactions")
public class TransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer userId; // The buyer/renter
    private Integer productId; // The resource being accessed

    @Enumerated(EnumType.STRING)
    private TransactionType type; // PURCHASE or RENT

    private Double amountPaid;
    private LocalDateTime transactionDate;
    private LocalDateTime rentalExpiryDate; // null if it's a permanent purchase

    // Getters, Setters, and Constructors
}

