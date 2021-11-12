package com.hoddmimes.resttest.server;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.session.SessionManagementFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityconfig extends WebSecurityConfigurerAdapter
{

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.ALWAYS);
		http.addFilterBefore(new TestFilter(), SessionManagementFilter.class);
		http.cors().and().csrf().disable();

	}
}
