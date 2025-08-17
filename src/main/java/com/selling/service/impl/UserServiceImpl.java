package com.selling.service.impl;

import com.selling.dto.UserDto;
import com.selling.dto.get.UserDtoForGet;
import com.selling.model.Otp;
import com.selling.model.Product;
import com.selling.model.User;
import com.selling.repository.OtpRepo;
import com.selling.repository.ProductRepo;
import com.selling.repository.UserRepo;
import com.selling.service.UserService;
import com.selling.util.MailService;
import com.selling.util.ModelMapperConfig;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepo userRepo;
    private final ModelMapperConfig modelMapper;
    private final OtpRepo otpRepo;
    private final ProductRepo productRepo;
    private final MailService mailService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


    public UserServiceImpl(UserRepo userRepo, ModelMapperConfig modelMapper, OtpRepo otpRepo, ProductRepo productRepo, MailService mailService) {
        this.userRepo = userRepo;
        this.modelMapper = modelMapper;
        this.otpRepo = otpRepo;
        this.productRepo = productRepo;
        this.mailService = mailService;
    }

    @Override
    public UserDto getUserById(String id) {
        User byId = userRepo.findUserById(Long.valueOf(id));
        return entityToUserDto(byId);
    }

    @Override
    public UserDto userLogin(UserDto dto) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        List<User> userNames = userRepo.findAllByEmail(dto.getEmail());
        for (User name : userNames) {
            boolean isPasswordMatches = passwordEncoder.matches(dto.getPassword(), name.getPassword());
            if (isPasswordMatches) {
                return entityToUserDto(name);
            }
        }
        return null;
    }

    @Override
    public UserDto findUserByName(String email, String userName) {
        Optional<User> userDetails = userRepo.findByEmail(email);
        return userDetails.map(this::entityToUserDto).orElse(null);
    }

    @Override
    public UserDtoForGet registerUser(UserDto userDto) {
        User user = this.dtoToEntity(userDto);
        System.out.println(user);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus("active");
        user.setRegistration_date(String.valueOf(LocalDateTime.now()));
        Optional<Product> byId = productRepo.findById(Long.valueOf(userDto.getProductId()));
        byId.ifPresent(user::setProduct);
        User save = this.userRepo.save(user);
        return entityToUserDtoForGet(save);
    }

    @Override
    public UserDtoForGet updateUser(UserDto userDto, Long userId) {
        User byId = userRepo.findUserById(userId);
        if (byId == null) {
            return null;
        } else {
            userDto.setId(byId.getId());
            userDto.setStatus(byId.getStatus());
            userDto.setRegistration_date(byId.getRegistration_date());
            User user = dtoToEntity(userDto);
            User save = userRepo.save(user);
            return entityToUserDtoForGet(save);
        }
    }

    @Override
    public List<UserDtoForGet> getAllUser() {
        List<User> all = userRepo.findAll();
        List<UserDtoForGet> allUsers = new ArrayList<>();
        for (User user : all) {
            UserDtoForGet userDto = entityToUserDtoForGet(user);
            allUsers.add(userDto);
        }
        return allUsers;
    }

    //  otp part ===================================

    // Generate random 4-digit OTP
    private String generateOtp() {
        Random random = new Random();
        int otp = 1000 + random.nextInt(9000);
        return String.valueOf(otp);
    }

    // Send OTP to email
    public boolean sendOtpToEmail(String email) {
       try {
           Optional<User> user = userRepo.findByEmail(email);
           if (user.isPresent()) {
               Optional<Otp> byEmail = otpRepo.findByEmail(email);
               byEmail.ifPresent(otp -> otpRepo.deleteById(otp.getId()));

               // Generate new OTP
               String otp = generateOtp();
               LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(5);

               // Save OTP to database
               Otp otpEntity = new Otp(email, otp, expiryTime);
               otpRepo.save(otpEntity);

               // Send OTP via email
               return mailService.sendOtpEmail(email, otp);
           }
           return false;
       }catch (Exception e){
           System.out.println("============");
           System.out.println(e.getMessage());
           return false;
       }
    }

    // Validate OTP
    public boolean validateOtp(String email, String otp) {
        Otp otpEntity = otpRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("OTP not found for this email"));

        if (otpEntity.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired");
        }

        return otpEntity.getOtpCode().equals(otp);
    }

    @Override
    public int getCustomerCount() {
        long count = userRepo.customerCount();
        return (int) count;
    }

    @Override
    public UserDtoForGet changePassword(String email, String newPassword) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        User save = userRepo.save(user);
        return entityToUserDtoForGet(save);
    }


    //  mapper ===================================
    private User dtoToEntity(UserDto dto) {
        return (dto == null) ? null : modelMapper.modelMapper().map(dto, User.class);
    }

    private UserDto entityToUserDto(User byId) {
        return (byId == null) ? null : modelMapper.modelMapper().map(byId, UserDto.class);
    }

    private UserDtoForGet entityToUserDtoForGet(User byId) {
        return (byId == null) ? null : modelMapper.modelMapper().map(byId, UserDtoForGet.class);
    }
}
