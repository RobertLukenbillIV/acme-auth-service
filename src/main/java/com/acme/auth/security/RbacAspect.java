package com.acme.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

@Aspect
@Component
public class RbacAspect {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Before("@annotation(com.acme.auth.security.RequireRole)")
    public void checkRole(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireRole requireRole = method.getAnnotation(RequireRole.class);

        String token = extractToken();
        if (token == null) {
            throw new AccessDeniedException("No authentication token provided");
        }

        List<String> userRoles = jwtTokenProvider.getRolesFromToken(token);
        List<String> requiredRoles = Arrays.asList(requireRole.value());

        boolean hasAccess;
        if (requireRole.requireAll()) {
            hasAccess = userRoles.containsAll(requiredRoles);
        } else {
            hasAccess = userRoles.stream().anyMatch(requiredRoles::contains);
        }

        if (!hasAccess) {
            throw new AccessDeniedException("Insufficient role permissions");
        }
    }

    @Before("@annotation(com.acme.auth.security.RequireScope)")
    public void checkScope(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireScope requireScope = method.getAnnotation(RequireScope.class);

        String token = extractToken();
        if (token == null) {
            throw new AccessDeniedException("No authentication token provided");
        }

        List<String> userScopes = jwtTokenProvider.getScopesFromToken(token);
        if (userScopes == null) {
            throw new AccessDeniedException("No scopes found in token");
        }

        List<String> requiredScopes = Arrays.asList(requireScope.value());

        boolean hasAccess;
        if (requireScope.requireAll()) {
            hasAccess = userScopes.containsAll(requiredScopes);
        } else {
            hasAccess = userScopes.stream().anyMatch(requiredScopes::contains);
        }

        if (!hasAccess) {
            throw new AccessDeniedException("Insufficient scope permissions");
        }
    }

    private String extractToken() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        String bearerToken = request.getHeader("Authorization");
        
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }
}
