package com.gcx.team.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcx.team.service.UserTeamService;
import com.gcx.team.model.domain.UserTeam;
import com.gcx.team.mapper.UserTeamMapper;
import org.springframework.stereotype.Service;

/**
 * 用户队伍服务实现类
 *
 */
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
        implements UserTeamService {

}




