package com.gcx.team.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcx.team.common.BaseResponse;
import com.gcx.team.common.DeleteRequest;
import com.gcx.team.common.ErrorCode;
import com.gcx.team.common.ResultUtils;
import com.gcx.team.exception.BusinessException;
import com.gcx.team.model.domain.Team;
import com.gcx.team.model.domain.User;
import com.gcx.team.model.domain.UserTeam;
import com.gcx.team.model.dto.TeamQuery;
import com.gcx.team.model.request.TeamAddRequest;
import com.gcx.team.model.request.TeamJoinRequest;
import com.gcx.team.model.request.TeamQuitRequest;
import com.gcx.team.model.request.TeamUpdateRequest;
import com.gcx.team.model.vo.TeamUserVO;
import com.gcx.team.service.TeamService;
import com.gcx.team.service.UserService;
import com.gcx.team.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 队伍接口
 *
 */
@RestController             // 声明这是一个控制器，用于处理HTTP请求
@RequestMapping("/team") // 指定该控制器处理的基本URL路径
@CrossOrigin(origins = {"http://localhost:3000"}) // 允许来自指定源的跨域请求
@Slf4j                      // Lombok注解，为类提供一个日志属性
public class TeamController {

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @Resource
    private UserTeamService userTeamService;

    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        if (teamAddRequest == null) { // 检查请求体是否为空
            throw new BusinessException(ErrorCode.PARAMS_ERROR); // 抛出参数异常
        }
        User loginUser = userService.getLoginUser(request); // 获取当前登录的用户
        Team team = new Team(); // 创建一个新的Team对象
        BeanUtils.copyProperties(teamAddRequest, team); // 将请求中的属性复制到新的Team对象中
        long teamId = teamService.addTeam(team, loginUser); // 添加队伍，并返回队伍ID
        return ResultUtils.success(teamId); // 返回添加成功的队伍ID
    }

    @PostMapping("/update") // 处理对应URL为/team/update的POST请求
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
        if (teamUpdateRequest == null) { // 检查请求体是否为空
            throw new BusinessException(ErrorCode.PARAMS_ERROR); // 抛出参数异常
        }
        User loginUser = userService.getLoginUser(request); // 获取当前登录的用户
        boolean result = teamService.updateTeam(teamUpdateRequest, loginUser); // 更新队伍信息
        if (!result) { // 如果更新失败
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败"); // 抛出系统异常
        }
        return ResultUtils.success(true); // 返回更新成功的状态
    }

    @GetMapping("/get") // 处理对应URL为/team/get的GET请求
    public BaseResponse<Team> getTeamById(long id) {
        if (id <= 0) { // 检查ID是否有效
            throw new BusinessException(ErrorCode.PARAMS_ERROR); // 抛出参数异常
        }
        Team team = teamService.getById(id); // 根据ID获取队伍信息
        if (team == null) { // 如果没有找到队伍
            throw new BusinessException(ErrorCode.NULL_ERROR); // 抛出空指针异常
        }
        return ResultUtils.success(team); // 返回找到的队伍信息
    }

    @GetMapping("/list") // 处理对应URL为/team/list的GET请求
    public BaseResponse<List<TeamUserVO>> listTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) { // 检查查询条件是否为空
            throw new BusinessException(ErrorCode.PARAMS_ERROR); // 抛出参数异常
        }
        boolean isAdmin = userService.isAdmin(request); // 判断当前用户是否为管理员
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, isAdmin); // 获取队伍列表
        final List<Long> teamIdList = teamList.stream().map(TeamUserVO::getId).collect(Collectors.toList()); // 提取队伍ID列表
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>(); // 创建查询条件包装器
        try {
            User loginUser = userService.getLoginUser(request); // 获取当前登录的用户
            userTeamQueryWrapper.eq("userId", loginUser.getId()); // 设置查询条件：用户ID
            userTeamQueryWrapper.in("teamId", teamIdList); // 设置查询条件：队伍ID列表
            List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper); // 获取用户加入的队伍列表
            Set<Long> hasJoinTeamIdSet = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toSet()); // 提取用户已加入的队伍ID集合
            teamList.forEach(team -> {
                boolean hasJoin = hasJoinTeamIdSet.contains(team.getId()); // 判断用户是否已加入该队伍
                team.setHasJoin(hasJoin); // 设置加入状态
            });
        } catch (Exception e) {
            // 异常处理略过（在实际应用中应进行异常处理）
        }
        QueryWrapper<UserTeam> userTeamJoinQueryWrapper = new QueryWrapper<>(); // 创建另一个查询条件包装器
        userTeamJoinQueryWrapper.in("teamId", teamIdList); // 设置查询条件：队伍ID列表
        List<UserTeam> userTeamList = userTeamService.list(userTeamJoinQueryWrapper); // 获取队伍成员列表
        Map<Long, List<UserTeam>> teamIdUserTeamList = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId)); // 按队伍ID分组，收集成员信息
        teamList.forEach(team -> team.setHasJoinNum(teamIdUserTeamList.getOrDefault(team.getId(), new ArrayList<>()).size())); // 设置每个队伍的成员数量
        return ResultUtils.success(teamList); // 返回队伍列表和相关信息
    }

    // todo 查询分页
    @GetMapping("/list/page") // 处理对应URL为/team/list/page的GET请求，实现分页查询
    public BaseResponse<Page<Team>> listTeamsByPage(TeamQuery teamQuery) {
        if (teamQuery == null) { // 检查查询条件是否为空
            throw new BusinessException(ErrorCode.PARAMS_ERROR); // 抛出参数异常
        }
        Team team = new Team(); // 创建Team对象
        BeanUtils.copyProperties(teamQuery, team); // 将查询条件复制到Team对象中
        Page<Team> page = new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize()); // 创建分页对象
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team); // 创建查询条件包装器
        Page<Team> resultPage = teamService.page(page, queryWrapper); // 进行分页查询
        return ResultUtils.success(resultPage); // 返回查询结果
    }

    @PostMapping("/join") // 处理对应URL为/team/join的POST请求，用于加入队伍
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        if (teamJoinRequest == null) { // 检查请求体是否为空
            throw new BusinessException(ErrorCode.PARAMS_ERROR); // 抛出参数异常
        }
        User loginUser = userService.getLoginUser(request); // 获取当前登录的用户
        boolean result = teamService.joinTeam(teamJoinRequest, loginUser); // 加入队伍
        return ResultUtils.success(result); // 返回加入结果
    }

    @PostMapping("/quit") // 处理对应URL为/team/quit的POST请求，用于退出队伍
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        if (teamQuitRequest == null) { // 检查请求体是否为空
            throw new BusinessException(ErrorCode.PARAMS_ERROR); // 抛出参数异常
        }
        User loginUser = userService.getLoginUser(request); // 获取当前登录的用户
        boolean result = teamService.quitTeam(teamQuitRequest, loginUser); // 退出队伍
        return ResultUtils.success(result); // 返回退出结果
    }

    @PostMapping("/delete") // 处理对应URL为/team/delete的POST请求，用于删除队伍
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) { // 检查请求体是否为空，以及ID是否有效
            throw new BusinessException(ErrorCode.PARAMS_ERROR); // 抛出参数异常
        }
        long id = deleteRequest.getId(); // 获取要删除的队伍ID
        User loginUser = userService.getLoginUser(request); // 获取当前登录的用户
        boolean result = teamService.deleteTeam(id, loginUser); // 删除队伍
        if (!result) { // 如果删除失败
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败"); // 抛出系统异常
        }
        return ResultUtils.success(true); // 返回删除成功的状态
    }


    /**
     * 获取我创建的队伍
     *
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("/list/my/create") // 处理对应URL为/team/list/my/create的GET请求，用于获取我创建的队伍列表
    public BaseResponse<List<TeamUserVO>> listMyCreateTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) { // 检查查询条件是否为空
            throw new BusinessException(ErrorCode.PARAMS_ERROR); // 抛出参数异常
        }
        User loginUser = userService.getLoginUser(request); // 获取当前登录的用户
        teamQuery.setUserId(loginUser.getId()); // 设置查询条件为当前用户ID
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, true); // 获取我创建的队伍列表
        return ResultUtils.success(teamList); // 返回队伍列表
    }


    /**
     * 获取我加入的队伍
     *
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("/list/my/join") // 处理对应URL为/team/list/my/join的GET请求，用于获取我加入的队伍列表
    public BaseResponse<List<TeamUserVO>> listMyJoinTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) { // 检查查询条件是否为空
            throw new BusinessException(ErrorCode.PARAMS_ERROR); // 抛出参数异常
        }
        User loginUser = userService.getLoginUser(request); // 获取当前登录的用户
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>(); // 创建查询条件包装器
        queryWrapper.eq("userId", loginUser.getId()); // 设置查询条件为当前用户ID
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper); // 获取用户加入的队伍列表
        // 取出不重复的队伍 id
        // teamId userId
        // 1, 2
        // 1, 3
        // 2, 3
        // result
        // 1 => 2, 3
        // 2 => 3
        Map<Long, List<UserTeam>> listMap = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId)); // 按队伍ID分组，收集成员信息
        List<Long> idList = new ArrayList<>(listMap.keySet()); // 提取队伍ID列表
        teamQuery.setIdList(idList); // 设置查询条件为队伍ID列表
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, true); // 获取我加入的队伍列表
        return ResultUtils.success(teamList); // 返回队伍列表
    }
}



























