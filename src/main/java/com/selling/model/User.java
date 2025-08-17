package com.selling.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;
    private String name;
    private String email;
    private String telephone;
    private String role;
    private String registration_date;
    private String status;
    private String type;
    private String password;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "product_id")
    private Product product;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Customer> customers;


    public User(Long id, String name, String email, String telephone, String role, String registration_date, String status, String type, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.telephone = telephone;
        this.role = role;
        this.registration_date = registration_date;
        this.status = status;
        this.type = type;
        this.password = password;
    }
}