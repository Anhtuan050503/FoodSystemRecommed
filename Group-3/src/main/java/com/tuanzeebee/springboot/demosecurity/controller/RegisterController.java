package com.tuanzeebee.springboot.demosecurity.controller;

import com.tuanzeebee.springboot.demosecurity.entity.Role;
import com.tuanzeebee.springboot.demosecurity.entity.User;
import com.tuanzeebee.springboot.demosecurity.repository.RoleRepository;
import com.tuanzeebee.springboot.demosecurity.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.HashSet;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/register")
public class RegisterController {

    private final UserService userService;
    private final RoleRepository roleRepository;

    // Regex đơn giản để kiểm tra email
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@(.+)$";

    @Autowired
    public RegisterController(UserService userService, RoleRepository roleRepository) {
        this.userService = userService;
        this.roleRepository = roleRepository;
    }

    @GetMapping
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping
    public String processRegistration(@Valid @ModelAttribute("user") User user, 
                                      BindingResult result, 
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        
        // 1. KIỂM TRA LỖI BỎ TRỐNG (VALIDATION)
        if (result.hasErrors()) {
            model.addAttribute("registrationError", "Please fill in all fields");
            return "register"; 
        }

        // 2. KIỂM TRA ĐỊNH DẠNG EMAIL THỦ CÔNG
        // Nếu không đúng định dạng email, báo lỗi riêng
        if (!isValidEmail(user.getEmail())) {
            model.addAttribute("registrationError", "Invalid email");
            return "register";
        }

        try {
            // [ĐÃ XÓA] Bước kiểm tra trùng username bằng findByUsername gây lỗi System Error
            // Chúng ta sẽ để userService.createUser tự kiểm tra và ném lỗi nếu trùng.

            // 3. Xử lý Role
            Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName("USER");
                    return roleRepository.save(newRole);
                });
            
            user.setRoles(new HashSet<>(Collections.singletonList(userRole)));
            
            // 4. Lưu User (Hàm này sẽ ném Exception nếu username đã tồn tại)
            userService.createUser(user);
            
            // 5. Thành công
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful!");
            return "redirect:/login"; 
            
        } catch (Exception e) {
            // Xử lý thông báo lỗi trùng lặp một cách thân thiện
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("Username already exists")) {
                model.addAttribute("registrationError", "Username already exists");
            } else {
                model.addAttribute("registrationError", "System error: " + errorMessage);
            }
            return "register";
        }
    }

    // Hàm kiểm tra email
    private boolean isValidEmail(String email) {
        return Pattern.compile(EMAIL_REGEX).matcher(email).matches();
    }
}   