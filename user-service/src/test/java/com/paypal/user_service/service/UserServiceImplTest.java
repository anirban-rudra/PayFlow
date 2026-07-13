package com.paypal.user_service.service;

import com.paypal.user_service.client.WalletClient;
import com.paypal.user_service.dto.CreateWalletRequest;
import com.paypal.user_service.entity.User;
import com.paypal.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    private UserRepository userRepository;
    private WalletClient walletClient;
    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        walletClient = mock(WalletClient.class);
        service = new UserServiceImpl(userRepository, walletClient);
    }

    @Test
    void createUserPersistsUserAndCreatesWallet() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        User created = service.createUser(user("ani@example.com"));

        assertThat(created.getId()).isEqualTo(42L);
        verify(walletClient).createWallet(org.mockito.ArgumentMatchers.argThat(request ->
                request instanceof CreateWalletRequest
                        && request.getUserId().equals(42L)
                        && request.getCurrency().equals("INR")));
    }

    @Test
    void createUserFailsWhenWalletCreationFailsSoTransactionCanRollback() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });
        doThrow(new RuntimeException("wallet unavailable")).when(walletClient).createWallet(any(CreateWalletRequest.class));

        assertThatThrownBy(() -> service.createUser(user("ani@example.com")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Wallet creation failed, user rolled back")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void delegatesReadOperationsToRepository() {
        User user = user("ani@example.com");
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(userRepository.findAll()).thenReturn(List.of(user));

        assertThat(service.getUserById(42L)).contains(user);
        assertThat(service.getAllUsers()).containsExactly(user);
    }

    private User user(String email) {
        User user = new User();
        user.setName("Alice");
        user.setEmail(email);
        user.setPayTag("@alice");
        user.setPassword("encoded-password");
        user.setRole("ROLE_USER");
        return user;
    }
}
