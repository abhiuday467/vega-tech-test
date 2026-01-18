package com.vega.techtest.mapper;

import com.vega.techtest.dto.TransactionItemRequest;
import com.vega.techtest.dto.TransactionRequest;
import com.vega.techtest.dto.TransactionResponse;
import com.vega.techtest.service.command.CreateTransactionCommand;
import com.vega.techtest.service.command.TransactionItem;
import com.vega.techtest.service.command.TransactionResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Mapper(componentModel = "spring")
public interface TransactionRequestMapper {

    @Mapping(target = "timestamp", source = "timestamp", qualifiedByName = "toUtc")
    CreateTransactionCommand toCommand(TransactionRequest request);

    TransactionItem toCommandItem(TransactionItemRequest item);

    List<TransactionItem> toCommandItems(List<TransactionItemRequest> items);

    @Mapping(target = "transactionTimestamp", source = "transactionTimestamp", qualifiedByName = "toUtc")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "toUtc")
    TransactionResponse toResponse(TransactionResult result);

    List<TransactionResponse> toResponseList(List<TransactionResult> results);

    @Named("toUtc")
    default ZonedDateTime toUtc(ZonedDateTime timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.withZoneSameInstant(ZoneOffset.UTC);
    }
}
