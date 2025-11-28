package com.tuanzeebee.springboot.demosecurity.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.support.SessionFlashMapManager;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, 
                                        Authentication authentication) throws IOException, ServletException {
        
        HttpSession session = request.getSession();
        session.setAttribute("user", authentication.getName());
        
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isManager = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
        
        String redirectUrl = "";
        String messageContent = "";

        if (isAdmin) {
            redirectUrl = "/admin/dashboard";
            messageContent = "Welcome admin!";
        } else if (isManager) {
            redirectUrl = "/manager/dashboard";
            messageContent = "Welcome manager!";
        } else {
            redirectUrl = "/"; 
            messageContent = "Welcome user!";
        }

        // --- SỬA ĐỔI QUAN TRỌNG ---
        // Gửi riêng 2 biến thay vì gom vào Map để dễ hiển thị bên HTML
        FlashMap flashMap = new FlashMap();
        flashMap.put("successMessage", messageContent); // Dùng tên biến successMessage cho nhất quán
        
        FlashMapManager flashMapManager = new SessionFlashMapManager();
        flashMapManager.saveOutputFlashMap(flashMap, request, response);

        response.sendRedirect(request.getContextPath() + redirectUrl);
    }
}