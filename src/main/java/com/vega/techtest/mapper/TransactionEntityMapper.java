package com.vega.techtest.mapper;

import com.vega.techtest.dto.TransactionItemRequest;
import com.vega.techtest.dto.TransactionItemResponse;
import com.vega.techtest.dto.TransactionRequest;
import com.vega.techtest.dto.TransactionResponse;
import com.vega.techtest.entity.TransactionEntity;
import com.vega.techtest.entity.TransactionItemEntity;
import com.vega.techtest.service.command.CreateTransactionCommand;
import com.vega.techtest.service.command.TransactionItem;
import com.vega.techtest.service.command.TransactionItemResult;
import com.vega.techtest.service.command.TransactionResult;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TransactionEntityMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "transactionId", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "transactionTimestamp", source = "timestamp")
    @Mapping(target = "items", ignore = true)
    TransactionEntity toEntityFromCommand(CreateTransactionCommand command);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "transaction", ignore = true)
    @Mapping(target = "totalPrice", expression = "java(calculateTotalPrice(item.unitPrice(), item.quantity()))")
    TransactionItemEntity toItemEntityFromCommand(TransactionItem item);

    List<TransactionItemEntity> toItemEntityListFromCommand(List<TransactionItem> items);

    TransactionResult toResult(TransactionEntity entity);

    List<TransactionResult> toResultList(List<TransactionEntity> entities);

    TransactionItemResult toItemResult(TransactionItemEntity entity);

    List<TransactionItemResult> toItemResultList(List<TransactionItemEntity> entities);

    @Deprecated
    TransactionResponse toResponse(TransactionEntity entity);

    @Deprecated
    List<TransactionResponse> toResponseList(List<TransactionEntity> entities);

    @Deprecated
    TransactionItemResponse toItemResponse(TransactionItemEntity entity);

    @Deprecated
    List<TransactionItemResponse> toItemResponseList(List<TransactionItemEntity> entities);

    @Deprecated
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "transactionTimestamp", expression = "java(request.timestamp() == null ? null : request.timestamp().toInstant())")
    @Mapping(target = "items", ignore = true)
    TransactionEntity toEntity(TransactionRequest request);

    @Deprecated
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "transaction", ignore = true)
    @Mapping(target = "totalPrice", expression = "java(calculateTotalPrice(request.unitPrice(), request.quantity()))")
    TransactionItemEntity toItemEntity(TransactionItemRequest request);

    @Deprecated
    List<TransactionItemEntity> toItemEntityList(List<TransactionItemRequest> requests);

    default BigDecimal calculateTotalPrice(BigDecimal unitPrice, Integer quantity) {
        if (unitPrice == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    default ZonedDateTime toUtc(Instant timestamp) {
        if (timestamp == null) {
            return null;
        }
        return ZonedDateTime.ofInstant(timestamp, ZoneOffset.UTC);
    }
}
