package EdTech.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class ProductEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    private String category;
    private int quantity;
    private double price;
    private String purpose;
    @Lob // Tells Hibernate to create a BLOB/LONGBLOB column datatype
    @Column(length = 1000000) // Allocates enough storage space for large files
    private byte[] imageBytes;
    private String imageContentType; // e.g. image/png
    private Integer createdByUserId;
}