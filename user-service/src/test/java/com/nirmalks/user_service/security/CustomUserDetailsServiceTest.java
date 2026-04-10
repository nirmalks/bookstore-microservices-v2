package com.nirmalks.user_service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.nirmalks.user_service.user.entity.User;
import com.nirmalks.user_service.user.repository.UserRepository;
import dto.UserRole;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CustomUserDetailsServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private CustomUserDetailsService customUserDetailsService;

	@Test
	void loadUserByUsername_should_return_user_details_when_user_exists() {
		User user = new User();
		user.setId(1L);
		user.setUsername("testuser");
		user.setPassword("hashedPassword");
		user.setRole(UserRole.CUSTOMER);
		user.setEmail("test@ex.com");

		when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

		UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

		assertNotNull(userDetails);
		assertEquals("testuser", userDetails.getUsername());
		assertEquals("hashedPassword", userDetails.getPassword());
		assertEquals(1, userDetails.getAuthorities().size());
		assertEquals("CUSTOMER", userDetails.getAuthorities().iterator().next().getAuthority());

		verify(userRepository).findByUsername("testuser");
	}

	@Test
	void loadUserByUsername_should_throw_exception_when_user_does_not_exist() {
		when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

		assertThrows(UsernameNotFoundException.class, () -> customUserDetailsService.loadUserByUsername("unknown"));

		verify(userRepository).findByUsername("unknown");
	}

}
