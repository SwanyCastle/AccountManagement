package com.account.controller;

import com.account.dto.AccountDto;
import com.account.dto.AccountInfo;
import com.account.dto.CreateAccount;
import com.account.dto.DeleteAccount;
import com.account.exception.AccountException;
import com.account.service.AccountService;
import com.account.type.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {
    @MockBean
    private AccountService accountService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void successCreateAccount() throws Exception {
        // given
        given(accountService.createAccount(anyLong(), anyLong()))
                .willReturn(AccountDto.builder()
                        .userId(1L)
                        .accountNumber("1234567890")
                        .registeredAt(LocalDateTime.now())
                        .unRegisteredAt(LocalDateTime.now())
                        .build());

        // when
        // then
        mockMvc.perform(post("/account")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new CreateAccount.Request(3333L, 1111L)
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andDo(print());
    }

    @Test
    void successDeleteAccount() throws Exception {
        // given
        given(accountService.deleteAccount(anyLong(), anyString()))
                .willReturn(AccountDto.builder()
                        .userId(1L)
                        .accountNumber("1234567890")
                        .registeredAt(LocalDateTime.now())
                        .unRegisteredAt(LocalDateTime.now())
                        .build());

        // when
        // then
        mockMvc.perform(delete("/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DeleteAccount.Request(3333L, "1111111111")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andDo(print());
    }

    @Test
    void successGetAccountsByUserId() throws Exception {
        // given
        List<AccountDto> accountDtoList =
                Arrays.asList(
                        AccountDto.builder()
                                .accountNumber("1111111111")
                                .balance(1000L)
                                .build()
                        , AccountDto.builder()
                                .accountNumber("2222222222")
                                .balance(2000L)
                                .build()
                        , AccountDto.builder()
                                .accountNumber("3333333333")
                                .balance(3000L)
                                .build()
                );

        given(accountService.getAccountsByUserId(anyLong()))
                .willReturn(accountDtoList);

        // when
        // then
        mockMvc.perform(get("/account?user_id=1"))
                .andDo(print())
                .andExpect(jsonPath("$[0].accountNumber").value("1111111111"))
                .andExpect(jsonPath("$[0].balance").value(1000L))
                .andExpect(jsonPath("$[1].accountNumber").value("2222222222"))
                .andExpect(jsonPath("$[1].balance").value(2000L))
                .andExpect(jsonPath("$[2].accountNumber").value("3333333333"))
                .andExpect(jsonPath("$[2].balance").value(3000L));
    }

    @Test
    @DisplayName("계좌번호로 계좌 조회 - 성공")
    void successGetAccount() throws Exception {
        // given
        given(accountService.getAccountByAccountId(anyLong()))
                .willReturn(AccountInfo.builder()
                        .accountNumber("1234567890")
                        .balance(10000L)
                        .build()
        );

        // when
        // then
        mockMvc.perform(get("/account/867"))
                .andDo(print())
                .andExpect(jsonPath("$.accountNumber")
                        .value("1234567890"))
                .andExpect(jsonPath("$.balance")
                        .value(10000L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("계좌번호로 계좌 조회 - 실패")
    void failedGetAccount() throws Exception {
        // given
        given(accountService.getAccountByAccountId(anyLong()))
                .willThrow(new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        // when
        // then
        mockMvc.perform(get("/account/867"))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode")
                        .value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.errorMessage")
                        .value("해당 계좌가 없습니다."))
                .andExpect(status().isOk());
    }
}