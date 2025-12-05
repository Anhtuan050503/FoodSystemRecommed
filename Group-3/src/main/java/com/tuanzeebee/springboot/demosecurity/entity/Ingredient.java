package com.tuanzeebee.springboot.demosecurity.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank; // Import quan trọng
import lombok.Data;

@Data
@Entity
@Table(name = "ingredients")
public class Ingredient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Ingredient name is required") // Bắt lỗi rỗng
    @Column(nullable = false)
    private String name;
    
    private String icon;
}