package com.project.userauthservice.entity.user;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "genders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Genders {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Column(name = "display_order")
    private Integer displayOrder;
    
    @Column(nullable = false)
    private boolean active = true;
}