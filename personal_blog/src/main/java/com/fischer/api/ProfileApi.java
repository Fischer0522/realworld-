package com.fischer.api;

import com.fischer.api.exception.BizException;
import com.fischer.dao.AdminDao;
import com.fischer.data.ProfileData;
import com.fischer.jwt.JwtService;
import com.fischer.pojo.Admin;
import com.fischer.pojo.User;
import com.fischer.repository.UserRepository;
import com.fischer.service.ProfileQueryService;
import com.fischer.service.user.UserQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.config.RepositoryNameSpaceHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Optional;

@RestController
@RequestMapping(path = "profiles/{username}")
public class ProfileApi {

    private AdminDao adminDao;
    private ProfileQueryService profileQueryService;
    private UserRepository userRepository;
    private JwtService jwtService;
    @Autowired
    public ProfileApi(ProfileQueryService profileQueryService,
                        AdminDao adminDao,
                      UserRepository userRepository,
                      JwtService jwtService){
        this.profileQueryService=profileQueryService;
        this.adminDao=adminDao;
        this.userRepository=userRepository;
        this.jwtService=jwtService;
    }
    @GetMapping
    public ResponseEntity getProfile(@PathVariable("username") String username){
        return profileQueryService.finByUsername(username)
                .map(this::profileResponse)
                .orElseThrow(()->new BizException(HttpStatus.NOT_FOUND,"该用户已不存在"));
    }

    @PostMapping("admin")
    public ResponseEntity addAdimin(@PathVariable("username") String username,
                                    @RequestHeader(value ="Authorization") String token){
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException(HttpStatus.NOT_FOUND, "该用户不存在"));
        User admin = jwtService.toUser(token)
                .orElseThrow(() -> new BizException(HttpStatus.UNAUTHORIZED, "token解析失败"));

        if(!admin.getId().equals("1234")){
            throw new BizException(HttpStatus.FORBIDDEN,"只有超级管理员才能设置管理员");
        }
        adminDao.insert(new Admin(user.getId(),user.getUsername()));

        return ResponseEntity.ok(new Admin(user.getId(),user.getUsername()));

    }
    @DeleteMapping("admin")
    public ResponseEntity removeAdmin(@PathVariable("username") String username,
                                      @RequestHeader(value = "Authorization") String token){
        User admin= jwtService.toUser(token).orElseThrow(() -> new BizException(HttpStatus.NOT_FOUND, "token解析失败"));
        User user = userRepository.findByUsername(username).orElseThrow(() -> new BizException(HttpStatus.NOT_FOUND, "该用户以不存在"));
        if(!admin.getId().equals("1234")){
            throw new BizException(HttpStatus.FORBIDDEN,"只有超级管理员才能设置管理员");
        }
        Admin byId = adminDao.selectById(user.getId());
        if(byId==null){
            throw new BizException(HttpStatus.NOT_FOUND,"当前用户不是管理员");
        }
        adminDao.deleteById(user.getId());
        return  ResponseEntity.status(HttpStatus.NO_CONTENT).build();

    }

    /*后续补充关注和取关的功能*/
    private ResponseEntity profileResponse(ProfileData profile) {
        return ResponseEntity.ok(new HashMap<String, Object>() {{
            put("profile", profile);
        }});
    }


}
