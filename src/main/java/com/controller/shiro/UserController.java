package com.controller.shiro;

import com.dto.shiro.LoginRequest;
import com.dto.shiro.UserDto;
import com.model.GroupRole;
import com.model.User;
import com.service.shiro.TokenManagerService;
import com.service.shiro.UserService;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;
    private final TokenManagerService tokenManagerService;
    private final DefaultPasswordService passwordservice;


    public UserController(UserService userService, TokenManagerService tokenManagerService, DefaultPasswordService passwordservice) {
        this.userService = userService;
        this.tokenManagerService = tokenManagerService;
        this.passwordservice = passwordservice;
    }

    @PostMapping("/auth/sign-in")
    public ResponseEntity<Object> login(@RequestBody LoginRequest userRequest){
        Subject subject = SecurityUtils.getSubject();
        UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(userRequest.getUsername(), userRequest.getPassword());
        subject.login(usernamePasswordToken);
        Map<String,Object> authInfo = new HashMap<String, Object>() {{
            put("token", tokenManagerService.createTokenForUser(userRequest.getUsername()));
        }};
        UUID customerAccountId=userService.getCustomerAccountIdByUsername(userRequest.getUsername()).get();
        authInfo.put("customerAccountId",customerAccountId);
        return ResponseEntity.ok(authInfo);
    }

    @PostMapping("/auth/sign-up")
    public ResponseEntity<User> adduser(@RequestBody UserDto dto){
        User user= User.builder()
                .id(UUID.randomUUID())
                .username(dto.getUsername()).email(dto.getEmail())
                .firstName(dto.getFirstName()).lastName(dto.getLastName())
                .nickname(dto.getNickname()).mobile(dto.getMobile())
                .password(passwordservice.encryptPassword(dto.getPassword()))
                .enabled(true).roles(dto.getGroupRoles().stream().map(r -> copyToGroupRoleEntity(r.getCode())).filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .customerAccountId(UUID.fromString(dto.getCustomerAccountId()))
                .build();

        if(userService.isCustomerAccountIdExist(UUID.fromString(dto.getCustomerAccountId())))
        {
            return new ResponseEntity<>(null,HttpStatus.FOUND);
        }
        return new ResponseEntity<>(userService.save(user),HttpStatus.OK);
    }

   // @RequiresPermissions("document:read")
    @GetMapping("/user")
    public ResponseEntity<List<UserDto>> getAll() {
        List<UserDto> list = userService.getUsers().stream().map(this::copyUserEntityToDto).collect(Collectors.toList());
        return new ResponseEntity<>(list, HttpStatus.OK);
    }
    private UserDto copyUserEntityToDto(User userEntity) {
        UserDto userDTO = new UserDto();
        BeanUtils.copyProperties(userEntity, userDTO);
        return userDTO;
    }

    private GroupRole copyToGroupRoleEntity(String roleCode) {
        GroupRole groupRole = new GroupRole();

        Optional<GroupRole> roleOptional = userService.getGroupRoleByCode(roleCode);
        if(roleOptional.isPresent()){
            BeanUtils.copyProperties(roleOptional.get(), groupRole);
            return groupRole;
        }
        return null;
    }
}