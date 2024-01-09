package de.hasait.sddups.security;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.auth.ViewAccessChecker;
import com.vaadin.flow.spring.security.UidlRedirectStrategy;
import com.vaadin.flow.spring.security.VaadinSavedRequestAwareAuthenticationSuccessHandler;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import de.hasait.sddups.ui.LoginView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;

@EnableWebSecurity
@Configuration
public class SecurityConfiguration extends VaadinWebSecurity {

    /**
     * FIXME Remove as soon as csrf AntPathRequestMatcher is implemented upstream
     */
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * FIXME Remove as soon as csrf AntPathRequestMatcher is implemented upstream
     */
    @Autowired
    private ViewAccessChecker viewAccessChecker;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Delegating the responsibility of general configurations
        // of http security to the super class. It's configuring
        // the followings: Vaadin's CSRF protection by ignoring
        // framework's internal requests, default request cache,
        // ignoring public views annotated with @AnonymousAllowed,
        // restricting access to other views/endpoints, and enabling
        // ViewAccessChecker authorization.
        // You can add any possible extra configurations of your own
        // here (the following is just an example):

        // http.rememberMe().alwaysRemember(false);

        // Configure your static resources with public access before calling
        // super.configure(HttpSecurity) as it adds final anyRequest matcher
        http.authorizeHttpRequests().requestMatchers(new AntPathRequestMatcher("/public/**"))
                .permitAll();

        super.configure(http);

        // This is important to register your login view to the
        // view access checker mechanism:
        setLoginView(http, LoginView.class);
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        // Customize your WebSecurity configuration.
        super.configure(web);
    }

    /**
     * FIXME Remove as soon as csrf AntPathRequestMatcher is implemented upstream
     */
    protected void setLoginView(HttpSecurity http,
                                Class<? extends Component> flowLoginView, String logoutSuccessUrl)
            throws Exception {
        Optional<Route> route = AnnotationReader.getAnnotationFor(flowLoginView,
                Route.class);

        if (!route.isPresent()) {
            throw new IllegalArgumentException(
                    "Unable find a @Route annotation on the login view "
                            + flowLoginView.getName());
        }

        if (!(applicationContext instanceof WebApplicationContext)) {
            throw new RuntimeException(
                    "VaadinWebSecurity cannot be used without WebApplicationContext.");
        }

        VaadinServletContext vaadinServletContext = new VaadinServletContext(
                ((WebApplicationContext) applicationContext)
                        .getServletContext());
        String loginPath = RouteUtil.getRoutePath(vaadinServletContext,
                flowLoginView);
        if (!loginPath.startsWith("/")) {
            loginPath = "/" + loginPath;
        }
        loginPath = applyUrlMapping(loginPath);

        // Actually set it up
        FormLoginConfigurer<HttpSecurity> formLogin = http.formLogin();
        formLogin.loginPage(loginPath).permitAll();
        formLogin.successHandler(
                getVaadinSavedRequestAwareAuthenticationSuccessHandler(http));
        http.csrf().ignoringRequestMatchers(new AntPathRequestMatcher(loginPath));
        configureLogout(http, logoutSuccessUrl);
        http.exceptionHandling().defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint(loginPath),
                AnyRequestMatcher.INSTANCE);
        viewAccessChecker.setLoginView(flowLoginView);
    }

    /**
     * FIXME Remove as soon as csrf AntPathRequestMatcher is implemented upstream
     */
    private void configureLogout(HttpSecurity http, String logoutSuccessUrl)
            throws Exception {
        SimpleUrlLogoutSuccessHandler logoutSuccessHandler = new SimpleUrlLogoutSuccessHandler();
        logoutSuccessHandler.setDefaultTargetUrl(logoutSuccessUrl);
        logoutSuccessHandler.setRedirectStrategy(new UidlRedirectStrategy());
        http.logout().logoutSuccessHandler(logoutSuccessHandler);
    }

    /**
     * FIXME Remove as soon as csrf AntPathRequestMatcher is implemented upstream
     */
    private VaadinSavedRequestAwareAuthenticationSuccessHandler getVaadinSavedRequestAwareAuthenticationSuccessHandler(
            HttpSecurity http) {
        VaadinSavedRequestAwareAuthenticationSuccessHandler vaadinSavedRequestAwareAuthenticationSuccessHandler = new VaadinSavedRequestAwareAuthenticationSuccessHandler();
        vaadinSavedRequestAwareAuthenticationSuccessHandler
                .setDefaultTargetUrl(applyUrlMapping(""));
        RequestCache requestCache = http.getSharedObject(RequestCache.class);
        if (requestCache != null) {
            vaadinSavedRequestAwareAuthenticationSuccessHandler
                    .setRequestCache(requestCache);
        }
        http.setSharedObject(
                VaadinSavedRequestAwareAuthenticationSuccessHandler.class,
                vaadinSavedRequestAwareAuthenticationSuccessHandler);
        return vaadinSavedRequestAwareAuthenticationSuccessHandler;
    }

    /**
     * TODO Load users from JSON.
     */
    @Bean
    public UserDetailsManager userDetailsService() {
        UserDetails view =
                User.withUsername("view")
                        .password("{noop}view")
                        .roles("VIEW")
                        .build();
        UserDetails admin =
                User.withUsername("admin")
                        .password("{noop}admin")
                        .roles("ADMIN")
                        .build();
        return new InMemoryUserDetailsManager(view, admin);
    }

}
