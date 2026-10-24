package EdTech.Services;

import EdTech.DTO.PaymentRequestDTO;
import EdTech.DTO.ProductDTO;
import EdTech.Entity.ProductEntity;
import EdTech.Entity.TransactionEntity;
import EdTech.Entity.TransactionType;
import EdTech.Repository.ProductRepository;
import EdTech.Repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServices {
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private ProductRepository repository;
    @Autowired
    private TransactionRepository transactionRepository;

    public ProductDTO addProduct(ProductDTO dto){
        ProductEntity entity = modelMapper.map(dto, ProductEntity.class);
        ProductEntity savedEntity = repository.save(entity);
        return modelMapper.map(savedEntity, ProductDTO.class);
    }
    public List<ProductDTO> viewAllProduct(){
        List<ProductEntity> entities = repository.findAll();
        return entities.stream()
                .map(entity -> modelMapper.map(entity, ProductDTO.class))
                .collect(Collectors.toList());
    }
    public List<ProductDTO> viewProductByParameter(String name, String category, Double minPrice, Double maxPrice){
        List<ProductEntity> entities = repository.searchProducts(name, category, minPrice, maxPrice);

        // Map list of entities to list of DTOs safely using Streams
        return entities.stream()
                .map(entity -> modelMapper.map(entity, ProductDTO.class))
                .collect(Collectors.toList());
    }
    public boolean processPayment(PaymentRequestDTO request) {
        // 1. Dummy Payment Gate Validation
        if (request.getCardNumber() == null || request.getCardNumber().length() < 16) {
            throw new IllegalArgumentException("Invalid card details. Payment failed.");
        }

        // 2. Mocking a successful payment transfer
        System.out.println("Processing dummy payment of $" + request.getAmount() + " for user " + request.getUserId());
        return true; // Payment approved!
    }

    public void createOrder(PaymentRequestDTO request) {
        // Execute the dummy payment process first
        processPayment(request);

        // Save order data into the database
        TransactionEntity transaction = new TransactionEntity();
        transaction.setUserId(request.getUserId());
        transaction.setProductId(request.getProductId());
        transaction.setAmountPaid(request.getAmount());
        transaction.setTransactionDate(LocalDateTime.now());

        if ("RENT".equalsIgnoreCase(request.getTransactionType())) {
            transaction.setType(TransactionType.RENT);
            // Example: Rentals last exactly 30 days
            transaction.setRentalExpiryDate(LocalDateTime.now().plusDays(30));
        } else {
            transaction.setType(TransactionType.PURCHASE);
            transaction.setRentalExpiryDate(null); // Permanent access
        }

        transactionRepository.save(transaction);
    }
    public ProductDTO updateProduct(Integer productId, ProductDTO updatedDto, Integer requestingUserId) throws AccessDeniedException {
        // 1. Fetch the existing product from DB
        ProductEntity existingEntity = repository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

        // 2. Security Check: Verify requesting user is the owner
        if (!existingEntity.getCreatedByUserId().equals(requestingUserId)) {
            throw new AccessDeniedException("You do not have permission to modify this product.");
        }

        // 3. Map new changes onto the existing managed entity (excluding ID and Owner)
        existingEntity.setName(updatedDto.getName());
        existingEntity.setPrice(updatedDto.getPrice());
        existingEntity.setCategory(updatedDto.getCategory());
        // Add any other fields your product contains here...

        // 4. Save updates and map back to DTO
        ProductEntity savedEntity = repository.save(existingEntity);
        return modelMapper.map(savedEntity, ProductDTO.class);
    }

    public void deleteProduct(Integer productId, Integer requestingUserId) throws AccessDeniedException {
        // 1. Fetch the existing product from DB
        ProductEntity existingEntity = repository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

        // 2. Security Check: Verify requesting user is the owner
        if (!existingEntity.getCreatedByUserId().equals(requestingUserId)) {
            throw new AccessDeniedException("You do not have permission to delete this product.");
        }

        // 3. Perform hard deletion
        repository.delete(existingEntity);
    }


}
