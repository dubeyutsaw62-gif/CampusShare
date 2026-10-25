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
    private String imageUrl;        // e.g. /api/products/5/image, or null
    private Integer createdByUserId;
}