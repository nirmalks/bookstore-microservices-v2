package com.nirmalks.catalog_service.author.dto;

public record AuthorDto(Long id, String name, String bio) {
	@Override
	public String toString() {
		return "AuthorDto{" + "id=" + id + ", name='" + name + '\'' + ", bio='" + bio + '\'' + '}';
	}

}
