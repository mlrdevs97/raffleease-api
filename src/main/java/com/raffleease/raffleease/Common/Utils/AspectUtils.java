package com.raffleease.raffleease.Common.Utils;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Utility class for aspect-related operations.
 * Provides common functionality for parameter extraction from method calls.
 */
public class AspectUtils {

    /**
     * Extract a parameter value by name and type from the method call.
     * Supports both @PathVariable annotated parameters and direct parameter name matching.
     * 
     * @param joinPoint the AOP join point
     * @param parameterName the name of the parameter to extract
     * @param type the expected type of the parameter
     * @param <T> the type parameter
     * @return the parameter value, or null if not found or type mismatch
     */
    public static <T> T extractParameterValue(JoinPoint joinPoint, String parameterName, Class<T> type) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            
            // Check if parameter has @PathVariable annotation with the desired name
            PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
            if (pathVariable != null) {
                String pathVarName = pathVariable.value().isEmpty() ? pathVariable.name() : pathVariable.value();
                if (pathVarName.isEmpty()) {
                    pathVarName = parameter.getName();
                }
                if (parameterName.equals(pathVarName) && type.isAssignableFrom(parameter.getType())) {
                    return type.cast(args[i]);
                }
            }
            
            // Fallback: check parameter name directly
            if (parameterName.equals(parameter.getName()) && type.isAssignableFrom(parameter.getType())) {
                return type.cast(args[i]);
            }
        }
        
        return null;
    }

    /**
     * Convenience method to extract associationId parameter from the method call.
     * 
     * @param joinPoint the AOP join point
     * @return the associationId as Long, or null if not found
     */
    public static Long extractAssociationId(JoinPoint joinPoint) {
        return extractParameterValue(joinPoint, "associationId", Long.class);
    }

    /**
     * Convenience method to extract userId parameter from the method call.
     * 
     * @param joinPoint the AOP join point
     * @return the userId as Long, or null if not found
     */
    public static Long extractUserId(JoinPoint joinPoint) {
        return extractParameterValue(joinPoint, "userId", Long.class);
    }
} 