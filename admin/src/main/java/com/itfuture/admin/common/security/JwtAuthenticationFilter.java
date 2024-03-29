package com.itfuture.admin.common.security;

import com.itfuture.admin.common.constant.JwtConstant;
import com.itfuture.admin.entity.CheckResult;
import com.itfuture.admin.entity.SysUser;
import com.itfuture.admin.service.SysUserService;
import com.itfuture.admin.util.JwtUtils;
import com.itfuture.admin.util.StringUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * jwt自定义认证过滤器:jwt token认证；
 *
 * @author： wxh
 * @version：v1.0
 * @date： 2023/01/12 13:11
 */
public class JwtAuthenticationFilter extends BasicAuthenticationFilter {

    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private MyUserDetailServiceImpl myUserDetailService;

    private static final String URL_WHITELIST[] ={
            "/login",
            "/logout",
            "/captcha",
            "/password",
            "/image/**"
    } ;

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String token = request.getHeader("token");
        System.out.println("请求Url：" + request.getRequestURI());
        //如果token是空或者url在白名单里 则放行 让后面的springsecurity认证过滤器去认证
        if (StringUtil.isEmpty(token) ||
                new ArrayList<String>(Arrays.asList(URL_WHITELIST)).contains(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }
        CheckResult checkResult = JwtUtils.validateJWT(token);
        //jwt认证失败
        if (!checkResult.isSuccess()) {
            switch (checkResult.getErrCode()) {
                case JwtConstant
                        .JWT_ERRCODE_NULL:
                    throw new JwtException("Token不存在");
                case JwtConstant.JWT_ERRCODE_FAIL:
                    throw new JwtException("Token验证不通过");
                case JwtConstant.JWT_ERRCODE_EXPIRE:
                    throw new JwtException("Token过期");
            }
        }
        //jwt认证成功，则解析
        Claims claims = JwtUtils.parseJWT(token);
        String username = claims.getSubject();
        SysUser sysUser = sysUserService.getByUsername(username);
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(username, null, myUserDetailService.getUserAuthority(sysUser.getId()));
        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
        chain.doFilter(request,response);
    }
}
