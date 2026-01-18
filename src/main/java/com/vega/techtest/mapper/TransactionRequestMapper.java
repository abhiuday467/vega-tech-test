package com.vega.techtest.mapper;

import com.vega.techtest.dto.TransactionItemRequest;
import com.vega.techtest.dto.TransactionItemResponse;
import com.vega.techtest.dto.TransactionRequest;
import com.vega.techtest.dto.TransactionResponse;
import com.vega.techtest.service.command.CreateTransactionCommand;
import com.vega.techtest.service.command.TransactionItem;
import com.vega.techtest.service.command.TransactionItemResult;
import com.vega.techtest.service.command.TransactionResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Mapper(componentModel = "spring")
public interface TransactionRequestMapper {

    @Mapping(target = "timestamp", source = "timestamp", qualifiedByName = "toInstantUtc")
    CreateTransactionCommand toCommand(TransactionRequest request);

    TransactionItem toCommandItem(TransactionItemRequest item);

    List<TransactionItem> toCommandItems(List<TransactionItemRequest> items);

    @Mapping(target = "transactionTimestamp", source = "transactionTimestamp", qualifiedByName = "toUtc")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "toUtc")
    TransactionResponse toResponse(TransactionResult result);

    List<TransactionResponse> toResponseList(List<TransactionResult> results);

    TransactionItemResponse toItemResponse(TransactionItemResult item);

    List<TransactionItemResponse> toItemResponseList(List<TransactionItemResult> items);

    @Named("toInstantUtc")
    default Instant toInstantUtc(ZonedDateTime timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toInstant();
    }

    @Named("toUtc")
    default ZonedDateTime toUtc(Instant timestamp) {
        if (timestamp == null) {
            return null;
        }
        return ZonedDateTime.ofInstant(timestamp, ZoneOffset.UTC);
    }
}
