package com.example.orderservice.service;

import com.example.orderservice.dto.ItemDTO;
import com.example.orderservice.entity.Item;
import com.example.orderservice.mapper.ItemMapper;
import com.example.orderservice.repository.ItemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    @Transactional(readOnly = true)
    public ItemDTO getItemById(Long id) {
        log.debug("Fetching item by id: {}", id);
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Item not found with id: " + id));
        return itemMapper.toDTO(item);
    }

    @Transactional(readOnly = true)
    public List<ItemDTO> getAllItems() {
        log.debug("Fetching all items");
        return itemRepository.findAll().stream()
                .map(itemMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ItemDTO> getItemsWithPagination(Pageable pageable) {
        log.debug("Fetching items with pagination: {}", pageable);
        return itemRepository.findAll(pageable)
                .map(itemMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public List<ItemDTO> getItemsByName(String name) {
        log.debug("Fetching items by name: {}", name);
        return itemRepository.findAll().stream()
                .filter(item -> item.getName().toLowerCase().contains(name.toLowerCase()))
                .map(itemMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ItemDTO> getItemsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        log.debug("Fetching items by price range: {} - {}", minPrice, maxPrice);
        return itemRepository.findAll().stream()
                .filter(item -> {
                    if (item.getPrice() == null) return false;
                    return item.getPrice().compareTo(minPrice) >= 0 &&
                            item.getPrice().compareTo(maxPrice) <= 0;
                })
                .map(itemMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ItemDTO createItem(ItemDTO itemDTO) {
        log.info("Creating new item: {}", itemDTO.getName());

        if (itemDTO.getPrice() != null && itemDTO.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }

        Item item = itemMapper.toEntity(itemDTO);
        Item savedItem = itemRepository.save(item);

        log.info("Item created with id: {}", savedItem.getId());
        return itemMapper.toDTO(savedItem);
    }

    @Transactional
    public ItemDTO updateItem(Long id, ItemDTO itemDTO) {
        log.info("Updating item with id: {}", id);

        Item existingItem = itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Item not found with id: " + id));

        if (itemDTO.getPrice() != null && itemDTO.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }

        itemMapper.updateEntityFromDTO(itemDTO, existingItem);
        existingItem.setUpdatedAt(LocalDateTime.now());

        Item updatedItem = itemRepository.save(existingItem);

        log.info("Item updated with id: {}", id);
        return itemMapper.toDTO(updatedItem);
    }

    @Transactional
    public void deleteItem(Long id) {
        log.info("Deleting item with id: {}", id);

        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Item not found with id: " + id));

        if (itemRepository.existsInOrderItems(id)) {
            throw new IllegalStateException("Cannot delete item with id " + id +
                    " because it is used in existing orders");
        }

        itemRepository.delete(item);
        log.info("Item deleted with id: {}", id);
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return itemRepository.existsById(id);
    }

    @Transactional(readOnly = true)
    public List<ItemDTO> searchItems(String name, BigDecimal minPrice, BigDecimal maxPrice) {
        log.debug("Searching items with filters - name: {}, minPrice: {}, maxPrice: {}",
                name, minPrice, maxPrice);

        return itemRepository.findAll().stream()
                .filter(item -> {
                    // Фильтр по имени
                    if (name != null && !name.trim().isEmpty()) {
                        if (!item.getName().toLowerCase().contains(name.toLowerCase())) {
                            return false;
                        }
                    }

                    // Фильтр по цене
                    if (minPrice != null && item.getPrice() != null) {
                        if (item.getPrice().compareTo(minPrice) < 0) {
                            return false;
                        }
                    }

                    if (maxPrice != null && item.getPrice() != null) {
                        if (item.getPrice().compareTo(maxPrice) > 0) {
                            return false;
                        }
                    }

                    return true;
                })
                .map(itemMapper::toDTO)
                .collect(Collectors.toList());
    }
}