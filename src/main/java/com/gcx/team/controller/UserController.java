package com.gcx.team.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcx.team.common.BaseResponse;
import com.gcx.team.common.ErrorCode;
import com.gcx.team.common.ResultUtils;
import com.gcx.team.exception.BusinessException;
import com.gcx.team.model.domain.User;
import com.gcx.team.model.request.UserLoginRequest;
import com.gcx.team.model.request.UserRegisterRequest;
import com.gcx.team.model.vo.UserVO;
import com.gcx.team.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.gcx.team.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户接口
 *
 */
@RestController
@RequestMapping("/user")
@CrossOrigin(origins = {"http://localhost:3000"})
@Slf4j
public class UserController {

    @Resource // 使用@Resource注解，自动注入UserService服务
    private UserService userService; // 定义一个UserService的私有变量

    @Resource // 使用@Resource注解，自动注入RedisTemplate
    private RedisTemplate<String, Object> redisTemplate; // 定义一个用于操作Redis的RedisTemplate变量

    @PostMapping("/register") // 处理/register路径的POST请求
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) { // 用户注册方法
        if (userRegisterRequest == null) { // 判断请求体是否为空
            throw new BusinessException(ErrorCode.PARAMS_ERROR); // 如果为空，抛出参数错误异常
        }
        String userAccount = userRegisterRequest.getUserAccount(); // 获取用户账号
        String userPassword = userRegisterRequest.getUserPassword(); // 获取用户密码
        String checkPassword = userRegisterRequest.getCheckPassword(); // 获取用户确认密码
        String planetCode = userRegisterRequest.getPlanetCode(); // 获取用户代码
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) { // 检查任何一个参数是否为空
            return null; // 如果任何一个参数为空，则返回null
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode); // 调用userService的注册方法
        return ResultUtils.success(result); // 返回注册结果
    }

    @PostMapping("/login") // 处理/login路径的POST请求
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) { // 用户登录方法
        if (userLoginRequest == null) { // 判断请求体是否为空
            return ResultUtils.error(ErrorCode.PARAMS_ERROR); // 如果为空，返回参数错误的错误信息
        }
        String userAccount = userLoginRequest.getUserAccount(); // 获取用户账号
        String userPassword = userLoginRequest.getUserPassword(); // 获取用户密码
        if (StringUtils.isAnyBlank(userAccount, userPassword)) { // 检查账号或密码是否为空
            return ResultUtils.error(ErrorCode.PARAMS_ERROR); // 如果为空，返回参数错误的错误信息
        }
        User user = userService.userLogin(userAccount, userPassword, request); // 调用userService的登录方法
        return ResultUtils.success(user); // 返回登录成功的用户信息
    }

    @PostMapping("/logout") // 处理/logout路径的POST请求
    public BaseResponse<Integer> userLogout(HttpServletRequest request) { // 用户登出方法
        if (request == null) { // 判断请求是否为空
            throw new BusinessException(ErrorCode.PARAMS_ERROR); // 如果为空，抛出参数错误异常
        }
        int result = userService.userLogout(request); // 调用userService的登出方法
        return ResultUtils.success(result); // 返回登出结果
    }

    @GetMapping("/current") // 处理/current路径的GET请求
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) { // 获取当前用户信息方法
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE); // 从session中获取用户登录状态
        User currentUser = (User) userObj; // 将获取的对象转换为User类型
        if (currentUser == null) { // 判断当前用户是否为空
            throw new BusinessException(ErrorCode.NOT_LOGIN); // 如果为空，抛出未登录异常
        }
        long userId = currentUser.getId(); // 获取当前用户的ID
        // TODO 校验用户是否合法
        User user = userService.getById(userId); // 通过ID获取用户信息
        User safetyUser = userService.getSafetyUser(user); // 获取去除敏感信息的用户信息
        return ResultUtils.success(safetyUser); // 返回处理过的用户信息
    }

    @GetMapping("/search") // 处理/search路径的GET请求
    public BaseResponse<List<User>> searchUsers(String username, HttpServletRequest request) { // 搜索用户方法
        if (!userService.isAdmin(request)) { // 判断当前用户是否为管理员
            throw new BusinessException(ErrorCode.PARAMS_ERROR); // 如果不是管理员，抛出参数错误异常
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>(); // 创建查询条件包装对象
        if (StringUtils.isNotBlank(username)) { // 如果用户名不为空
            queryWrapper.like("username", username); // 添加模糊查询条件
        }
        List<User> userList = userService.list(queryWrapper); // 根据条件查询用户列表
        List<User> list = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList()); // 将用户列表中的每个用户处理为去除敏感信息的形式
        return ResultUtils.success(list); // 返回处理过的用户列表
    }

    @GetMapping("/search/tags") // 处理/search/tags路径的GET请求
    public BaseResponse<List<User>> searchUsersByTags(@RequestParam(required = false) List<String> tagNameList) { // 通过标签搜索用户方法
        if (CollectionUtils.isEmpty(tagNameList)) { // 判断标签列表是否为空
            throw new BusinessException(ErrorCode.PARAMS_ERROR); // 如果为空，抛出参数错误异常
        }
        List<User> userList = userService.searchUsersByTags(tagNameList); // 根据标签列表搜索用户
        return ResultUtils.success(userList); // 返回搜索结果
    }

    // todo 推荐多个，未实现
    @GetMapping("/recommend") // 处理/recommend路径的GET请求
    public BaseResponse<Page<User>> recommendUsers(long pageSize, long pageNum, HttpServletRequest request) { // 推荐用户方法
        User loginUser = userService.getLoginUser(request); // 获取当前登录用户
        String redisKey = String.format("team:user:recommend:%s", loginUser.getId()); // 格式化生成Redis键
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue(); // 获取操作值的ValueOperations
        // 如果有缓存，直接读缓存
        Page<User> userPage = (Page<User>) valueOperations.get(redisKey); // 从Redis获取缓存的用户分页信息
        if (userPage != null) { // 如果缓存存在
            return ResultUtils.success(userPage); // 直接返回缓存的分页信息
        }
        // 无缓存，查数据库
        QueryWrapper<User> queryWrapper = new QueryWrapper<>(); // 创建查询条件包装对象
        userPage = userService.page(new Page<>(pageNum, pageSize), queryWrapper); // 分页查询用户信息
        // 写缓存
        try {
            valueOperations.set(redisKey, userPage, 30000, TimeUnit.MILLISECONDS); // 将查询结果写入Redis缓存
        } catch (Exception e) {
            log.error("redis set key error", e); // 如果写入缓存失败，记录错误日志
        }
        return ResultUtils.success(userPage); // 返回查询结果
    }

    @PostMapping("/update") // 处理/update路径的POST请求
    public BaseResponse<Integer> updateUser(@RequestBody User user, HttpServletRequest request) { // 更新用户信息方法
        // 校验参数是否为空
        if (user == null) { // 如果用户对象为空
            throw new BusinessException(ErrorCode.PARAMS_ERROR); // 抛出参数错误异常
        }
        User loginUser = userService.getLoginUser(request); // 获取当前登录用户
        int result = userService.updateUser(user, loginUser); // 更新用户信息
        return ResultUtils.success(result); // 返回更新结果
    }

    @PostMapping("/delete") // 处理/delete路径的POST请求
    public BaseResponse<Boolean> deleteUser(@RequestBody long id, HttpServletRequest request) { // 删除用户方法
        if (!userService.isAdmin(request)) { // 判断当前用户是否为管理员
            throw new BusinessException(ErrorCode.NO_AUTH); // 如果不是管理员，抛出无权限异常
        }
        if (id <= 0) { // 判断ID是否合法
            throw new BusinessException(ErrorCode.PARAMS_ERROR); // 如果ID不合法，抛出参数错误异常
        }
        boolean b = userService.removeById(id); // 根据ID删除用户
        return ResultUtils.success(b); // 返回删除结果
    }

    /**
     * 获取最匹配的用户
     *
     * @param num
     * @param request
     * @return
     */
    @GetMapping("/match") // 处理/match路径的GET请求
    public BaseResponse<List<User>> matchUsers(long num, HttpServletRequest request) { // 匹配用户方法
        if (num <= 0 || num > 20) { // 判断请求的用户数量是否在合法范围内
            throw new BusinessException(ErrorCode.PARAMS_ERROR); // 如果不在合法范围内，抛出参数错误异常
        }
        User user = userService.getLoginUser(request); // 获取当前登录用户
        return ResultUtils.success(userService.matchUsers(num, user)); // 返回匹配的用户列表
    }

}
