package io.security.basicsecurity;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	private final UserDetailsService userDetailsService;

	public SecurityConfig(UserDetailsService userDetailsService) {
		this.userDetailsService = userDetailsService;
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.inMemoryAuthentication().withUser("user").password("{noop}1111").roles("USER");
		auth.inMemoryAuthentication().withUser("sys").password("{noop}1111").roles("SYS");
		auth.inMemoryAuthentication().withUser("admin").password("{noop}1111").roles("ADMIN");
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		// 인증
		http
			.authorizeRequests()
			.antMatchers("/login").permitAll()
			.antMatchers("/user").hasRole("USER")
			.antMatchers("/admin/pay").hasRole("ADMIN")
			.antMatchers("/admin/**").access("hasRole('ADMIN') or hasRole('SYS')")
			.anyRequest()
			.authenticated();

		login(http);

		SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);

//		http
//			.exceptionHandling()
//			.authenticationEntryPoint((request, response, authException) -> response.sendRedirect("/login"))
//			.accessDeniedHandler((request, response, accessDeniedException) -> response.sendRedirect("/denied"))
//		;

//		logout(http);

//		http.rememberMe()
//			.rememberMeParameter("remember") // default:  remember-me
//			.tokenValiditySeconds(3600) //  default: 14일
//			.alwaysRemember(true) // remember-me 기능이 활성화되지 않아도 항상 실행
//			.userDetailsService(userDetailsService)
//			;

//		sessionManagement(http);
	}

	private void sessionManagement(HttpSecurity http) throws Exception {
		http.sessionManagement() // 세션 관리 기능이 작동한다.
			.maximumSessions(1) // 최대 허용 가능 세션 수, -1: 무제한 로그인 세션 허용
			.maxSessionsPreventsLogin(false) // 동시 로그인 차단함, false: 기존 세션 만료(default)
		;

//		http
//			.sessionManagement()
//			.sessionFixation()
//			.none() // 쿠키 값을 변경하지 않음 (공격에 취약함)
//			.migrateSession() // 3.1 미만 default
//			.changeSessionId() // 3.1 이상 default
		;
	}

	private void login(HttpSecurity http) throws Exception {
		// 인가
		http.formLogin()
			.successHandler((request, response, authentication) -> { // 로그인 후 기존 접속하려던 URL 로 Redirect
				HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
				SavedRequest savedRequest = requestCache.getRequest(request, response);
				String redirectUrl = savedRequest.getRedirectUrl();
				response.sendRedirect(redirectUrl);
			})
//			.loginPage("/loginPage") // 사용자 정의 로그인 페이지
//			.defaultSuccessUrl("/23") // 로그인 성공 후 이동 페이지
//			.failureUrl("/login2") // 로그인 실패 후 이동 페이지
//			.usernameParameter("userId") // 아이디 파라미터명 설정
//			.passwordParameter("passwd") // 패스워드 파라미터명 설정
//			.loginProcessingUrl("/login_proc") // 로그인 Form Action Url
//			// 상황에 따라서 핸들러나 default, fail url 중에서 하나만 설정해도 될듯
//			.successHandler(new AuthenticationSuccessHandler() { // 로그인 성공 후 핸들러
//				@Override
//				public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
//					System.out.println("authentication = " + authentication.getName());
//					response.sendRedirect("/");
//				}
//			})
//			.failureHandler(new AuthenticationFailureHandler() { // 로그인 실패 후 핸들러
//				@Override
//				public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
//					System.out.println("exception = " + exception.getMessage());
//					response.sendRedirect("/login");
//				}
//			})
//			.permitAll()
		;
	}

	private void logout(HttpSecurity http) throws Exception {
		http.logout()
			.logoutUrl("/logout") // 로그아웃 처리 URL
			.logoutSuccessUrl("/login") // 로그아웃 성공 후 이동페이지
			.addLogoutHandler(new LogoutHandler() { // 로그아웃 핸들러
				@Override
				public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
					HttpSession session = request.getSession();
					session.invalidate(); // 세션 무효화
				}
			})
			.logoutSuccessHandler(new LogoutSuccessHandler() { // 로그아웃 성공 후 핸들러
				@Override
				public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
					response.sendRedirect("/login");
				}
			})
			.deleteCookies("remember-me") // 로그아웃 후 쿠키 삭제
		;
	}

}
