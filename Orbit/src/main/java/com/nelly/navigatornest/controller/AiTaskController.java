package com.nelly.navigatornest.controller;

import com.nelly.navigatornest.dto.AiSuggestRequest;
import com.nelly.navigatornest.dto.AiSuggestResponse;
import com.nelly.navigatornest.entity.User;
import com.nelly.navigatornest.repository.UserRepository;
import com.nelly.navigatornest.service.AiTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiTaskController {

    private final AiTaskService aiTaskService;
    private final UserRepository userRepository;

    /**
     * 從 JWT（SecurityContext）取得目前登入使用者。
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Unauthorized: no authenticated user");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    // 依使用者心情 / 想法，建議任務、日記文字與歌詞靈感
    // saveToTasks=true 時存 Task，並同步存 Diary（關聯 Task）
    // saveToDiary=true 可只存日記
    @PostMapping("/suggest")
    public ResponseEntity<AiSuggestResponse> suggest(@RequestBody AiSuggestRequest request) {
        if (request.getUserInput() == null || request.getUserInput().isBlank()) {
            throw new RuntimeException("userInput must not be blank");
        }

        boolean saveToTasks = request.isSaveToTasks();
        boolean saveToDiary = request.isSaveToDiary();

        Long userId = null;
        if (saveToTasks || saveToDiary) {
            userId = getCurrentUser().getId();
        }

        AiSuggestResponse response = aiTaskService.suggest(
                request.getUserInput().trim(),
                saveToTasks,
                saveToDiary,
                userId
        );
        return ResponseEntity.ok(response);
    }
}
