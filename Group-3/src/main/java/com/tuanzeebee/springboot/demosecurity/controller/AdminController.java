package com.tuanzeebee.springboot.demosecurity.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tuanzeebee.springboot.demosecurity.dao.IngredientDTO;
import com.tuanzeebee.springboot.demosecurity.dao.RecipeDTO;
import com.tuanzeebee.springboot.demosecurity.entity.Ingredient;
import com.tuanzeebee.springboot.demosecurity.entity.Role;
import com.tuanzeebee.springboot.demosecurity.entity.User;
import com.tuanzeebee.springboot.demosecurity.repository.RoleRepository;
import com.tuanzeebee.springboot.demosecurity.service.IngredientService;
import com.tuanzeebee.springboot.demosecurity.service.RecipeService;
import com.tuanzeebee.springboot.demosecurity.service.UserService;
import com.tuanzeebee.springboot.demosecurity.service.PostService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final RoleRepository roleRepository;
    private final IngredientService ingredientService;
    private final RecipeService recipeService;
    private final PostService postService;

    // Regex kiểm tra email
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@(.+)$";

    @Autowired
    public AdminController(UserService userService, 
                           RoleRepository roleRepository, 
                           IngredientService ingredientService,
                           RecipeService recipeService,
                           PostService postService) {
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.ingredientService = ingredientService;
        this.recipeService = recipeService;
        this.postService = postService;
    }
    
    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model, Authentication authentication) {
        long totalUsers = userService.countUsers();
        long totalRecipes = recipeService.countRecipes();
        long totalIngredients = ingredientService.countIngredients();
        long totalComments = postService.countPosts();

        String username = authentication.getName();
        User currentUser = userService.findByUsername(username);
        model.addAttribute("user", currentUser);

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalRecipes", totalRecipes);
        model.addAttribute("totalIngredients", totalIngredients);
        model.addAttribute("totalComments", totalComments);

        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String userManagement(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("newUser", new User());
        return "admin/users";
    }
    
    // --- PHƯƠNG THỨC ADD USER ĐÃ CẬP NHẬT ---
    @PostMapping("/users/add")
    public String addUser(@Valid @ModelAttribute("newUser") User user, 
                          BindingResult result,
                          // Role không bắt buộc (required=false) để ta tự kiểm tra
                          @RequestParam(value = "roleIds", required = false) List<Long> roleIds, 
                          RedirectAttributes redirectAttributes) {
        
        // 1. Kiểm tra Validation:
        // - Nếu có lỗi @NotBlank (trường trống)
        // - HOẶC nếu không chọn Role nào (roleIds bị null hoặc rỗng)
        if (result.hasErrors() || roleIds == null || roleIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", 
                Map.of("type", "alert-danger", "content", "Please fill in all fields"));
            return "redirect:/admin/users";
        }

        // 2. Kiểm tra định dạng Email thủ công
        if (!isValidEmail(user.getEmail())) {
            redirectAttributes.addFlashAttribute("message", 
                Map.of("type", "alert-danger", "content", "Invalid email"));
            return "redirect:/admin/users";
        }

        try {
            Set<Role> roles = roleIds.stream()
                    .map(roleId -> roleRepository.findById(roleId).orElseThrow())
                    .collect(Collectors.toSet());
            user.setRoles(roles);
            userService.createUser(user);
            redirectAttributes.addFlashAttribute("message", 
                Map.of("type", "alert-success", "content", "Thêm người dùng thành công!"));
        } catch (RuntimeException e) {
            // Xử lý lỗi trùng username hoặc lỗi khác từ Service
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("Username already exists")) {
                 redirectAttributes.addFlashAttribute("message", 
                    Map.of("type", "alert-danger", "content", "Username already exists"));
            } else {
                 redirectAttributes.addFlashAttribute("message", 
                    Map.of("type", "alert-danger", "content", errorMsg != null ? errorMsg : "Error creating user"));
            }
        }
        return "redirect:/admin/users";
    }
    
    // Hàm phụ trợ kiểm tra email
    private boolean isValidEmail(String email) {
        return email != null && Pattern.compile(EMAIL_REGEX).matcher(email).matches();
    }

    // ... Các phương thức khác (Update, Delete, Recipe, Ingredient...) giữ nguyên
    
     @PostMapping("/users/update/{id}")
    public String updateUser(@PathVariable Long id, 
                             @ModelAttribute User user, 
                             @RequestParam List<Long> roleIds, 
                             @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                             RedirectAttributes redirectAttributes) {
        try {
            // 1. Validation: Kiểm tra các trường bắt buộc bị bỏ trống
            if (user.getFirstName() == null || user.getFirstName().trim().isEmpty() ||
                user.getLastName() == null || user.getLastName().trim().isEmpty() ||
                user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                
                redirectAttributes.addFlashAttribute("message", 
                    Map.of("type", "alert-danger", "content", "Please fill out all field"));
                return "redirect:/admin/users";
            }

           if (!isValidEmail(user.getEmail())) {
                redirectAttributes.addFlashAttribute("message", 
                    Map.of("type", "alert-danger", "content", "Invalid email"));
                return "redirect:/admin/users";
            }

            
            // Kiểm tra User có tồn tại không
            User existingUser = userService.findById(id);
            if (existingUser == null) {
                throw new RuntimeException("User not found");
            }

            Set<Role> roles = roleIds.stream()
                    .map(roleId -> roleRepository.findById(roleId).orElseThrow())
                    .collect(Collectors.toSet());
            user.setRoles(roles);

            // 3. Xử lý Avatar (Không check size/type, chỉ lưu nếu có file)
            if (avatarFile != null && !avatarFile.isEmpty()) {
                String avatarPath = saveAvatar(avatarFile);
                user.setAvatar(avatarPath);
            } else {
                user.setAvatar(existingUser.getAvatar());
            }

            // Giữ lại password cũ
            user.setPassword(existingUser.getPassword());

            // Cập nhật User
            userService.updateUser(id, user);
            
            // 4. Thành công
            redirectAttributes.addFlashAttribute("message", 
                Map.of("type", "alert-success", "content", "User update successfully"));

        } catch (RuntimeException | IOException e) {
            // Xử lý lỗi trùng email hoặc lỗi khác
            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("Duplicate entry") || errorMsg.contains("Email already exists"))) {
                redirectAttributes.addFlashAttribute("message", 
                    Map.of("type", "alert-danger", "content", "Email already exists"));
            } else {
                redirectAttributes.addFlashAttribute("message", 
                    Map.of("type", "alert-danger", "content", errorMsg != null ? errorMsg : "Error updating user"));
            }
        }
        return "redirect:/admin/users";
    }
     private String saveAvatar(MultipartFile file) throws java.io.IOException {
        String uploadDir = "src/main/resources/static/uploads/avatars";
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if(originalFilename != null && originalFilename.contains(".")) {
             fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + fileExtension;
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);
        return "/uploads/avatars/" + fileName;
    }
    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("message", 
                Map.of("type", "alert-success", "content", "Xóa người dùng thành công!"));
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("message", 
                Map.of("type", "alert-danger", "content", e.getMessage()));
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/ingredients")
    public String showIngredientsPage(Model model) {
        model.addAttribute("ingredients", ingredientService.getAllIngredients());
        return "admin/ingredients";
    }
    
    @PostMapping("/ingredients/add")
    public String addIngredient(Ingredient ingredient, Model model, RedirectAttributes redirectAttributes) {
        try {
            ingredientService.createIngredient(ingredient);
            redirectAttributes.addFlashAttribute("message", 
                Map.of("type", "alert-success", "content", "Thêm nguyên liệu thành công!"));
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("message", 
                Map.of("type", "alert-danger", "content", e.getMessage()));
        }
        return "redirect:/admin/ingredients";
    }
    
    @PostMapping("/ingredients/update")
    public String updateIngredient(Ingredient ingredient, RedirectAttributes redirectAttributes) {
        try {
            ingredientService.updateIngredient(ingredient.getId(), ingredient);
            redirectAttributes.addFlashAttribute("message", 
                Map.of("type", "alert-success", "content", "Cập nhật nguyên liệu thành công!"));
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("message", 
                Map.of("type", "alert-danger", "content", e.getMessage()));
        }
        return "redirect:/admin/ingredients";
    }
    
    @PostMapping("/ingredients/delete")
    public String deleteIngredient(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        try {
            ingredientService.deleteIngredient(id);
            redirectAttributes.addFlashAttribute("message", 
                Map.of("type", "alert-success", "content", "Xóa nguyên liệu thành công!"));
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("message", 
                Map.of("type", "alert-danger", "content", e.getMessage()));
        }
        return "redirect:/admin/ingredients";
    }

    @GetMapping("/recipes")
    public String showRecipesPage(Model model) {
        List<RecipeDTO> recipes = recipeService.getAllRecipes();
        
        recipes.forEach(recipe -> {
            if (recipe.getIngredients() == null) {
                recipe.setIngredients(new HashSet<>());
            }
        });
        
        model.addAttribute("recipes", recipes);
        model.addAttribute("ingredients", ingredientService.getAllIngredients());
        return "admin/recipes";
    }

    @PostMapping("/recipes/add")
    public String addRecipe(@RequestParam("name") String name,
                           @RequestParam("description") String description,
                           @RequestParam(value = "ingredients", required = false) List<Long> ingredientIds,
                           @RequestParam("imageFile") MultipartFile imageFile,
                           RedirectAttributes redirectAttributes) {
        try {
            RecipeDTO recipeDTO = new RecipeDTO();
            recipeDTO.setName(name);
            recipeDTO.setDescription(description);
            
            if (!imageFile.isEmpty()) {
                String imagePath = saveImage(imageFile);
                recipeDTO.setImage(imagePath);
            }
            
            if (ingredientIds != null && !ingredientIds.isEmpty()) {
                Set<IngredientDTO> ingredients = ingredientIds.stream()
                        .map(id -> {
                            IngredientDTO dto = new IngredientDTO();
                            dto.setId(id);
                            return dto;
                        })
                        .collect(Collectors.toSet());
                recipeDTO.setIngredients(ingredients);
            } else {
                recipeDTO.setIngredients(new HashSet<>());
            }
            
            recipeService.createRecipe(recipeDTO);
            redirectAttributes.addFlashAttribute("message", 
                Map.of("type", "alert-success", "content", "Thêm công thức thành công!"));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", 
                Map.of("type", "alert-danger", "content", e.getMessage()));
        }
        return "redirect:/admin/recipes";
    }
    
    @PostMapping("/recipes/update")
    public String updateRecipe(@RequestParam("id") Long id,
                              @RequestParam("name") String name,
                              @RequestParam("description") String description,
                              @RequestParam(value = "ingredients", required = false) List<Long> ingredientIds,
                              @RequestParam("imageFile") MultipartFile imageFile,
                              @RequestParam(value = "currentImage", required = false) String currentImage,
                              RedirectAttributes redirectAttributes) {
        try {
            RecipeDTO recipeDTO = recipeService.getRecipeById(id);
            recipeDTO.setName(name);
            recipeDTO.setDescription(description);
            
            if (!imageFile.isEmpty()) {
                String imagePath = saveImage(imageFile);
                recipeDTO.setImage(imagePath);
            } else if (currentImage != null && !currentImage.isEmpty()) {
                recipeDTO.setImage(currentImage);
            }
            
            if (ingredientIds != null && !ingredientIds.isEmpty()) {
                Set<IngredientDTO> ingredients = ingredientIds.stream()
                        .map(ingredientId -> {
                            IngredientDTO dto = new IngredientDTO();
                            dto.setId(ingredientId);
                            return dto;
                        })
                        .collect(Collectors.toSet());
                recipeDTO.setIngredients(ingredients);
            } else {
                recipeDTO.setIngredients(new HashSet<>());
            }
            
            recipeService.updateRecipe(id, recipeDTO);
            redirectAttributes.addFlashAttribute("message", 
                Map.of("type", "alert-success", "content", "Cập nhật công thức thành công!"));
        } catch (RuntimeException | java.io.IOException e) {
            redirectAttributes.addFlashAttribute("message", 
                Map.of("type", "alert-danger", "content", e.getMessage()));
        }
        return "redirect:/admin/recipes";
    }

    @GetMapping("/recipes/delete/{id}")
    public String deleteRecipe(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            recipeService.deleteRecipe(id);
            redirectAttributes.addFlashAttribute("message", 
                Map.of("type", "alert-success", "content", "Xóa công thức thành công!"));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Có lỗi xảy ra khi xóa công thức";
            redirectAttributes.addFlashAttribute("message", 
                Map.of("type", "alert-danger", "content", errorMessage));
        }
        return "redirect:/admin/recipes";
    }
    
    private String saveImage(MultipartFile file) throws java.io.IOException {
        String uploadDir = "src/main/resources/static/uploads/recipes";
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString() + fileExtension;
        
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);
        
        return "/uploads/recipes/" + fileName;
    }
}