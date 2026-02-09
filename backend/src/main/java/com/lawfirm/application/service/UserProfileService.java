package com.lawfirm.application.service;

import com.lawfirm.application.dto.request.UpdateProfileRequest;
import com.lawfirm.application.dto.response.UserProfileResponse;
import com.lawfirm.application.mapper.UserProfileMapper;
import com.lawfirm.domain.model.User;
import com.lawfirm.domain.model.UserProfile;
import com.lawfirm.domain.repository.UserProfileRepository;
import com.lawfirm.domain.repository.UserRepository;
import com.lawfirm.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final UserProfileMapper userProfileMapper;

    public UserProfileResponse getProfile(Long userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User profile not found for user id: " + userId));
        return userProfileMapper.toResponse(profile);
    }

    @Transactional
    public UserProfileResponse upsertProfile(Long userId, UpdateProfileRequest request) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseGet(() -> createNewProfile(userId));

        userProfileMapper.updateEntityFromRequest(request, profile);
        UserProfile savedProfile = userProfileRepository.save(profile);
        
        return userProfileMapper.toResponse(savedProfile);
    }

    private UserProfile createNewProfile(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return UserProfile.builder()
            .user(user)
            .build();
    }
}
