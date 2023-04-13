package com.easybytes.gatewayserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

	
	@Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http.authorizeExchange(exchanges -> exchanges.pathMatchers("/eazybank/accounts/**").hasRole("ACCOUNTS")
                        .pathMatchers("/eazybank/cards/**").authenticated()
                        .pathMatchers("/eazybank/loans/**").permitAll())
                .oauth2ResourceServer().jwt().jwtAuthenticationConverter(grantedAuthoritiesExtractor());
        http.csrf().disable();
        return http.build();
    }
	
	 Converter<Jwt, Mono<AbstractAuthenticationToken>> grantedAuthoritiesExtractor() {
	        JwtAuthenticationConverter jwtAuthenticationConverter =
	                new JwtAuthenticationConverter();
	        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter
	                (new KeyCloakRoleConverter());
	        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
	    }
}
