package com.nirmalks.catalog_service.author.api;

import jakarta.validation.constraints.NotNull;

public class AuthorRequest {

	@NotNull(message = "Name is required")
	private String name;

	@NotNull(message = "Bio is required")
	private String bio;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	@Override
	public String toString() {
		return "AuthorRequest{" + "name='" + name + '\'' + ", bio='" + bio + '\'' + '}';
	}

}
