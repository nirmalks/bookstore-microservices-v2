package com.nirmalks.checkout_service.order.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dto.OrderMessage;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OrderMessageJsonConverter implements AttributeConverter<OrderMessage, String> {

	private final ObjectMapper objectMapper;

	public OrderMessageJsonConverter() {
		this.objectMapper = new ObjectMapper();
		this.objectMapper.registerModule(new JavaTimeModule());
	}

	@Override
	public String convertToDatabaseColumn(OrderMessage attribute) {
		if (attribute == null) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(attribute);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException("Error converting OrderMessage to JSON string", e);
		}
	}

	@Override
	public OrderMessage convertToEntityAttribute(String dbData) {
		if (dbData == null) {
			return null;
		}
		try {
			return objectMapper.readValue(dbData, OrderMessage.class);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException("Error converting JSON string to OrderMessage object", e);
		}
	}

}
