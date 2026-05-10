package shift.shift_backend.security;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import shift.shift_backend.domain.entity.Credential;
import shift.shift_backend.repository.CredentialRepository;
import shift.shift_backend.repository.UserRepository;
import shift.shift_backend.repository.UserRoleRepository;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final CredentialRepository credentialRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Credential credential = credentialRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Email not found"));

        shift.shift_backend.domain.entity.User user = userRepository.findById(credential.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<SimpleGrantedAuthority> authorities = userRoleRepository.findRoleNamesByUserId(user.getId()).stream()
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .toList();

        return User.withUsername(credential.getEmail())
                .password(credential.getPasswordHash())
                .authorities(authorities)
                .build();
    }
}
