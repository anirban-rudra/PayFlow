package com.paypal.user_service.controller;

import com.paypal.user_service.entity.User;
import com.paypal.user_service.repository.UserRepository;
import com.paypal.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerTest {

    private UserService userService;
    private UserRepository userRepository;
    private UserController controller;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        userRepository = mock(UserRepository.class);
        controller = new UserController(userService, userRepository);
    }

    @Test
    void userCanReadSelfWithoutPasswordInResponse() {
        when(userService.getUserById(10L)).thenReturn(Optional.of(user(10L, "user@example.com")));

        ResponseEntity<?> response = controller.getUserById(10L, "10", "ROLE_USER");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasFieldOrPropertyWithValue("email", "user@example.com");
        assertThat(response.getBody()).hasFieldOrPropertyWithValue("payTag", "@user10");
        assertThat(response.getBody().getClass().getDeclaredFields())
                .extracting(Field::getName)
                .doesNotContain("password");
    }

    @Test
    void userCannotReadAnotherUser() {
        ResponseEntity<?> response = controller.getUserById(10L, "99", "ROLE_USER");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(userService, never()).getUserById(10L);
    }

    @Test
    void malformedUserHeaderIsForbiddenForNonAdmin() {
        ResponseEntity<?> response = controller.getUserById(10L, "not-a-number", "ROLE_USER");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(userService, never()).getUserById(10L);
    }

    @Test
    void adminCanReadAnyUserAndMissingUserReturnsNotFound() {
        when(userService.getUserById(10L)).thenReturn(Optional.of(user(10L, "user@example.com")));
        when(userService.getUserById(404L)).thenReturn(Optional.empty());

        ResponseEntity<?> found = controller.getUserById(10L, null, "ROLE_ADMIN");
        ResponseEntity<?> missing = controller.getUserById(404L, null, "ROLE_ADMIN");

        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void adminCanListAllUsers() {
        when(userService.getAllUsers()).thenReturn(List.of(user(10L, "user@example.com")));

        ResponseEntity<?> response = controller.getAllUsers("ROLE_ADMIN");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userService).getAllUsers();
    }

    @Test
    void nonAdminCannotListAllUsers() {
        ResponseEntity<?> response = controller.getAllUsers("ROLE_USER");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(userService, never()).getAllUsers();
    }

    @Test
    void resolvesTransferTargetWithoutExposingInternalId() {
        when(userRepository.findByPayTag("@receiver")).thenReturn(Optional.of(user(20L, "receiver@example.com")));

        ResponseEntity<?> response = controller.resolveTransferTarget("@Receiver");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasFieldOrPropertyWithValue("displayName", "User");
        assertThat(response.getBody()).hasFieldOrPropertyWithValue("payTag", "@user20");
        assertThat(response.getBody().getClass().getDeclaredFields())
                .extracting(Field::getName)
                .doesNotContain("userId", "id", "email");
    }

    @Test
    void internalLookupReturnsUserIdAndPayTag() {
        when(userService.getUserById(10L)).thenReturn(Optional.of(user(10L, "user@example.com")));

        ResponseEntity<?> response = controller.getUserInternal(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasFieldOrPropertyWithValue("userId", 10L);
        assertThat(response.getBody()).hasFieldOrPropertyWithValue("displayName", "User");
        assertThat(response.getBody()).hasFieldOrPropertyWithValue("payTag", "@user10");
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setName("User");
        user.setEmail(email);
        user.setPayTag("@user" + id);
        user.setPassword("encoded-password");
        user.setRole("ROLE_USER");
        return user;
    }
}
