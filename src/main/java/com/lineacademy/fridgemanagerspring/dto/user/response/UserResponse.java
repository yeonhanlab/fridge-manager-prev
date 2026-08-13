package com.lineacademy.fridgemanagerspring.dto.user.response;


import com.lineacademy.fridgemanagerspring.domain.enums.RoleType;
import com.lineacademy.fridgemanagerspring.domain.user.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;


// Entity 타입의 객체를 그대로 내뱉는 것 자체에도 에러가 발생될 여지가 있긴 하지만,
// 특히, "password"처럼 response에 포함하지 말아야 되느느 정보들을 걸러내기 위헤
// response에도 DTO를 이용함

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String nickname;
    private String email;
    private LocalDate birthDate;
    private RoleType role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // from 메서드를 쓰면 User 타입의 객체를 받아, UserResponse의 객체로 반환하여 리턴
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .birthDate(user.getBirthdate())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdateAt())
                .build();
    }
}
