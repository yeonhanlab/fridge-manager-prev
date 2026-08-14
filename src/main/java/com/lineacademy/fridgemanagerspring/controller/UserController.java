package com.lineacademy.fridgemanagerspring.controller;


import com.lineacademy.fridgemanagerspring.domain.user.User;
import com.lineacademy.fridgemanagerspring.dto.user.request.CreateUserRequest;
import com.lineacademy.fridgemanagerspring.dto.user.request.LoginRequest;
import com.lineacademy.fridgemanagerspring.dto.user.request.UpdateUserRequest;
import com.lineacademy.fridgemanagerspring.dto.user.response.UserResponse;
import com.lineacademy.fridgemanagerspring.service.UserService;
import com.lineacademy.fridgemanagerspring.utils.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.aspectj.apache.bcel.classfile.Code;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController  // 이 클래스가 웹서비스를 할 때 이용되는 컨트롤러임을 명시
@RequestMapping("/users")  // /users 라는 주소로 Request가 오면 이 컨트롤러에 도달
@RequiredArgsConstructor     // final 필드나 @Nonnull 필드가 붙은 것들을 매개변수로 한
                            // 매개변수 생성자를 자동으로 생성해주는 어느테이션
public class UserController {
    // 멤버변수
    private final UserService userService;     // Java에서는 객체를 만들어야 실행이 가능하니까
    private final JwtUtil jwtUtil;             // Bean이기 때문에 새로 생성하는게 아니라 있는 걸 불러오게 됨

    // 멤버메서드
    @PostMapping("/create")  // class의 매핑정보인 "/users"뒤에 "/create"가 붙고, POST 방식이면 이 메서드 실행
    // ResponseEntity<T> : Spring - Boot Web Service에서 응답에 대한 타입
    // T 자리에는 response.body(실제 내용이 기록되는 편지지)의 타입이 들어가야 함
    public ResponseEntity<Map<String, Object>> createUser(
            // Spring-Boot에서는 컨트롤러의 메서드를 실행할 때,
            // 자동으로 req.body 값이 매개변수로 들어옴

            //@Valid = 이 매개변수에 대해 검증 절차를 실행할 것이고, 실패하면 GlobalExceptionHandler로 에러를 던질 것이야
            // @RequestBody 는 이 매개변수에 request.body 내용을 넣어줘
            // 스트링으로 되어져있던게 객체가 되어서 request에 들어감
            @Valid @RequestBody CreateUserRequest request
            ) {
        try {
            // 서비스에 request를 그대로 넘겨서, 생성 요청을 할 것이고
            // 서비스는 생성이 끝난 결과(생성 '된' User 객체)를 리턴하게 만들 것임
            User user = userService.createUser(request);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "message", "성공적으로 회원가입 되었습니다.",
                            "data", UserResponse.from(user)
                    ));
        } catch (RuntimeException e) {
            if (e.getMessage().equals("ALREADY_EXISTS_EMAIL"))
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of(
                                "message", "이미 가입된 이메일입니다."
                        ));
            if (e.getMessage().equals("ALREADY_EXISTS_NICKNAME"))
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of(
                                "message", "이미 사용중인 이메일입니다."
                        ));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "서버 에러가 발생되었습니다."
                    ));


        }

    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request
    ) {
         try {
             // 1. 사용자가 입력해온 값을 DB에서 조회해서 있는지 확인
             User user = userService.login(request);

             // 2. 토큰을 생성해서 response 전달
             String token = jwtUtil.generateToken(user.getId());

             // ResponseEntity.status(200).body(Map.of(어쩌구, 저쩌구)
             // ResponseEntity.ok(Map.of(어쩌구, 저쩌구))로 쓸 수 있음
             return ResponseEntity.ok(Map.of(
                     "message", "로그인에 성공했습니다.",
                     "date", Map.of(
                             "user", UserResponse.from(user),
                             "token", token
                     )
             ));
         } catch (RuntimeException e) {
             if (e.getMessage().equals("INVALID_CREDENTIALS")) {
                 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                         "message", "아이디 또는 비밀번호가 일치하지 않습니다."
                 ));
             }
             return ResponseEntity.status(500).body(Map.of(
                     "message", "서버 에러"
             ));
         }
    }

    // 이미 SecurityConfig에서 사용자를 확인하였고, 로그인된 요청이라는걸 알기 때문에 여기에 도달할 수 있는 건 맞음
    @PreAuthorize("isAuthenticated()")    // 인증된 회원인지 여부를 검사하는 어노테이션
    @PatchMapping("/update")
    public ResponseEntity<Map<String, Object>> updateUser(
            @AuthenticationPrincipal Long currentUserId, // 로그인 사용자 ID를 꺼내줌
            @Valid @RequestBody UpdateUserRequest request
            ) {
        try {
            User updatedUser = userService.updateUser(currentUserId, request);
            return ResponseEntity.ok(Map.of(
                    "message", "회원정보가 성공적으로 수정되었습니다.",
                    "dadta", UserResponse.from(updatedUser)
            ));

        } catch (RuntimeException e) {
            if (e.getMessage().equals("USER_NOT_FOUND"))
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "message", "해당 사용자를 찾을 수 없습니다."
                        ));
            if (e.getMessage().equals("DUPLICATED_NICKNAME"))
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of(
                                "message", "이미 사용중인 닉네임입니다."
                        ));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "서버 에러가 발생되었습니다."
                    ));

        }
    }

}
