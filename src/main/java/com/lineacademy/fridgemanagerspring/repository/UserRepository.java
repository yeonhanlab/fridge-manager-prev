package com.lineacademy.fridgemanagerspring.repository;

import com.lineacademy.fridgemanagerspring.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// JpaRepository<엔티티 클래스, PK타입>을 상속함
public interface UserRepository extends JpaRepository<User, Long> {
    // String 이메일을 통해 검색해오는 메서드
    // SELECT * FROM User WHERE email = ""
    Optional<User> findByEmail(String email);


    // Nickname을 기준으로 존재유무를 확인하는 메서드
    // SELECT count(*) FROM User WHERE nickname = ""  => 없으면 0, 있으면 양수가 나옴 => 0일때 fasle, 양수일 때 true
    boolean existsByNickname(String nickname);

    // 내 ID를 가진 데이터를 제외하고 해당 닉네임이 존재하는지 검사하는 메서드
    boolean existsByNicknameAndIdNot(String nickname, Long id);
}
