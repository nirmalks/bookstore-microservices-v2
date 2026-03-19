package com.nirmalks.user_service.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nirmalks.user_service.address.dto.AddressDto;
import com.nirmalks.user_service.address.dto.AddressRequestWithUserId;
import com.nirmalks.user_service.user.api.CreateUserRequest;
import com.nirmalks.user_service.user.api.UpdateUserRequest;
import com.nirmalks.user_service.user.api.UserResponse;
import com.nirmalks.user_service.user.service.UserService;
import dto.PageRequestDto;
import dto.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private UserService userService;

	private UserResponse userResponse;

	@BeforeEach
	void setUp() {
		userResponse = new UserResponse();
		userResponse.setId(1L);
		userResponse.setUsername("testuser");
		userResponse.setEmail("test@ex.com");
		userResponse.setUserRole(UserRole.CUSTOMER);
	}

	@Test
	void getUsers_returns_user_response_with_pagination() throws Exception {
		Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
		Page<UserResponse> page = new PageImpl<>(List.of(userResponse), pageable, 1);
		when(userService.getUsers(any(PageRequestDto.class))).thenReturn(page);

		mockMvc.perform(get("/api/v1/users").param("page", "0").param("size", "10").param("sortKey", "id"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].id").value(1L))
			.andExpect(jsonPath("$.content[0].username").value("testuser"))
			.andExpect(jsonPath("$.content[0].userRole").value("CUSTOMER"))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.totalPages").value(1))
			.andExpect(jsonPath("$.size").value(10))
			.andExpect(jsonPath("$.number").value(0))
			.andExpect(jsonPath("$.first").value(true))
			.andExpect(jsonPath("$.last").value(true));

		verify(userService).getUsers(any(PageRequestDto.class));
	}

	@Test
	void getUserById_returns_user_response() throws Exception {
		when(userService.getUserById(1L)).thenReturn(userResponse);

		mockMvc.perform(get("/api/v1/users/{id}", 1L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value("testuser"))
			.andExpect(jsonPath("$.email").value("test@ex.com"))
			.andExpect(jsonPath("$.userRole").value("CUSTOMER"));

		verify(userService).getUserById(1L);
	}

	@Test
	void addUser_creates_customer_and_returns_location_header_with_201_created() throws Exception {
		CreateUserRequest request = new CreateUserRequest();
		request.setUsername("testuser");
		request.setPassword("password");
		request.setEmail("test@ex.com");

		when(userService.createUser(any(CreateUserRequest.class), eq(UserRole.CUSTOMER))).thenReturn(userResponse);

		mockMvc
			.perform(post("/api/v1/users").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "http://localhost/api/v1/users/1"))
			.andExpect(jsonPath("$.id").value(1L))
			.andExpect(jsonPath("$.username").value("testuser"))
			.andExpect(jsonPath("$.email").value("test@ex.com"))
			.andExpect(jsonPath("$.userRole").value("CUSTOMER"));

		verify(userService).createUser(any(CreateUserRequest.class), eq(UserRole.CUSTOMER));
	}

	@Test
	void addUser_rejects_invalid_payload_with_400_bad_request() throws Exception {
		CreateUserRequest request = new CreateUserRequest();
		request.setUsername("testuser");
		request.setPassword("123");
		request.setEmail("not-an-email");

		mockMvc
			.perform(post("/api/v1/users").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());

		verifyNoInteractions(userService);
	}

	@Test
	void updateUser_with_valid_request_updates_user_and_returns_updated_body() throws Exception {
		UpdateUserRequest request = new UpdateUserRequest();
		request.setPassword("updatedPassword");
		request.setEmail("updated@ex.com");
		request.setRole(UserRole.ADMIN);

		UserResponse updatedResponse = new UserResponse();
		updatedResponse.setId(1L);
		updatedResponse.setUsername("testuser");
		updatedResponse.setEmail("updated@ex.com");
		updatedResponse.setUserRole(UserRole.ADMIN);

		when(userService.updateUser(eq(1L), any(UpdateUserRequest.class))).thenReturn(updatedResponse);

		mockMvc
			.perform(put("/api/v1/users/{id}", 1L).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("updated@ex.com"))
			.andExpect(jsonPath("$.userRole").value("ADMIN"));

		verify(userService).updateUser(eq(1L), any(UpdateUserRequest.class));
	}

	@Test
	void updateUser_with_invalid_request_rejects_with_400_bad_request() throws Exception {
		UpdateUserRequest request = new UpdateUserRequest();
		request.setPassword("123");
		request.setEmail("bad-email");

		mockMvc
			.perform(put("/api/v1/users/{id}", 1L).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());

		verifyNoInteractions(userService);
	}

	@Test
	void deleteUser_returns_ok_when_deletion_succeeds() throws Exception {
		doNothing().when(userService).deleteUser(1L);

		mockMvc.perform(delete("/api/v1/users/{id}", 1L).with(csrf())).andExpect(status().isOk());

		verify(userService).deleteUser(1L);
	}

	@Test
	void registerUser_creates_customer_and_returns_location_header() throws Exception {
		CreateUserRequest request = new CreateUserRequest();
		request.setUsername("testuser");
		request.setPassword("password");
		request.setEmail("test@ex.com");

		when(userService.createUser(any(CreateUserRequest.class), eq(UserRole.CUSTOMER))).thenReturn(userResponse);

		mockMvc
			.perform(post("/api/v1/users/register").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "http://localhost/api/v1/users/register/1"))
			.andExpect(jsonPath("$.username").value("testuser"));

		verify(userService).createUser(any(CreateUserRequest.class), eq(UserRole.CUSTOMER));
	}

	@Test
	void registerAdminUser_creates_admin_account_and_returns_location_header() throws Exception {
		CreateUserRequest request = new CreateUserRequest();
		request.setUsername("adminuser");
		request.setPassword("password");
		request.setEmail("admin@ex.com");

		UserResponse adminResponse = new UserResponse();
		adminResponse.setId(2L);
		adminResponse.setUsername("adminuser");
		adminResponse.setEmail("admin@ex.com");
		adminResponse.setUserRole(UserRole.ADMIN);

		when(userService.createUser(any(CreateUserRequest.class), eq(UserRole.ADMIN))).thenReturn(adminResponse);

		mockMvc
			.perform(post("/api/v1/users/admin/register").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "http://localhost/api/v1/users/admin/register/2"))
			.andExpect(jsonPath("$.username").value("adminuser"))
			.andExpect(jsonPath("$.userRole").value("ADMIN"));

		verify(userService).createUser(any(CreateUserRequest.class), eq(UserRole.ADMIN));
	}

	@Test
	void updateAddress_returns_saved_address() throws Exception {
		AddressRequestWithUserId request = new AddressRequestWithUserId();
		request.setUserId(1L);
		request.setCity("New York");
		request.setCountry("USA");
		request.setPinCode("10001");
		request.setState("NY");
		request.setAddress("5th Ave");
		request.setDefault(true);

		AddressDto response = new AddressDto();
		response.setId(10L);
		response.setCity("New York");
		response.setCountry("USA");
		response.setPinCode("10001");
		response.setState("NY");
		response.setAddress("5th Ave");
		response.setDefault(true);

		when(userService.updateAddress(any(AddressRequestWithUserId.class))).thenReturn(response);

		mockMvc
			.perform(post("/api/v1/users/address").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.city").value("New York"))
			.andExpect(jsonPath("$.country").value("USA"))
			.andExpect(jsonPath("$.default").value(true));

		verify(userService).updateAddress(any(AddressRequestWithUserId.class));
	}

}
