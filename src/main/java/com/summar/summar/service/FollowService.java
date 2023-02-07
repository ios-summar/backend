package com.summar.summar.service;

import com.summar.summar.common.SummarCommonException;
import com.summar.summar.common.SummarErrorCode;
import com.summar.summar.domain.Follow;
import com.summar.summar.domain.User;
import com.summar.summar.dto.*;
import com.summar.summar.repository.FollowRepository;
import com.summar.summar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.webjars.NotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowService {
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final PushService pushService;


    @Transactional
    public Page<FollowerResponseDto> findFollowers(Long userSeq, Pageable pageable) {
        //해당 유저의 팔로워들 정보 추출
        //나를 팔로우하는사람들 리스트
        //userSeq = 1
        User userInfo = userRepository.findByUserSeqAndLeaveYn(userSeq,false).orElseThrow(() -> new SummarCommonException(SummarErrorCode.USER_NOT_FOUND.getCode(), SummarErrorCode.USER_NOT_FOUND.getMessage()));
        Page<Follow> followingList = followRepository.findByFollowedUserAndFollowYn(userInfo,true, pageable);
        for (Follow follow1:followingList) {
            //followed가 1이면서
            List<Follow> followList = followRepository.findByFollowedUserAndFollowYn(follow1.getFollowingUser(),true);
            for (Follow follow2:followList) {
                if(follow2.getFollowedUser().equals(follow1.getFollowingUser())){
                    log.info("맞팔인 사람 : {}",follow2.getFollowId());
                    Follow followInfo1 = followRepository.findById(follow2.getFollowId()).orElseThrow(() -> new SummarCommonException(SummarErrorCode.USER_NOT_FOUND.getCode(), SummarErrorCode.USER_NOT_FOUND.getMessage()));
                    Follow followInfo2 = followRepository.findById(follow1.getFollowId()).orElseThrow(() -> new SummarCommonException(SummarErrorCode.USER_NOT_FOUND.getCode(), SummarErrorCode.USER_NOT_FOUND.getMessage()));

                    log.info("userInfo : {}",followInfo1.getFollowId()+" , "+followInfo2.getFollowId());
                    followInfo1.setFollowUp(true);
                    followInfo2.setFollowUp(true);

                    followRepository.save(followInfo1);
                    followRepository.save(followInfo2);
                }
            }
        }
        return followingList.map(m -> FollowerResponseDto.builder()
                .userNickname(m.getFollowingUser().getUserNickname())
                .follower(m.getFollowingUser().getFollower())
                .following(m.getFollowingUser().getFollowing())
                .major1(m.getFollowingUser().getMajor1())
                .major2(m.getFollowingUser().getMajor2())
                .profileImageUrl(m.getFollowingUser().getProfileImageUrl())
                .userSeq(m.getFollowingUser().getUserSeq())
                .followUp(m.getFollowUp())
                .build());
    }

   @Transactional(readOnly = true)
    public Page<?> findFollowings(Long userSeq, Pageable pageable) {
        //내가 팔로우 하는 사람들 리스트
        User userInfo = userRepository.findByUserSeqAndLeaveYn(userSeq,false).orElseThrow(() -> new SummarCommonException(SummarErrorCode.USER_NOT_FOUND.getCode(), SummarErrorCode.USER_NOT_FOUND.getMessage()));
        Page<Follow> followingList = followRepository.findByFollowingUserAndFollowYn(userInfo, true, pageable);
        return followingList.map(m -> FollowerResponseDto.builder()
                .userNickname(m.getFollowedUser().getUserNickname())
                .follower(m.getFollowedUser().getFollower())
                .following(m.getFollowedUser().getFollowing())
                .major1(m.getFollowedUser().getMajor1())
                .major2(m.getFollowedUser().getMajor2())
                .profileImageUrl(m.getFollowedUser().getProfileImageUrl())
                .userSeq(m.getFollowedUser().getUserSeq())
                .build());
    }
    @Transactional
    public void addFollower(FollowerRequestDto followerRequestDto) {
        if (followerRequestDto.getFollowedUserSeq().equals(followerRequestDto.getFollowingUserSeq())) {
            throw new NotFoundException("같을수없다");
        }
        User followedUser = userRepository.findByUserSeqAndLeaveYn(followerRequestDto.getFollowedUserSeq(),false).orElseThrow(() -> new SummarCommonException(SummarErrorCode.USER_NOT_FOUND.getCode(), SummarErrorCode.USER_NOT_FOUND.getMessage()));
        User followingUser = userRepository.findByUserSeqAndLeaveYn(followerRequestDto.getFollowingUserSeq(),false).orElseThrow(() -> new SummarCommonException(SummarErrorCode.USER_NOT_FOUND.getCode(), SummarErrorCode.USER_NOT_FOUND.getMessage()));
        Follow followInfo = followRepository.findByFollowedUserAndFollowingUserAndFollowYn(followedUser, followingUser, false).orElse(null);
        //푸쉬 알람
        PushNotificationDto pushNotificationDto = PushNotificationDto.builder()
                .title("Summar")
                .body(followingUser.getUserNickname() + "님이 회원님을 팔로우했어요.")
                .userNickname(followedUser.getUserNickname())
                .build();
        //팔로우 정보가 없다면
        if (followInfo == null) {
            FollowerSaveDto followerSaveDto = FollowerSaveDto.builder()
                    .followedUser(followedUser)
                    .followYn(true)
                    .followingUser(followingUser)
                    .build();
            followRepository.save(new Follow(followerSaveDto));
            Integer followerCount = followRepository.countByFollowedUserAndFollowYn(followedUser, true);
            Integer followingCount = followRepository.countByFollowingUserAndFollowYn(followingUser, true);
            followedUser.updateFollower(followerCount);
            userRepository.save(followedUser);
            followingUser.updateFollowing(followingCount);
            userRepository.save(followingUser);

        } else {
            //기존 팔로우 정보가 있다면
            followInfo.setFollowYn(true);
            followRepository.save(followInfo);
            Integer followerCount = followRepository.countByFollowedUserAndFollowYn(followedUser, true);
            Integer followingCount = followRepository.countByFollowingUserAndFollowYn(followingUser, true);
            followedUser.updateFollower(followerCount);
            userRepository.save(followedUser);
            followingUser.updateFollowing(followingCount);
            userRepository.save(followingUser);
        }
        //푸시알림 발송
        pushService.pushNotification(pushNotificationDto);
    }

    @Transactional
    public void deleteFollower(FollowerRequestDto followerRequestDto) {
        User followingUser = userRepository.findByUserSeqAndLeaveYn(followerRequestDto.getFollowingUserSeq(),false).orElseThrow(() -> new SummarCommonException(SummarErrorCode.USER_NOT_FOUND.getCode(), SummarErrorCode.USER_NOT_FOUND.getMessage()));
        User followedUser = userRepository.findByUserSeqAndLeaveYn(followerRequestDto.getFollowedUserSeq(),false).orElseThrow(() -> new SummarCommonException(SummarErrorCode.USER_NOT_FOUND.getCode(), SummarErrorCode.USER_NOT_FOUND.getMessage()));
        Follow followInfo = followRepository.findByFollowedUserAndFollowingUserAndFollowYn(followedUser,followingUser , true).orElseThrow(() -> new SummarCommonException(SummarErrorCode.FOLLOW_NOT_FOUND.getCode(), SummarErrorCode.FOLLOW_NOT_FOUND.getMessage()));
        followInfo.setFollowYn(false);
        followRepository.save(followInfo);
        Integer followerCount = followRepository.countByFollowedUserAndFollowYn(followedUser, true);
        Integer followingCount = followRepository.countByFollowingUserAndFollowYn(followingUser, true);
        followedUser.updateFollower(followerCount);
        followingUser.updateFollowing(followingCount);
        userRepository.save(followingUser);
        userRepository.save(followedUser);
    }

    @Transactional(readOnly = true)
    //followedUserSeq = 팔로우 당한사람
    //followingUserSeq = 팔로우 한사람
    public Boolean followCheck(Long followedUserSeq, Long followingUserSeq) {
        User followedUser = userRepository.findByUserSeqAndLeaveYn(followedUserSeq,false)
                .orElseThrow(() -> new SummarCommonException(SummarErrorCode.USER_NOT_FOUND.getCode(), SummarErrorCode.USER_NOT_FOUND.getMessage()));
        User followingUser = userRepository.findByUserSeqAndLeaveYn(followingUserSeq,false)
                .orElseThrow(() -> new SummarCommonException(SummarErrorCode.USER_NOT_FOUND.getCode(), SummarErrorCode.USER_NOT_FOUND.getMessage()));
        return followRepository.existsByFollowedUserAndFollowingUserAndFollowYn(followedUser,followingUser,true);
    }
}
