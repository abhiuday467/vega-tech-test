package com.vega.techtest.mapper;

import com.vega.techtest.dto.TransactionItemRequest;
import com.vega.techtest.dto.TransactionItemResponse;
import com.vega.techtest.dto.TransactionRequest;
import com.vega.techtest.dto.TransactionResponse;
import com.vega.techtest.entity.TransactionEntity;
import com.vega.techtest.entity.TransactionItemEntity;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-17T12:14:42+0000",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.9 (Amazon.com Inc.)"
)
@Component
public class TransactionMapperImpl implements TransactionMapper {

    @Override
    public TransactionResponse toResponse(TransactionEntity entity) {
        if ( entity == null ) {
            return null;
        }

        TransactionResponse transactionResponse = new TransactionResponse();

        transactionResponse.setTransactionId( entity.getTransactionId() );
        transactionResponse.setCustomerId( entity.getCustomerId() );
        transactionResponse.setStoreId( entity.getStoreId() );
        transactionResponse.setTillId( entity.getTillId() );
        transactionResponse.setPaymentMethod( entity.getPaymentMethod() );
        transactionResponse.setTotalAmount( entity.getTotalAmount() );
        transactionResponse.setCurrency( entity.getCurrency() );
        transactionResponse.setTransactionTimestamp( entity.getTransactionTimestamp() );
        transactionResponse.setCreatedAt( entity.getCreatedAt() );
        transactionResponse.setStatus( entity.getStatus() );
        transactionResponse.setItems( toItemResponseList( entity.getItems() ) );

        return transactionResponse;
    }

    @Override
    public List<TransactionResponse> toResponseList(List<TransactionEntity> entities) {
        if ( entities == null ) {
            return null;
        }

        List<TransactionResponse> list = new ArrayList<TransactionResponse>( entities.size() );
        for ( TransactionEntity transactionEntity : entities ) {
            list.add( toResponse( transactionEntity ) );
        }

        return list;
    }

    @Override
    public TransactionItemResponse toItemResponse(TransactionItemEntity entity) {
        if ( entity == null ) {
            return null;
        }

        TransactionItemResponse transactionItemResponse = new TransactionItemResponse();

        transactionItemResponse.setProductName( entity.getProductName() );
        transactionItemResponse.setProductCode( entity.getProductCode() );
        transactionItemResponse.setUnitPrice( entity.getUnitPrice() );
        transactionItemResponse.setQuantity( entity.getQuantity() );
        transactionItemResponse.setTotalPrice( entity.getTotalPrice() );
        transactionItemResponse.setCategory( entity.getCategory() );

        return transactionItemResponse;
    }

    @Override
    public List<TransactionItemResponse> toItemResponseList(List<TransactionItemEntity> entities) {
        if ( entities == null ) {
            return null;
        }

        List<TransactionItemResponse> list = new ArrayList<TransactionItemResponse>( entities.size() );
        for ( TransactionItemEntity transactionItemEntity : entities ) {
            list.add( toItemResponse( transactionItemEntity ) );
        }

        return list;
    }

    @Override
    public TransactionEntity toEntity(TransactionRequest request) {
        if ( request == null ) {
            return null;
        }

        TransactionEntity transactionEntity = new TransactionEntity();

        transactionEntity.setTransactionTimestamp( request.getTimestamp() );
        transactionEntity.setTransactionId( request.getTransactionId() );
        transactionEntity.setCustomerId( request.getCustomerId() );
        transactionEntity.setStoreId( request.getStoreId() );
        transactionEntity.setTillId( request.getTillId() );
        transactionEntity.setPaymentMethod( request.getPaymentMethod() );
        transactionEntity.setTotalAmount( request.getTotalAmount() );
        transactionEntity.setCurrency( request.getCurrency() );

        transactionEntity.setCreatedAt( java.time.ZonedDateTime.now() );

        return transactionEntity;
    }

    @Override
    public TransactionItemEntity toItemEntity(TransactionItemRequest request) {
        if ( request == null ) {
            return null;
        }

        TransactionItemEntity transactionItemEntity = new TransactionItemEntity();

        transactionItemEntity.setProductName( request.getProductName() );
        transactionItemEntity.setProductCode( request.getProductCode() );
        transactionItemEntity.setUnitPrice( request.getUnitPrice() );
        transactionItemEntity.setQuantity( request.getQuantity() );
        transactionItemEntity.setCategory( request.getCategory() );

        transactionItemEntity.setTotalPrice( calculateTotalPrice(request.getUnitPrice(), request.getQuantity()) );

        return transactionItemEntity;
    }

    @Override
    public List<TransactionItemEntity> toItemEntityList(List<TransactionItemRequest> requests) {
        if ( requests == null ) {
            return null;
        }

        List<TransactionItemEntity> list = new ArrayList<TransactionItemEntity>( requests.size() );
        for ( TransactionItemRequest transactionItemRequest : requests ) {
            list.add( toItemEntity( transactionItemRequest ) );
        }

        return list;
    }
}
