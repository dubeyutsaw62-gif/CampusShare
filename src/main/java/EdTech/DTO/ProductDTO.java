package EdTech.DTO;

import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDTO {
    private Integer id;
    private String name;
    private String category;
    private int quantity;
    private double price;
    private String purpose;
    @Lob // Tells Hibernate to create a BLOB/LONGBLOB column datatype
    @Column(length = 1000000) // Allocates enough storage space for large files
    private byte[] imageBytes;
}
