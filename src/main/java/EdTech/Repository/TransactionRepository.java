package EdTech.Repository;

import EdTech.Entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    // Finds all purchases OR active rentals that haven't expired yet
    @Query("SELECT t.productId FROM TransactionEntity t WHERE t.userId = :userId AND " +
            "(t.type = 'PURCHASE' OR (t.type = 'RENT' AND t.rentalExpiryDate > :now))")
    List<Long> findActiveProductIdsByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
