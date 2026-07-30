package com.caltalk.backend.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.caltalk.backend.common.error.GlobalExceptionHandler;
import com.caltalk.backend.common.error.PasswordMismatchException;
import com.caltalk.backend.config.SecurityConfig;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class SignupRequestValidationTests {

    private static final String VALID_EMAIL = "user@example.com";
    private static final String VALID_PASSWORD = "example-password";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SignupService signupService;

    @Test
    void allowsSignupWithoutAuthentication() throws Exception {
        when(signupService.signup(any(SignupRequest.class)))
                .thenReturn(new SignupResponse(VALID_EMAIL, "Asia/Seoul", Instant.parse("2026-07-30T00:00:00Z")));

        performSignup(VALID_EMAIL, VALID_PASSWORD, VALID_PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                .andExpect(jsonPath("$.timezone").value("Asia/Seoul"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void rejectsInvalidEmail() throws Exception {
        assertValidationError(
                performSignup("invalid-email", VALID_PASSWORD, VALID_PASSWORD),
                "email",
                "INVALID_EMAIL"
        );
    }

    @Test
    void rejectsEmailLongerThan254Characters() throws Exception {
        String email = "a".repeat(243) + "@example.com";

        assertValidationError(
                performSignup(email, VALID_PASSWORD, VALID_PASSWORD),
                "email",
                "EMAIL_TOO_LONG"
        );
    }

    @Test
    void rejectsPasswordShorterThanEightCharacters() throws Exception {
        assertValidationError(
                performSignup(VALID_EMAIL, "short", "short"),
                "password",
                "INVALID_PASSWORD_LENGTH"
        );
    }

    @Test
    void rejectsPasswordLongerThan64Characters() throws Exception {
        String password = "a".repeat(65);

        assertValidationError(
                performSignup(VALID_EMAIL, password, password),
                "password",
                "INVALID_PASSWORD_LENGTH"
        );
    }

    @Test
    void rejectsWhitespaceOnlyPasswordWithoutEchoingIt() throws Exception {
        String password = "        ";
        when(signupService.signup(any(SignupRequest.class)))
                .thenThrow(new PasswordMismatchException(
                        "password",
                        "INVALID_PASSWORD",
                        "비밀번호는 공백만으로 구성할 수 없습니다."
                ));

        assertValidationError(
                performSignup(VALID_EMAIL, password, password),
                "password",
                "INVALID_PASSWORD"
        )
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(password)
                )));
    }

    @Test
    void rejectsMismatchedPasswordConfirmation() throws Exception {
        when(signupService.signup(any(SignupRequest.class)))
                .thenThrow(new PasswordMismatchException(
                        "passwordConfirmation",
                        "PASSWORD_MISMATCH",
                        "비밀번호 확인이 일치하지 않습니다."
                ));

        assertValidationError(
                performSignup(VALID_EMAIL, VALID_PASSWORD, "different-password"),
                "passwordConfirmation",
                "PASSWORD_MISMATCH"
        );
    }

    @Test
    void rejectsMissingRequiredValues() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("입력한 내용을 다시 확인해주세요."))
                .andExpect(jsonPath("$.fieldErrors.length()").value(3))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.secret").doesNotExist());
    }

    private org.springframework.test.web.servlet.ResultActions performSignup(
            String email,
            String password,
            String passwordConfirmation
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "%s",
                          "password": "%s",
                          "passwordConfirmation": "%s"
                        }
                        """.formatted(email, password, passwordConfirmation)));
    }

    private org.springframework.test.web.servlet.ResultActions assertValidationError(
            org.springframework.test.web.servlet.ResultActions resultActions,
            String field,
            String fieldCode
    ) throws Exception {
        return resultActions
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("입력한 내용을 다시 확인해주세요."))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == '%s')].code".formatted(field))
                        .value(org.hamcrest.Matchers.hasItem(fieldCode)))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.secret").doesNotExist());
    }
}
