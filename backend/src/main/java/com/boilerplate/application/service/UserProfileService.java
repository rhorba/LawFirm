package com.boilerplate.application.service;

import com.boilerplate.application.dto.request.UpdateProfileRequest;
import com.boilerplate.application.dto.response.UserProfileResponse;
import com.boilerplate.application.mapper.UserProfileMapper;
import com.boilerplate.domain.model.User;
import com.boilerplate.domain.model.UserProfile;
import com.boilerplate.domain.repository.UserProfileRepository;
import com.boilerplate.domain.repository.UserRepository;
import com.boilerplate.presentation.exception.ResourceNotFoundException;
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
