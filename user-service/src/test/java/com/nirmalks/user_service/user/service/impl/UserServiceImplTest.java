package com.nirmalks.user_service.user.service.impl;

import com.nirmalks.user_service.address.Address;
import com.nirmalks.user_service.address.dto.AddressDto;
import com.nirmalks.user_service.address.dto.AddressRequestWithUserId;
import com.nirmalks.user_service.address.repository.AddressRepository;
import com.nirmalks.user_service.auth.api.LoginResponse;
import com.nirmalks.user_service.user.api.CreateUserRequest;
import com.nirmalks.user_service.user.api.UpdateUserRequest;
import com.nirmalks.user_service.user.api.UserResponse;
import com.nirmalks.user_service.user.dto.UserDtoInternal;
import com.nirmalks.user_service.user.entity.User;
import com.nirmalks.user_service.user.repository.UserRepository;
import dto.PageRequestDto;
import dto.UserRole;
import exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import security.JwtUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UserServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private AddressRepository addressRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private UserServiceImpl userService;

	private User sampleUser;

	private CreateUserRequest createRequest;

	private UpdateUserRequest updateRequest;

	private PageRequestDto pageRequestDto;

	@BeforeEach
	void setUp() {
		sampleUser = new User();
		sampleUser.setId(1L);
		sampleUser.setUsername("testuser");
		sampleUser.setPassword("encodedPassword");
		sampleUser.setEmail("test@ex.com");
		sampleUser.setRole(UserRole.CUSTOMER);

		createRequest = new CreateUserRequest();
		createRequest.setUsername("testuser");
		createRequest.setPassword("rawPassword");
		createRequest.setEmail("test@ex.com");

		updateRequest = new UpdateUserRequest();
		updateRequest.setPassword("updatedPassword");
		updateRequest.setEmail("updated@ex.com");
		updateRequest.setRole(UserRole.ADMIN);

		pageRequestDto = new PageRequestDto(0, 10, "id", "asc");
	}

	@Test
	void getUsers_returns_user_info_with_pagination() {

		Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
		Page<User> userPage = new PageImpl<>(List.of(sampleUser), pageable, 1);
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

		Page<UserResponse> result = userService.getUsers(pageRequestDto);

		assertNotNull(result);
		assertEquals(1, result.getTotalElements());
		assertEquals(1, result.getTotalPages());
		var user = result.getContent().get(0);
		assertEquals("testuser", user.getUsername());
		assertEquals(UserRole.CUSTOMER, user.getUserRole());
		assertEquals("test@ex.com", user.getEmail());
		assertEquals(0, result.getPageable().getPageNumber());
		assertEquals(10, result.getPageable().getPageSize());
		verify(userRepository).findAll(pageableCaptor.capture());
		assertEquals(0, pageableCaptor.getValue().getPageNumber());
		assertEquals(10, pageableCaptor.getValue().getPageSize());
	}

	@Test
	void create_user_encodes_password_and_saves_user() {
		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UserResponse response = userService.createUser(createRequest, UserRole.CUSTOMER);

		assertNotNull(response);
		assertEquals("testuser", response.getUsername());
		assertEquals("test@ex.com", response.getEmail());
		verify(userRepository).save(userCaptor.capture());
		assertEquals("encodedPassword", userCaptor.getValue().getPassword());
		assertEquals(UserRole.CUSTOMER, userCaptor.getValue().getRole());
	}

	@Test
	void create_users_throws_error_on_duplicate_username() {
		when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(sampleUser));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> userService.createUser(createRequest, UserRole.CUSTOMER));

		assertEquals("Username already exists", exception.getMessage());
		verify(userRepository, never()).save(any(User.class));
		verifyNoInteractions(passwordEncoder);
	}

	@Test
	void getUserById_returns_user_response() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

		UserResponse response = userService.getUserById(1L);

		assertNotNull(response);
		assertEquals("testuser", response.getUsername());
		assertEquals(UserRole.CUSTOMER, response.getUserRole());
	}

	@Test
	void getUserById_throws_error_when_user_is_not_found() {
		when(userRepository.findById(1L)).thenReturn(Optional.empty());

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> userService.getUserById(1L));

		assertEquals("User not found", exception.getMessage());
	}

	@Test
	void update_user_returns_response_with_updated_fields() {
		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UserResponse response = userService.updateUser(1L, updateRequest);

		assertNotNull(response);
		assertEquals("updated@ex.com", response.getEmail());
		assertEquals(UserRole.ADMIN, response.getUserRole());
		verify(userRepository).save(userCaptor.capture());
		assertEquals("updatedPassword", userCaptor.getValue().getPassword());
		assertEquals("updated@ex.com", userCaptor.getValue().getEmail());
		assertEquals(UserRole.ADMIN, userCaptor.getValue().getRole());
		assertEquals("testuser", userCaptor.getValue().getUsername());
	}

	@Test
	void update_user_throws_exception_if_user_is_not_found() {
		when(userRepository.findById(1L)).thenReturn(Optional.empty());

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> userService.updateUser(1L, updateRequest));

		assertEquals("User not found", exception.getMessage());
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void deleteUser_removes_existing_user() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

		userService.deleteUser(1L);

		verify(userRepository).deleteById(1L);
	}

	@Test
	void deleteUser_throws_exception_if_user_not_found() {
		when(userRepository.findById(1L)).thenReturn(Optional.empty());

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> userService.deleteUser(1L));

		assertEquals("User not found", exception.getMessage());
		verify(userRepository, never()).deleteById(any(Long.class));
	}

	@Test
	void authenticate_returns_jwt_for_valid_credentials() {
		when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
		when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);

		try (MockedStatic<JwtUtils> jwtUtilsMock = mockStatic(JwtUtils.class)) {
			jwtUtilsMock.when(() -> JwtUtils.generateToken("testuser", sampleUser.getAuthorities()))
				.thenReturn("mockedJwtToken");

			LoginResponse response = userService.authenticate("testuser", "rawPassword");

			assertNotNull(response);
			assertEquals("mockedJwtToken", response.getToken());
			assertEquals("testuser", response.getUsername());
			assertEquals(1L, response.getUserId());
			assertEquals("CUSTOMER", response.getRole());
		}
	}

	@Test
	void authenticate_throws_error_when_user_is_not_found() {
		when(userRepository.findByUsername("missing-user")).thenReturn(Optional.empty());

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> userService.authenticate("missing-user", "rawPassword"));

		assertEquals("user not found", exception.getMessage());
		verifyNoInteractions(passwordEncoder);
	}

	@Test
	void authenticate_throws_error_when_password_does_not_match() {
		when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
		when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

		try (MockedStatic<JwtUtils> jwtUtilsMock = mockStatic(JwtUtils.class)) {
			jwtUtilsMock.when(() -> JwtUtils.generateToken("testuser", sampleUser.getAuthorities()))
				.thenReturn("mockedJwtToken");

			IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> userService.authenticate("testuser", "wrongPassword"));

			assertEquals("Invalid username or password", exception.getMessage());
		}
	}

	@Test
	void internalAuthenticate_returns_internal_dto_for_valid_credentials() {
		when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
		when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);

		UserDtoInternal response = userService.internalAuthenticate("testuser", "rawPassword");

		assertNotNull(response);
		assertEquals("testuser", response.username());
		assertEquals("encodedPassword", response.hashedPassword());
		assertEquals(UserRole.CUSTOMER, response.role());
		assertEquals(1L, response.id());
	}

	@Test
	void internalAuthenticate_throws_error_when_password_does_not_match() {
		when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
		when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> userService.internalAuthenticate("testuser", "wrongPassword"));

		assertEquals("Invalid username or password", exception.getMessage());
	}

	@Test
	void internalAuthenticate_throws_error_when_user_is_not_found() {
		when(userRepository.findByUsername("missing-user")).thenReturn(Optional.empty());

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> userService.internalAuthenticate("missing-user", "rawPassword"));

		assertEquals("user not found", exception.getMessage());
	}

	@Test
	void updateAddress_persists_address_for_existing_user() {
		AddressRequestWithUserId request = new AddressRequestWithUserId();
		request.setUserId(1L);
		request.setCity("New York");
		request.setCountry("USA");
		request.setPinCode("10001");
		request.setState("NY");
		request.setAddress("5th Ave");
		request.setDefault(true);

		Address savedAddress = new Address();
		savedAddress.setId(10L);
		savedAddress.setCity("New York");
		savedAddress.setCountry("USA");
		savedAddress.setPinCode("10001");
		savedAddress.setState("NY");
		savedAddress.setAddress("5th Ave");
		savedAddress.setDefault(true);
		savedAddress.setUser(sampleUser);

		ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);
		when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
		when(addressRepository.save(any(Address.class))).thenReturn(savedAddress);

		AddressDto response = userService.updateAddress(request);

		assertNotNull(response);
		assertEquals("New York", response.getCity());
		assertEquals("USA", response.getCountry());
		assertEquals(true, response.isDefault());
		verify(addressRepository).save(addressCaptor.capture());
		assertSame(sampleUser, addressCaptor.getValue().getUser());
		assertEquals("5th Ave", addressCaptor.getValue().getAddress());
	}

	@Test
	void updateAddress_throws_exception_if_user_not_found() {
		AddressRequestWithUserId request = new AddressRequestWithUserId();
		request.setUserId(42L);

		when(userRepository.findById(42L)).thenReturn(Optional.empty());

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> userService.updateAddress(request));

		assertEquals("User not found", exception.getMessage());
		verifyNoInteractions(addressRepository);
	}

	@Test
	void findByUsername_returns_entity_when_present() {
		when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));

		User result = userService.findByUsername("testuser");

		assertSame(sampleUser, result);
	}

	@Test
	void findByUsername_throws_exception_if_user_not_found() {
		when(userRepository.findByUsername("missing-user")).thenReturn(Optional.empty());

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> userService.findByUsername("missing-user"));

		assertEquals("User not found", exception.getMessage());
	}

}
