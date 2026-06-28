package com.interviewlab.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock  UserRepository userRepository;
    @InjectMocks UserService userService;

    private static final String GOOGLE_SUB = "google-sub-123";
    private static final String EMAIL      = "user@example.com";
    private static final String NAME       = "Test User";
    private static final String PICTURE    = "https://pic.url";

    @Test
    void findOrCreate_newUser_createsAndReturnsUser() {
        when(userRepository.findByGoogleSub(GOOGLE_SUB)).thenReturn(Optional.empty());
        User saved = new User(GOOGLE_SUB, EMAIL, NAME, PICTURE);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        User result = userService.findOrCreate(GOOGLE_SUB, EMAIL, NAME, PICTURE);

        assertThat(result.getEmail()).isEqualTo(EMAIL);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void findOrCreate_existingUser_returnsExistingWithoutSave() {
        User existing = new User(GOOGLE_SUB, EMAIL, NAME, PICTURE);
        when(userRepository.findByGoogleSub(GOOGLE_SUB)).thenReturn(Optional.of(existing));

        User result = userService.findOrCreate(GOOGLE_SUB, EMAIL, NAME, PICTURE);

        assertThat(result).isSameAs(existing);
        verify(userRepository, never()).save(any());
    }

    @Test
    void findOrCreate_existingUser_updatesNameWhenChanged() {
        User existing = new User(GOOGLE_SUB, EMAIL, "Old Name", PICTURE);
        when(userRepository.findByGoogleSub(GOOGLE_SUB)).thenReturn(Optional.of(existing));

        User result = userService.findOrCreate(GOOGLE_SUB, EMAIL, "New Name", PICTURE);

        assertThat(result.getName()).isEqualTo("New Name");
        verify(userRepository, never()).save(any());  // Dirty tracking handles persistence
    }

    @Test
    void findOrCreate_existingUser_updatesPictureWhenChanged() {
        User existing = new User(GOOGLE_SUB, EMAIL, NAME, "https://old-pic.url");
        when(userRepository.findByGoogleSub(GOOGLE_SUB)).thenReturn(Optional.of(existing));

        User result = userService.findOrCreate(GOOGLE_SUB, EMAIL, NAME, "https://new-pic.url");

        assertThat(result.getPicture()).isEqualTo("https://new-pic.url");
    }

    @Test
    void findById_userNotFound_throwsAuthExceptionWithUserNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(id))
            .isInstanceOf(AuthException.class)
            .satisfies(ex -> assertThat(((AuthException) ex).errorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    void findById_userExists_returnsUser() {
        UUID id = UUID.randomUUID();
        User user = new User(GOOGLE_SUB, EMAIL, NAME, PICTURE);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        User result = userService.findById(id);

        assertThat(result.getEmail()).isEqualTo(EMAIL);
    }
}
