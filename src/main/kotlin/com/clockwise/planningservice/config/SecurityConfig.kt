package com.clockwise.planningservice.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.server.ServerWebExchange
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler
import org.springframework.http.HttpStatus
import reactor.core.publisher.Mono
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.MediaType
import mu.KotlinLogging
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

private val logger = KotlinLogging.logger {}

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig {

    @Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private lateinit var jwkSetUri: String

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .cors { cors -> cors.configurationSource(corsConfigurationSource()) }
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/actuator/**").permitAll()
                    .pathMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .pathMatchers(HttpMethod.OPTIONS).permitAll()
                    // Schedule endpoints - Role hierarchy: admin > manager > employee
                    .pathMatchers(HttpMethod.POST, "/v1/schedules").hasAnyRole("admin", "manager")
                    .pathMatchers(HttpMethod.PUT, "/v1/schedules/**").hasAnyRole("admin", "manager")
                    .pathMatchers(HttpMethod.POST, "/v1/schedules/*/publish").hasAnyRole("admin", "manager")
                    .pathMatchers(HttpMethod.POST, "/v1/schedules/*/draft").hasAnyRole("admin", "manager")
                    .pathMatchers(HttpMethod.PUT, "/v1/schedules/*/published").hasRole("admin")
                    // New schedule endpoints with shifts - week-based access
                    .pathMatchers(HttpMethod.GET, "/v1/business-units/*/schedules/week/published").hasAnyRole("admin", "manager", "employee")
                    .pathMatchers(HttpMethod.GET, "/v1/business-units/*/schedules/week").hasAnyRole("admin", "manager")
                    // Comprehensive shifts endpoint - admin and manager only
                    .pathMatchers(HttpMethod.GET, "/v1/business-units/*/shifts/comprehensive").hasAnyRole("admin", "manager")
                    // Shift endpoints - Role hierarchy: admin > manager > employee
                    .pathMatchers(HttpMethod.POST, "/v1/shifts").hasAnyRole("admin", "manager")
                    .pathMatchers(HttpMethod.PUT, "/v1/shifts/**").hasAnyRole("admin", "manager")
                    .pathMatchers(HttpMethod.DELETE, "/v1/shifts/**").hasAnyRole("admin", "manager")
                    // Availability endpoints - All authenticated users can manage availabilities
                    .pathMatchers(HttpMethod.POST, "/v1/availabilities").hasAnyRole("admin", "manager", "employee")
                    .pathMatchers(HttpMethod.PUT, "/v1/availabilities/**").hasAnyRole("admin", "manager", "employee")
                    .pathMatchers(HttpMethod.DELETE, "/v1/availabilities/**").hasAnyRole("admin", "manager", "employee")
                    // All other authenticated users can read
                    .anyExchange().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtDecoder(jwtDecoder())
                       .jwtAuthenticationConverter(ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter()))
                }
            }
            .exceptionHandling { exceptions ->
                exceptions.accessDeniedHandler(accessDeniedHandler())
            }
            .build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOriginPatterns = listOf("*")
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }

    @Bean
    fun jwtDecoder(): ReactiveJwtDecoder {
        logger.info { "Configuring JWT decoder with JWK Set URI: $jwkSetUri" }
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build()
    }

    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter { jwt ->
            // Add detailed logging to see what's in the JWT
            logger.info("JWT Claims: ${jwt.claims}")
            logger.info("JWT Subject: ${jwt.subject}")
            
            val realmAccess = jwt.getClaimAsMap("realm_access")
            val resourceAccess = jwt.getClaimAsMap("resource_access")
            
            logger.info("Realm access: $realmAccess")
            logger.info("Resource access: $resourceAccess")
            
            val authorities = mutableListOf<org.springframework.security.core.GrantedAuthority>()
            
            // Extract realm roles and add ROLE_ prefix
            realmAccess?.get("roles")?.let { roles ->
                if (roles is List<*>) {
                    logger.info("Found realm roles: $roles")
                    roles.filterIsInstance<String>().forEach { role ->
                        // Add both with and without ROLE_ prefix for compatibility
                        authorities.add(org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_$role"))
                        authorities.add(org.springframework.security.core.authority.SimpleGrantedAuthority(role))
                        logger.info("Added authorities for role '$role': ROLE_$role, $role")
                    }
                }
            }
            
            // Extract resource-specific roles and add ROLE_ prefix
            // Note: We're mapping auth-service roles to standard roles for compatibility
            resourceAccess?.forEach { (resource, access) ->
                if (access is Map<*, *>) {
                    access["roles"]?.let { roles ->
                        if (roles is List<*>) {
                            roles.filterIsInstance<String>().forEach { role ->
                                when {
                                    // Map auth-service roles to standard roles for compatibility
                                    resource == "auth-service" && role == "admin" -> {
                                        authorities.add(org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin"))
                                        authorities.add(org.springframework.security.core.authority.SimpleGrantedAuthority("admin"))
                                    }
                                    // Add other resource roles as-is
                                    else -> {
                                        val resourceRole = "${resource}_${role}"
                                        authorities.add(org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_$resourceRole"))
                                        authorities.add(org.springframework.security.core.authority.SimpleGrantedAuthority(resourceRole))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            logger.info("Final extracted authorities from JWT: ${authorities.map { it.authority }}")
            authorities
        }
        return converter
    }

    @Bean
    fun accessDeniedHandler(): ServerAccessDeniedHandler {
        return ServerAccessDeniedHandler { exchange: ServerWebExchange, denied ->
            logger.warn { "Access denied: ${denied.message}" }
            
            val response = exchange.response
            response.statusCode = HttpStatus.FORBIDDEN
            response.headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            
            val errorMessage = """
                {
                    "status": 403,
                    "error": "Access Denied",
                    "message": "Insufficient permissions to access this resource",
                    "path": "${exchange.request.path.value()}"
                }
            """.trimIndent()
            
            val buffer: DataBuffer = response.bufferFactory().wrap(errorMessage.toByteArray())
            response.writeWith(Mono.just(buffer))
        }
    }
}