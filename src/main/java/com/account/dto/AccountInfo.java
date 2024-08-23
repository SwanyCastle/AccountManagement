package com.account.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountInfo {
    // client 와 controller 간의 데이터를 주고받기위한 DTO
    // 이런식으로 DTO 를 왜 계속 만들어야 하는가 하나 만들어서 돌려 쓰면 되는거 아닌가 할 수 있지만
    // 전용으로 사용하는 DTO 를 만들지 않는다면 복잡한 상황이 생겼을 때 장애가 일어날 가능성이 높아진다.
    private String accountNumber;
    private Long balance;
}
