package com.tuanzeebee.springboot.demosecurity.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy; // [QUAN TRỌNG] Import Lazy
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuanzeebee.springboot.demosecurity.dao.UserDTO;
import com.tuanzeebee.springboot.demosecurity.entity.User;
import com.tuanzeebee.springboot.demosecurity.entity.Post;
import com.tuanzeebee.springboot.demosecurity.entity.Comment;
import com.tuanzeebee.springboot.demosecurity.repository.RoleRepository;
import com.tuanzeebee.springboot.demosecurity.repository.UserRepository;
import com.tuanzeebee.springboot.demosecurity.repository.PostRepository;
import com.tuanzeebee.springboot.demosecurity.repository.CommentRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PostRepository postRepository;
    private final CommentService commentService;
    private final PostService postService;

    @Autowired
    public UserService(UserRepository userRepository, RoleRepository roleRepository, 
                       PasswordEncoder passwordEncoder, PostRepository postRepository,
                       @Lazy CommentService commentService, // [SỬA] Thêm @Lazy để phá vòng lặp
                       @Lazy PostService postService) {     // [SỬA] Thêm @Lazy để phá vòng lặp
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.postRepository = postRepository;
        this.commentService = commentService;
        this.postService = postService;
    }

    // --- [QUAN TRỌNG] Hàm này dùng cho AdminController ---
    public User findById(Long id) {
        // Trả về null nếu không tìm thấy, thay vì ném lỗi ngay lập tức
        return userRepository.findById(id).orElse(null);
    }

    // --- [QUAN TRỌNG] Hàm update dùng cho AdminController ---
    public UserDTO updateUser(Long id, User updatedUser) {
        return userRepository.findById(id)
            .map(user -> {
                user.setFirstName(updatedUser.getFirstName());
                user.setLastName(updatedUser.getLastName());
                user.setEmail(updatedUser.getEmail());
                
                // Chỉ cập nhật avatar nếu có giá trị mới
                if (updatedUser.getAvatar() != null) {
                    user.setAvatar(updatedUser.getAvatar());
                }
                
                user.setBio(updatedUser.getBio());
                user.setRoles(updatedUser.getRoles()); 
                
                // Mật khẩu đã được xử lý ở Controller (giữ nguyên hoặc mã hóa mới)
                // Nếu muốn cập nhật pass tại đây, cần check xem updatedUser.getPassword() có khác không
                
                return convertToDTO(userRepository.save(user));
            })
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ... (Các phương thức khác giữ nguyên)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    public UserDTO getUserById(Long id) {
        return userRepository.findById(id).map(this::convertToDTO).orElseThrow(() -> new RuntimeException("User not found"));
    }
    public UserDTO createUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (user.getEmail() != null && !user.getEmail().isEmpty() && userRepository.existsByEmail(user.getEmail())) {
             throw new RuntimeException("Email already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }
    
    @Transactional
    public void deleteUser(Long id) {
        User userToDelete = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + id));
        List<Post> postsLikedByUser = postRepository.findAll().stream()
                .filter(post -> post.getLikedByUsers().contains(userToDelete))
                .collect(Collectors.toList());
        for (Post post : postsLikedByUser) {
            post.getLikedByUsers().remove(userToDelete);
            postRepository.save(post);
        }
        Set<Post> userPosts = userToDelete.getPosts();
        for (Post post : userPosts) {
            postService.deletePost(post.getId()); 
        }
        userToDelete.getSavedRecipes().clear();
        userToDelete.getFollowing().clear(); 
        for(User follower : userToDelete.getFollowers()) {
            follower.getFollowing().remove(userToDelete);
            userRepository.save(follower);
        }
        userToDelete.getFollowers().clear();
        userToDelete.getRoles().clear();
        userRepository.save(userToDelete);
        userRepository.delete(userToDelete);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
    }
    public UserDTO followUser(Long followerId, Long followedId) {
        User follower = userRepository.findById(followerId).orElseThrow(() -> new RuntimeException("Follower not found"));
        User followed = userRepository.findById(followedId).orElseThrow(() -> new RuntimeException("Followed user not found"));
        follower.getFollowing().add(followed);
        userRepository.save(follower);
        return convertToDTO(follower);
    }
    public UserDTO unfollowUser(Long followerId, Long followedId) {
        User follower = userRepository.findById(followerId).orElseThrow(() -> new RuntimeException("Follower not found"));
        User followed = userRepository.findById(followedId).orElseThrow(() -> new RuntimeException("Followed user not found"));
        follower.getFollowing().remove(followed);
        userRepository.save(follower);
        return convertToDTO(follower);
    }
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setAvatar(user.getAvatar());
        dto.setBio(user.getBio());
        dto.setRoles(user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toSet()));
        return dto;
    }
    public UserDTO findByEmail(String email) {
        return userRepository.findByEmail(email).map(this::convertToDTO).orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }
    public long countUsers() { return userRepository.count(); }
    public User save(User user) { return userRepository.save(user); }
}