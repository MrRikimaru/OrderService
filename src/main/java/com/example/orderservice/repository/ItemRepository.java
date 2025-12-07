package com.example.orderservice.repository;

import com.example.orderservice.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    List<Item> findByNameContainingIgnoreCase(String name);

    List<Item> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    @Query("SELECT i FROM Item i WHERE i.price <= :maxPrice")
    List<Item> findItemsBelowPrice(@Param("maxPrice") BigDecimal maxPrice);

    @Query("SELECT i FROM Item i WHERE i.price >= :minPrice")
    List<Item> findItemsAbovePrice(@Param("minPrice") BigDecimal minPrice);

    // Проверка, используется ли товар в заказах
    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi WHERE oi.item.id = :itemId")
    boolean existsInOrderItems(@Param("itemId") Long itemId);
}