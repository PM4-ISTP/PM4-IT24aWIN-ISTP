package com.pm4.istp.user.services;

import com.pm4.istp.user.db.entities.User;
import com.pm4.istp.user.dto.UpdateUserProfileRequestDto;
import java.util.Collection;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;

public interface UserProfileService {
  User getProfile(UUID userId);

  User updateProfile(
      UUID actorUserId,
      Collection<? extends GrantedAuthority> actorAuthorities,
      UUID targetUserId,
      UpdateUserProfileRequestDto request);
}
