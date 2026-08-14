package com.lineacademy.fridgemanagerspring.service;

import com.lineacademy.fridgemanagerspring.domain.fridge.Fridge;
import com.lineacademy.fridgemanagerspring.domain.user.User;
import com.lineacademy.fridgemanagerspring.dto.user.request.CreateUserRequest;
import com.lineacademy.fridgemanagerspring.dto.user.request.LoginRequest;
import com.lineacademy.fridgemanagerspring.dto.user.request.UpdateUserRequest;
import com.lineacademy.fridgemanagerspring.repository.FridgeRepository;
import com.lineacademy.fridgemanagerspring.repository.UserRepository;
import jakarta.validation.Valid;
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
        if (userRepository.existsByNickname(request.getNickname())) {
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
        // user 변수에 저장된 객체는 repository에 기반을 두지 않은,
        // 신규 user 객체 => save를 명시적으로 해줬어야 함
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

    @Transactional
    public User login(LoginRequest request) {
        // 1. 받아온 emal값을 통해 사용자가 있는지 확인하고
        // 함수처럼 만들어서 쓸 수 있는게 Java에서 지원되지만 함수는 아니고
        // 람다 표현식 () ->
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("INVALID_CREDENTIALS"));

        // 2. 사용자가 존재한다면, 탈퇴된 회원인지를 걷사하고
        if (user.getDeletedAt() != null) {
            throw new RuntimeException("INVALID_CREDENTIALS");
        }

        // 3. 비밀번호가 일치하는지 확인하고
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("INVALID_CREDENTIALS");
        }

        return user;
    }

    @Transactional
    public User updateUser(Long currentUserId, UpdateUserRequest request) {
        // 1. 사용자가 존재하는지
        // 여기 user 변수에 저장이 된 User Entity 객체는 Respository에서 가져온 값이 저장되어져 있음
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));

        // 2. 탈퇴는 하지 않았는지
        if (user.getDeletedAt() != null) {
            throw new RuntimeException("USER_NOT_FOUND");
        }

        // 3. 변경하려는 닉네임이 겹치지 않는지
        if (request.getNickname() != null) {
            if (userRepository.existsByNicknameAndIdNot(request.getNickname(), currentUserId)) {
                throw new RuntimeException("DUPLICATED_NICKNAME");
            }
            user.updateNickname(request.getNickname());  // 변수의 값을 변경
        }

        // 4. birthdate에 대해서 변환해서 업데이트
        if (request.getBirthdate() != null && !request.getBirthdate().isBlank()) {
            LocalDate parsedBirthdate = LocalDate.parse(request.getBirthdate(), DateTimeFormatter.ISO_DATE);
            user.updateBirthdate(parsedBirthdate);   // 변수의 값을 변경
        }

        // 디비에 쓰지않고, 리턴으로 끝냈음. 엔티티의 특성!!
        return user;

        // repository에서 가져온 내용을 할당한 user 객체의 값이 변경된 상태에서 메서드 실행이 끝나면
        // 자동으로 디비의 값도 업데이트 함 => Dirty Check라고 함

    }
}
