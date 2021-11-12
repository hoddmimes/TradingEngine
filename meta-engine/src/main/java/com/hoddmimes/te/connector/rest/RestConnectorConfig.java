package com.hoddmimes.te.connector.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.session.SessionManagementFilter;


@Configuration
@EnableWebSecurity
public class RestConnectorConfig extends WebSecurityConfigurerAdapter
{
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.ALWAYS);
		http.addFilterBefore(new TeFilter(), SessionManagementFilter.class);
		http.antMatcher("/**").authorizeRequests().anyRequest().permitAll();
		http.cors().and().csrf().disable();
	}
}
