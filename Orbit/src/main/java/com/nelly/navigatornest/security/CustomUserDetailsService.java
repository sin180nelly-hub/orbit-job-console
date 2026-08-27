package com.nelly.navigatornest.security;

import com.nelly.navigatornest.entity.User;
import com.nelly.navigatornest.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;


/*
職責分離（Separation of Concerns）
service 資料夾 → 放業務邏輯（例如建立任務、查詢任務、更新狀態等）
security 資料夾 → 放認證與授權相關的類別（JWT、Security Config、UserDetailsService 等）

CustomUserDetailsService 的本質
它雖然名字有 "Service"，但它不是業務 Service。
它是 Spring Security 專用的 UserDetailsService 實作，職責是「從資料庫載入使用者資訊給 Security 使用」。
把這種 Security 專屬的類別放在 security 資料夾，比較清楚。

*/
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}