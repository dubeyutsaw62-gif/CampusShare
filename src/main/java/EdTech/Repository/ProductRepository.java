package EdTech.Repository;

import EdTech.Entity.ProductEntity;
import EdTech.Entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity,Integer> {
    @Query("SELECT p FROM ProductEntity p WHERE " +
            "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:category IS NULL OR LOWER(p.category) = LOWER(:category)) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    List<ProductEntity> searchProducts(
            @Param("name") String name,
            @Param("category") String category,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice
    );
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProductEntity p SET p.quantity = p.quantity - 1 WHERE p.id = :id AND p.quantity > 0")
    int decrementStock(@Param("id") Integer id);
}
