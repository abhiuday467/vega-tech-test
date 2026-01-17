package com.vega.techtest.mapper;

import com.vega.techtest.dto.TransactionItemRequest;
import com.vega.techtest.dto.TransactionRequest;
import com.vega.techtest.dto.TransactionResponse;
import com.vega.techtest.service.command.CreateTransactionCommand;
import com.vega.techtest.service.command.TransactionItem;
import com.vega.techtest.service.command.TransactionResult;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TransactionRequestMapper {

    CreateTransactionCommand toCommand(TransactionRequest request);

    TransactionItem toCommandItem(TransactionItemRequest item);

    List<TransactionItem> toCommandItems(List<TransactionItemRequest> items);

    TransactionResponse toResponse(TransactionResult result);

    List<TransactionResponse> toResponseList(List<TransactionResult> results);
}
