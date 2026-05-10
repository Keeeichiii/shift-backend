package shift.shift_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/",
								"/index.html",
								"/map.html",
								"/services.html",
								"/vehicle.html",
								"/support.html",
								"/fueling.html",
								"/tips.html",
								"/news.html",
								"/account.html",
								"/admin.html",
								"/moderator.html",
								"/css/**",
								"/js/**",
								"/images/**",
								"/uploads/**"
						).permitAll()
						.requestMatchers(HttpMethod.GET, "/api/vehicle-cards/public", "/api/vehicle-cards/public/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/news/public").permitAll()
						.requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
						.requestMatchers("/api/auth/me", "/api/auth/logout").authenticated()
						.requestMatchers("/api/me/**").authenticated()
						.requestMatchers("/api/support-requests/**").authenticated()
						.requestMatchers("/api/news/**").hasAnyRole("MODERATOR", "ADMIN")
						.requestMatchers("/api/vehicle-cards/**").hasAnyRole("MODERATOR", "ADMIN")
						.requestMatchers("/api/users/**").hasRole("ADMIN")
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.requestMatchers("/api/moderator/**").hasAnyRole("MODERATOR", "ADMIN")
						.anyRequest().authenticated())
				.logout(logout -> logout.logoutUrl("/api/auth/logout"));
		return http.build();
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider(
			UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder
	) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return provider;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}
}
