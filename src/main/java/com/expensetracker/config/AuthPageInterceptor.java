package com.expensetracker.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthPageInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        String uri = request.getRequestURI();

        String contextPath = request.getContextPath();

        String path = uri.substring(contextPath.length());

        if (path.equals("/login.html")
                || path.equals("/register.html")
                || path.equals("/forgot-password.html")
                || path.equals("/reset-password.html")) {

            return true;
        }

        if (!path.equals("/") && !path.endsWith(".html")) {
            return true;
        }

        HttpSession session =
                request.getSession(false);

        if (session == null
                || !(session.getAttribute("USER_ID")
                instanceof Long)) {

            response.sendRedirect("/login.html");

            return false;
        }

        return true;
    }
}