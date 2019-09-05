package com.karat.cn.blog_backstage.controller;

import com.karat.cn.blog_backstage.bean.Author;
import com.karat.cn.blog_backstage.bean.Friend;
import com.karat.cn.blog_backstage.bean.User;
import com.karat.cn.blog_backstage.dao.*;
import com.karat.cn.blog_backstage.service.PermissionService;
import com.karat.cn.blog_backstage.service.RolePermissionService;
import com.karat.cn.blog_backstage.service.ShiroRoleService;
import com.karat.cn.blog_backstage.service.ShiroUserService;
import com.karat.cn.blog_backstage.util.PageUtil;
import com.karat.cn.blog_backstage.util.RedisKey;
import com.karat.cn.blog_backstage.vo.shiro.RoleVo;
import com.karat.cn.blog_backstage.vo.shiro.ShiroResponseVo;
import com.karat.cn.blog_backstage.vo.shiro.ShiroUserVo;
import com.karat.cn.blog_backstage.vo.view.Response;
import com.karat.cn.blog_backstage.vo.view.ResponseNumVo;
import com.karat.cn.blog_backstage.vo.view.ResponseTagVo;
import com.karat.cn.blog_backstage.vo.view.ResponseUserVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 *  属于user角色@RequiresRoles("user")
 * 	必须同时属于user和admin角@RequiresRoles({ "user", "admin" })
 * 	属于user或者admin之一;修改logical为OR 即可@RequiresRoles(value = { "user", "admin"},
 * 	logical = Logical.OR)
 */
@Controller
@RequestMapping("/view")
@Api("后台接口")
public class ViewController {


    private static final Logger log = LoggerFactory.getLogger(ViewController.class);

    @Autowired
    BlogDao blogDao;
    @Autowired
    TagDao tagDao;
    @Autowired
    CommentDao commentDao;
    @Autowired
    UserDao userDao;
    @Autowired
    FriendDao friendDao;
    @Autowired
    AuthorDao authorDao;

    /*=============================================*/
    @Autowired
    ShiroUserService shiroUserService;
    @Autowired
    ShiroRoleService shiroRoleService;
    @Autowired
    PermissionService permissionService;
    @Autowired
    RolePermissionService rolePermissionService;

    /*=====================================后台登陆============================================*/

    @GetMapping("/ok")
    @ApiOperation("调转登录")
    public String ok()  {
        return "login";
    }

    @PostMapping("/login")
    @ApiOperation("后台登陆")
    public String login(String username, String password, Map<String, Object> map) {
        //将用户名与密码存入令牌中
        UsernamePasswordToken token = new UsernamePasswordToken(username, password);
        String msg = "";
        try {
            Subject subject = SecurityUtils.getSubject();
            //将用户名密码生成的token令牌传入login方法中
            subject.login(token);
            return "redirect:/html/toIndex";
        } catch (IncorrectCredentialsException e) {
            msg = "登录密码错误";
            log.warn("登录密码错误!!!" + e);
        } catch (ExcessiveAttemptsException e) {
            msg = "登录失败次数过多";
            log.warn("登录失败次数过多!!!" + e);
        } catch (LockedAccountException e) {
            msg = "帐号已被锁定";
            log.warn("帐号已被锁定!!!" + e);
        } catch (DisabledAccountException e) {
            msg = "帐号已被禁用";
            log.warn("帐号已被禁用!!!" + e);
        } catch (ExpiredCredentialsException e) {
            msg = "帐号已过期";
            log.warn("帐号已过期!!!" + e);
        } catch (UnknownAccountException e) {
            msg = "帐号不存在";
            log.warn("帐号不存在!!!" + e);
        } catch (UnauthorizedException e) {
            msg = "您没有得到相应的授权！";
            log.warn("您没有得到相应的授权！" + e);
        } catch (Exception e) {
            msg = e.getMessage();
            log.warn("出错！！！" + e);
        }
        map.put("msg", msg);//返回错误信息
        return "login";
    }

