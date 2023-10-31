package nz.ac.canterbury.seng302.portfolio;

import nz.ac.canterbury.seng302.portfolio.authentication.JwtAuthenticationFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity security) throws Exception
    {
        // Force authentication for all endpoints except the given pages
        security
                .addFilterBefore(new JwtAuthenticationFilter(), BasicAuthenticationFilter.class)
                .authorizeRequests()
                .antMatchers("/login")
                .permitAll()
                .and()
                .authorizeRequests()
                .antMatchers("/register")
                .permitAll()
                .and()
                .authorizeRequests()
                .antMatchers(HttpMethod.GET, "/logout")
                .permitAll()
                .and()
                .authorizeRequests().antMatchers("/").permitAll()
                .and()
                .authorizeRequests().antMatchers("/console/**").permitAll()
                .and()
                .authorizeRequests().antMatchers("/css/**").permitAll()
                .and()
                .authorizeRequests().antMatchers("/js/**").permitAll()
                .and()
                .authorizeRequests().antMatchers("/webjars/**").permitAll()
                .and()
                .authorizeRequests()
                .antMatchers(HttpMethod.GET, "/")
                .permitAll()
                .and()
                .authorizeRequests()
                .anyRequest()
                .authenticated();


        security.cors();
        security.csrf().disable();
        security.logout()
                .permitAll()
                .invalidateHttpSession(true)
                .deleteCookies("lens-session-token")
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.OK));

        // Disable basic http security and the spring security login form
        security
                .httpBasic().disable()
                .formLogin().disable();

        // Allows the H2 console to load properly with authentication settings
        security.csrf().disable();
        security.headers().frameOptions().disable();

        // Disable error handling. This means that authentication errors aren't caught, and can then be passed through
        // to springboot/thymleaf error handling
        security.exceptionHandling().disable();
    }

    @Override
    public void configure(WebSecurity web) throws Exception
    {
        web.ignoring().antMatchers("/login");
        web.ignoring().antMatchers("/css/**");
        web.ignoring().antMatchers("/js/**");
        web.ignoring().antMatchers("/register");
        web.ignoring().antMatchers("/logout");
        web.ignoring().antMatchers("/");
        web.ignoring().antMatchers("/webjars/**");
    }
}
