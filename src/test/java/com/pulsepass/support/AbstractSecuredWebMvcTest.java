package com.pulsepass.support;

import com.pulsepass.domain.Role;
import com.pulsepass.security.CustomUserDetailsService;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.RestAccessDeniedHandler;
import com.pulsepass.security.RestAuthenticationEntryPoint;
import com.pulsepass.security.SecurityConfig;
import com.pulsepass.security.SecurityPrincipal;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@Import({SecurityConfig.class, JwtService.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@TestPropertySource(properties = {
        "pulsepass.security.jwt.secret-base64=" + AbstractSecuredWebMvcTest.TEST_SECRET,
        "pulsepass.security.jwt.issuer=pulsepass-api",
        "pulsepass.security.jwt.access-token-ttl=PT15M"
})
public abstract class AbstractSecuredWebMvcTest {
    public static final String TEST_SECRET = "cHVsc2VwYXNzLXRlc3Qtc2VjcmV0LWtleS0zMi1ieXRlcy1taW5pbXVtIQ==";

    @MockitoBean
    protected CustomUserDetailsService customUserDetailsService;

    protected static SecurityPrincipal principal(Long id, String username, Role... roles) {
        return new SecurityPrincipal(id, username, null, true, Set.of(roles));
    }

    protected static RequestPostProcessor asUser() {
        return user(principal(1L, "andrea", Role.USER));
    }

    protected static RequestPostProcessor asStaff() {
        return user(principal(3L, "laura", Role.STAFF));
    }

    protected static RequestPostProcessor asAdmin() {
        return user(principal(4L, "miguel", Role.ADMIN));
    }
}
