package com.pm4.istp.controller;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pm4.istp.domain.entites.User;
import com.pm4.istp.dto.PublicTeamMemberDto;
import com.pm4.istp.mappers.UserMapper;
import com.pm4.istp.service.UserService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PublicControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private PublicController publicController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(publicController).build();
    }

    @Test
    void getTeamMembers_returnsEmptyList_whenEmailsNotConfigured() throws Exception {
        ReflectionTestUtils.setField(publicController, "teamEmailsRaw", "");
        when(userService.getTeamMembers(List.of())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/public/team"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getTeamMembers_returnsMembers_whenEmailsConfigured() throws Exception {
        ReflectionTestUtils.setField(publicController, "teamEmailsRaw", "alice@example.com , bob@example.com");

        User alice = new User();
        alice.setId(UUID.randomUUID());
        alice.setName("Alice");
        alice.setEmail("alice@example.com");

        User bob = new User();
        bob.setId(UUID.randomUUID());
        bob.setName("Bob");
        bob.setEmail("bob@example.com");

        PublicTeamMemberDto aliceDto = new PublicTeamMemberDto("Alice", null, "Developer");
        PublicTeamMemberDto bobDto = new PublicTeamMemberDto("Bob", null, "Designer");

        when(userService.getTeamMembers(List.of("alice@example.com", "bob@example.com")))
                .thenReturn(List.of(alice, bob));
        when(userMapper.toPublicTeamMemberDto(alice)).thenReturn(aliceDto);
        when(userMapper.toPublicTeamMemberDto(bob)).thenReturn(bobDto);

        mockMvc.perform(get("/api/v1/public/team"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Alice"))
                .andExpect(jsonPath("$[1].name").value("Bob"));
    }

    @Test
    void getTeamMembers_trimsWhitespaceAndFiltersBlankEmails() throws Exception {
        ReflectionTestUtils.setField(publicController, "teamEmailsRaw", " alice@example.com , , bob@example.com ");

        User alice = new User();
        alice.setId(UUID.randomUUID());
        alice.setName("Alice");
        alice.setEmail("alice@example.com");

        PublicTeamMemberDto aliceDto = new PublicTeamMemberDto("Alice", null, null);

        when(userService.getTeamMembers(List.of("alice@example.com", "bob@example.com")))
                .thenReturn(List.of(alice));
        when(userMapper.toPublicTeamMemberDto(alice)).thenReturn(aliceDto);

        mockMvc.perform(get("/api/v1/public/team"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Alice"));
    }

    @Test
    void getTeamMembers_isAccessibleWithoutAuthentication() throws Exception {
        ReflectionTestUtils.setField(publicController, "teamEmailsRaw", "");
        when(userService.getTeamMembers(anyList())).thenReturn(List.of());

        // No auth header provided — endpoint should return 200, not 401/403
        mockMvc.perform(get("/api/v1/public/team"))
                .andExpect(status().isOk());
    }
}
