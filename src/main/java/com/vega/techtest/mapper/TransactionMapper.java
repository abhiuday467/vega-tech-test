package com.vega.techtest.mapper;

import com.vega.techtest.dto.TransactionItemRequest;
import com.vega.techtest.dto.TransactionItemResponse;
import com.vega.techtest.dto.TransactionRequest;
import com.vega.techtest.dto.TransactionResponse;
import com.vega.techtest.entity.TransactionEntity;
import com.vega.techtest.entity.TransactionItemEntity;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TransactionMapper {

    TransactionResponse toResponse(TransactionEntity entity);
    List<TransactionResponse> toResponseList(List<TransactionEntity> entities);
    TransactionItemResponse toItemResponse(TransactionItemEntity entity);
    List<TransactionItemResponse> toItemResponseList(List<TransactionItemEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.ZonedDateTime.now())")
    @Mapping(target = "transactionTimestamp", source = "timestamp")
    @Mapping(target = "items", ignore = true)
    TransactionEntity toEntity(TransactionRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "transaction", ignore = true)
    @Mapping(target = "totalPrice", expression = "java(calculateTotalPrice(request.getUnitPrice(), request.getQuantity()))")
    TransactionItemEntity toItemEntity(TransactionItemRequest request);

    List<TransactionItemEntity> toItemEntityList(List<TransactionItemRequest> requests);

    default BigDecimal calculateTotalPrice(BigDecimal unitPrice, Integer quantity) {
        if (unitPrice == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
