package com.summar.summar.domain;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.summar.summar.dto.ChangeUserInfoRequestDto;
import com.summar.summar.dto.LoginRequestDto;
import com.summar.summar.dto.UserSaveDto;
import com.summar.summar.enumeration.SocialType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "USER")
public class User extends BaseTimeEntity implements Serializable {

    /**
     * This VO is for security.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userSeq;
    private String userNickname; //닉네임
    private String userEmail; //이메일

    private LocalDate lastLoginDate; //최종로그인일시
    @Enumerated(EnumType.STRING)
    private SocialType socialType; //소셜로그인 타입
    @Enumerated(EnumType.STRING)
    private UserStatus userStatus = UserStatus.normal; //유저 상태
    private String major1; //계열
    private String major2; //전공

    private String introduce; //자기소개

    private Integer follower; //팔로워

    private Integer following; //팔로윙

    private String profileImageUrl; //프로필이미지경로

    private UUID deviceToken; //디바이스 식별 토큰값 (랜덤)

    @Type(type="yes_no")
    private Boolean pushAlarmYn;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    @JsonManagedReference
    private RefreshToken refreshToken;

    @Builder
    public User(Long userSeq, String userEmail, String userNickname) {
        this.userSeq = userSeq;
        this.userEmail = userEmail;
        this.userNickname = userNickname;
    }

    public User(LoginRequestDto loginRequestDto) {
        this.userNickname = loginRequestDto.getUserNickname();
        this.userEmail = loginRequestDto.getUserEmail();
    }

    public User(UserSaveDto userSaveDto){
        this.userEmail = userSaveDto.getUserEmail();
        this.userNickname = userSaveDto.getUserNickname();
        this.major1 = userSaveDto.getMajor1();
        this.major2 = userSaveDto.getMajor2();
        this.follower = userSaveDto.getFollower();
        this.following = userSaveDto.getFollowing();
        this.deviceToken = userSaveDto.getDeviceToken();
        this.socialType = userSaveDto.getSocialType();
        this.lastLoginDate = userSaveDto.getLastLoginDate();
        this.pushAlarmYn = userSaveDto.getPushAlarmYn();
    }
    public void setLastLoginDateAndDeviceToken(LocalDate lastLoginDate,UUID deviceToken){
        this.lastLoginDate = lastLoginDate;
        this.deviceToken = deviceToken;
    }
    public void setUserStatus(UserStatus userStatus){
        this.userStatus = userStatus;
    }

    public void setIntroduce(String introduce){
        this.introduce = introduce;
    }

    public void changeUserInfo(ChangeUserInfoRequestDto changeUserInfoRequestDto) {
        this.userNickname = changeUserInfoRequestDto.getUserNickname();
        this.major1 = changeUserInfoRequestDto.getMajor1();
        this.major2 = changeUserInfoRequestDto.getMajor2();
        this.profileImageUrl = changeUserInfoRequestDto.getProfileImageUrl();
    }
}
