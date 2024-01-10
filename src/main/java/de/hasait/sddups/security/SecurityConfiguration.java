package de.hasait.sddups.security;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import de.hasait.sddups.ui.LoginView;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@EnableWebSecurity
@Configuration
public class SecurityConfiguration
        extends VaadinWebSecurity {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfiguration.class);

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Delegating the responsibility of general configurations
        // of http security to the super class. It's configuring
        // the followings: Vaadin's CSRF protection by ignoring
        // framework's internal requests, default request cache,
        // ignoring public views annotated with @AnonymousAllowed,
        // restricting access to other views/endpoints, and enabling
        // NavigationAccessControl authorization.
        // You can add any possible extra configurations of your own
        // here (the following is just an example):

        // http.rememberMe().alwaysRemember(false);

        // Configure your static resources with public access before calling
        // super.configure(HttpSecurity) as it adds final anyRequest matcher
        http.authorizeHttpRequests(auth -> auth.requestMatchers(new AntPathRequestMatcher("/public/**"))
                .permitAll());

        super.configure(http);

        // This is important to register your login view to the
        // navigation access control mechanism:
        setLoginView(http, LoginView.class);
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        // Customize your WebSecurity configuration.
        super.configure(web);
    }

    @Bean
    public UserDetailsManager userDetailsService() {
        InMemoryUserDetailsManager userDetailsManager = new InMemoryUserDetailsManager();

        File usersJsonFile = new File("users.json");
        if (!usersJsonFile.canRead()) {
            throw new RuntimeException("Users configuration does not exist or cannot be read: " + usersJsonFile);
        }

        log.info("Reading users from {}...", usersJsonFile);
        String usersJson;
        try {
            usersJson = FileUtils.readFileToString(usersJsonFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        JsonObject jsonRoot = Json.parse(usersJson);
        JsonArray jsonUsers = jsonRoot.getArray("users");
        for (int i = 0; i < jsonUsers.length(); i++) {
            JsonObject jsonUser = jsonUsers.getObject(i);
            String username = jsonUser.getString("username");
            log.debug("Found user: {}", username);

            UserDetails user =
                    User.withUsername(username)
                            .password(jsonUser.getString("password"))
                            .roles(jsonUser.getString("role"))
                            .build();
            userDetailsManager.createUser(user);
        }

        return userDetailsManager;
    }

}
