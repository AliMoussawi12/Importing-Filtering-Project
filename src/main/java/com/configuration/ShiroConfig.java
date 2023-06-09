package com.configuration;

import com.authentication.UserRealm;
import java.util.List;

import com.authentication.filter.AuthenticationFilter;
import org.apache.shiro.authc.Authenticator;
import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.authc.credential.PasswordMatcher;
import org.apache.shiro.authc.pam.FirstSuccessfulStrategy;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.mgt.SessionStorageEvaluator;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.DefaultWebSessionStorageEvaluator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.Filter;

@Configuration
@EnableAsync(proxyTargetClass = true)
public class ShiroConfig {

    @Bean
    DefaultPasswordService pwdService(){
        return new DefaultPasswordService();
    }

    @Bean("userRealm")
    public UserRealm userRealm() {
        UserRealm userRealm = new UserRealm();
        PasswordMatcher pwdMatcher = new PasswordMatcher();
        pwdMatcher.setPasswordService(pwdService());
        userRealm.setCredentialsMatcher(pwdMatcher);
        userRealm.setCachingEnabled(false);
        return userRealm;
    }


    @Bean("shiroFilterFactoryBean")
    public ShiroFilterFactoryBean shiroFilterFactoryBean(@Qualifier("defaultSecurityManager")
                                                         DefaultWebSecurityManager securityManager) {
        ShiroFilterFactoryBean factoryBean = new ShiroFilterFactoryBean();

        Map<String, Filter> filterMap = new HashMap<>();
        // Add custom filter
        filterMap.put("api", new AuthenticationFilter());
        factoryBean.setFilters(filterMap);

        // Config security manager
        factoryBean.setSecurityManager(securityManager);
        factoryBean.setUnauthorizedUrl("/401");

         /*
        - anon:  Access without authentication
        - authc:  Must be authenticated to access
        - user:  You have to remember me to use it
        - perms:  You can access a resource only if you have permission to it
        - role:  Have a role permission
         */
        Map<String, String> filterRuleMap = new HashMap<>();
        filterRuleMap.put("/api/user/**", "api");
        filterRuleMap.put("/api/**","anon");
        filterRuleMap.put("/401", "anon");
        factoryBean.setFilterChainDefinitionMap(filterRuleMap);
        return factoryBean;
    }

    @Bean
    public SessionStorageEvaluator sessionStorageEvaluator() {
        DefaultSessionStorageEvaluator sessionStorageEvaluator = new DefaultWebSessionStorageEvaluator();
        sessionStorageEvaluator.setSessionStorageEnabled(false);
        return sessionStorageEvaluator;
    }

    @Bean
    public DefaultSubjectDAO subjectDAO() {
        DefaultSubjectDAO defaultSubjectDAO = new DefaultSubjectDAO();
        defaultSubjectDAO.setSessionStorageEvaluator(sessionStorageEvaluator());
        return defaultSubjectDAO;
    }

    @Bean(name="defaultSecurityManager")
    public DefaultWebSecurityManager getDefaultWebSecurityManager() {

        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(userRealm());
        securityManager.setSubjectDAO(subjectDAO());
        return securityManager;
    }

    @Bean
    public Authenticator authenticator() {
        ModularRealmAuthenticator authenticator = new ModularRealmAuthenticator();
        List<Realm> realms = new java.util.ArrayList<>();
        realms.add(userRealm());
        authenticator.setRealms(realms);
        authenticator.setAuthenticationStrategy(new FirstSuccessfulStrategy());
        return authenticator;
    }

}

