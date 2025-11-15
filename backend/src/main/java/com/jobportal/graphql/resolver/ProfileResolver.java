package com.jobportal.graphql.resolver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import com.jobportal.dto.ProfileUpdateDTO;
import com.jobportal.security.CustomUserDetails;
import com.jobportal.service.ProfileService;

import java.util.Map;

@Controller
public class ProfileResolver {

    @Autowired
    private ProfileService profileService;

    @MutationMapping
    public Map<String, Object> updateProfile(
            @Argument Map<String, Object> input,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new RuntimeException("User not authenticated");
        }

        ProfileUpdateDTO updateDTO = new ProfileUpdateDTO();
        if (input.containsKey("name")) {
            updateDTO.setName((String) input.get("name"));
        }
        if (input.containsKey("bio")) {
            updateDTO.setBio((String) input.get("bio"));
        }

        return profileService.updateUserProfile(userDetails.getUser().getId(), updateDTO);
    }
}