    @RequestMapping(value="logout",method = {RequestMethod.POST, RequestMethod.GET})
    @ApiOperation("退出")
    public String logout(){
        //subject的实现类DelegatingSubject的logout方法，将本subject对象的session清空了
        //即使session托管给了redis ，redis有很多个浏览器的session
        //只要调用退出方法，此subject的、此浏览器的session就没了
        try {
            //退出
            SecurityUtils.getSubject().logout();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return "login";

    }
    /*============================================博客小程序数据统计======================================*/

    @PostMapping("/insertUser")
    @ResponseBody
    @ApiOperation("添加用户")
    public ResponseUserVo insertUser(User user)  {
        log.warn(user.toString());
        userDao.addUser(user);
        List<User> users=userDao.selectAll();
        return new ResponseUserVo(users.size(),1,PageUtil.getPageByList(users,1,2));
    }
    @PostMapping("/delUser")
    @ResponseBody
    @ApiOperation("删除用户")
    public ResponseUserVo delUser(String openId)  {
        userDao.delUser(openId);
        List<User> users=userDao.selectAll();
        return new ResponseUserVo(users.size(),1,PageUtil.getPageByList(users,1,2));
    }
    @PostMapping("/editUser")
    @ResponseBody
    @ApiOperation("修改用户")
    public Response editUser(String openId, String url, String name)  {
        User user=userDao.selectById(openId);
        if(user!=null){
            user.setName(name);
            user.setUrl(url);
            userDao.updateUser(user);
            return new Response(200,"ok");
        }else{
            return new Response(201,"error");
        }
    }
    @PostMapping("/getUserByPage")
    @ResponseBody
    @ApiOperation("查看用户")
    public ResponseUserVo getUserByPage(int limit,int curr)  {
        log.warn("每页大小："+limit+"当前页:"+curr);
        List<User> users=userDao.selectAll();
        System.out.println(users.size());
        return new ResponseUserVo(users.size(),curr,PageUtil.getPageByList(users,curr,limit));
    }

    @PostMapping("getFrends")
    @ResponseBody
    @ApiOperation("查看友链")
    public List<Friend> getFrends(){
        return friendDao.selectAll();
    }
    @PostMapping("selectAuthor")
    @ResponseBody
    @ApiOperation("查看联系我")
    public Author selectAuthor(){
        return authorDao.select();
    }
    @PostMapping("selectTag")
    @ResponseBody
    @ApiOperation("查看标签")
    public List<ResponseTagVo> selectTag(){
        List<ResponseTagVo> vo=new ArrayList<>();
        vo.add(new ResponseTagVo(RedisKey.JAVA));
        vo.add(new ResponseTagVo(RedisKey.HOT));
        vo.add(new ResponseTagVo(RedisKey.PYTHON));
        vo.add(new ResponseTagVo(RedisKey.OTHER));
        vo.add(new ResponseTagVo(RedisKey.WEB));
        return vo;
    }
    @PostMapping("getNum")
    @ResponseBody
    @ApiOperation("查看数据统计")
    public ResponseNumVo getNum(){
        return new ResponseNumVo(blogDao.selectAll().size(),userDao.selectAll().size(),199, 5,friendDao.selectAll().size());
    }

    /*=======================================权限相关===========================================*/

    @PostMapping("selectShiroUser")
    @ResponseBody
    @RequiresPermissions("user:select")//权限管理;
    @ApiOperation("查看用户")
    public ShiroResponseVo selectShiroUser(){
        ShiroResponseVo vo=new ShiroResponseVo(200,"ok");

        List<ShiroUserVo> shiroUserVos=new ArrayList<>();
        shiroUserService.getAllShiroUser().forEach(i->{
            ShiroUserVo shiroUserVo=new ShiroUserVo();
            shiroUserVo.setId(i.getId());
            shiroUserVo.setLocked(i.getLocked());
            shiroUserVo.setPassword(i.getPassword());
            shiroUserVo.setUsername(i.getUsername());
            //查看角色
            shiroUserService.findRoles(i.getUsername()).forEach(j->{
                if(shiroUserVo.getRole()==null){
                    shiroUserVo.setRole("【"+j+"】");
                }else{
                    shiroUserVo.setRole(shiroUserVo.getRole()+"【"+j+"】");
                }
            });
            //查看权限
            shiroUserService.findPermissions(i.getUsername()).forEach(j->{
                if(shiroUserVo.getPermission()==null){
                    shiroUserVo.setPermission("【"+j+"】");
                }else {
                    shiroUserVo.setPermission(shiroUserVo.getPermission() + "【" + j + "】");
                }
            });

            shiroUserVos.add(shiroUserVo);
        });
        vo.setShiroUsers(shiroUserVos);
        return vo;
    }

    @PostMapping("selectShiroRole")
    @ResponseBody
    @ApiOperation("查看角色")
    public ShiroResponseVo selectShiroRole(){
        List<RoleVo> shiroRoles=new ArrayList<>();
        shiroRoleService.getShiroRoles().forEach(i->{
            RoleVo vo=new RoleVo();
            vo.setRoleid(i.getRoleid());
            vo.setRole(i.getRole());
            vo.setDescription(i.getDescription());

            rolePermissionService.select(i.getRoleid()).forEach(j->{
                if(vo.getRoledes()==null){
                    vo.setRoledes("【"+permissionService.getPermissionByid(j.getPermissionId()).getPermission()+"】");
                }else{
                    vo.setRoledes(vo.getRoledes()+"【"+permissionService.getPermissionByid(j.getPermissionId()).getPermission()+"】");
                }
            });
            shiroRoles.add(vo);
        });
        shiroRoles.forEach(i->{
            if(i==null){
                shiroRoles.remove(i);
            }
        });
        ShiroResponseVo vo=new ShiroResponseVo(200,"ok");
        vo.setShiroRoles(shiroRoles);
        return vo;
    }

    @PostMapping("selectPermission")
    @ResponseBody
    @RequiresRoles("user")
    @ApiOperation("查看权限")
    public ShiroResponseVo selectPermission(){
        ShiroResponseVo vo=new ShiroResponseVo(200,"ok");
        vo.setPermissions(permissionService.getPermissions());
        return vo;
    }





}