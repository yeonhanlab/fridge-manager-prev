package com.lineacademy.fridgemanagerspring.service;

import com.lineacademy.fridgemanagerspring.domain.fridge.Fridge;
import com.lineacademy.fridgemanagerspring.domain.user.User;
import com.lineacademy.fridgemanagerspring.dto.user.request.CreateUserRequest;
import com.lineacademy.fridgemanagerspring.repository.FridgeRepository;
import com.lineacademy.fridgemanagerspring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor

public class UserService {
    private final UserRepository userRepository;
    private final FridgeRepository fridgeRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional   // 진행 중간에 에러가 만약 발생되면 DB 작업한 것을 롤백하는 어노테이션
    public User createUser(CreateUserRequest request) {
        // 우리가 Express를 사용할 때에는 schema.prisma만 만들고,
        // generate를 실행하면 우리가 사용할 수 있는 모든 메서드가 작성되어서 만들어지고 사용만 했으면 됨

        // Java Spring-Boot, Hibernate에서는 자동으로 만들어주는건 findById (PrimaryKey를 대상으로 검색)만 있고
        // 나머지는 직접 만들어야 함. 대신 '메서드명'을 작성하면 해당 코드는 알아서 만들어줌

        // 이러한 구조에서 데이터베이스 등의 저장소에 접근하는 기능 모음 클래스들을 "레포지토리(repository)"라고 함

        // 이메일 중복 체크
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("ALREADY_EXISTS_EMAIL");
        }

        // 닉네임 중복 체크
        if (userRepository.existsByNickName(request.getNickname())) {
            throw new IllegalArgumentException("ALREADY_EXISTS_NICKNAME");
        }

        // 입력된 birthdate는 String이니까 이걸 LocalDate 객체로 변환
        LocalDate parsedBirthdate = null;
        // 사용자가 입력한 birthdate가 null이 아니면서 빈값("")도 아닐 경우
        if (request.getBirthdate() != null && request.getBirthdate().isBlank()) {
            parsedBirthdate = LocalDate.parse(request.getBirthdate(), DateTimeFormatter.ISO_DATE);
        }

        // 사용자 정보를 데이터베이스에 저장
        // 1. 사용자 저장
        User user = User.builder()
                .nickname(request.getNickname())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .birthdate(parsedBirthdate)
                .build();
        userRepository.save(user);

        // 2. 기본 냉장고 저장
        Fridge defaultFridge = Fridge.builder()
                .name("내 냉장고")
                .user(user)
                .build();
        fridgeRepository.save(defaultFridge);

        return user;

    }
}
