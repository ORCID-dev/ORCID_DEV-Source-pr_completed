package org.orcid.frontend.spring.web.social.config;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.social.connect.jdbc.JdbcUsersConnectionRepository;
import org.springframework.social.connect.support.ConnectionFactoryRegistry;
import org.springframework.social.connect.web.ProviderSignInController;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.impl.FacebookTemplate;
import org.springframework.social.facebook.connect.FacebookConnectionFactory;

/**
 * 
 * @author Shobhit Tyagi
 *
 */
@Configuration
public class FacebookConfig implements InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(FacebookConfig.class);

	private static final String appId = "161378537526754";
	private static final String appSecret = "9382f990c575be17af9afd7c8b856532";

	private SocialContext socialContext;

	private UsersConnectionRepository usersConnectionRepositiory;

	@Resource
	private DataSource simpleDataSource;

	@Bean
	public SocialContext socialContext() {

		return socialContext;
	}

	@Bean
	public ConnectionFactoryLocator connectionFactoryLocator() {
		logger.info("getting connectionFactoryLocator");
		ConnectionFactoryRegistry registry = new ConnectionFactoryRegistry();
		registry.addConnectionFactory(new FacebookConnectionFactory(appId, appSecret));
		return registry;
	}

	@Bean
	public UsersConnectionRepository usersConnectionRepository() {
		return usersConnectionRepositiory;
	}

	@Bean
	@Scope(value = "request", proxyMode = ScopedProxyMode.INTERFACES)
	public ConnectionRepository connectionRepository() {
		String userId = socialContext.getUserId();
		return usersConnectionRepository().createConnectionRepository(userId);
	}

	@Bean
	@Scope(value = "request", proxyMode = ScopedProxyMode.INTERFACES)
	public Facebook facebook() {
		String accessToken = connectionRepository().getPrimaryConnection(Facebook.class).createData().getAccessToken();
		return new FacebookTemplate(accessToken);
	}

	@Bean
	public ProviderSignInController providerSignInController() {
		ProviderSignInController providerSigninController = new ProviderSignInController(connectionFactoryLocator(),
				usersConnectionRepository(), socialContext);
		providerSigninController.setPostSignInUrl("/fb/access");
		return providerSigninController;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		JdbcUsersConnectionRepository usersConnectionRepositiory = new JdbcUsersConnectionRepository(simpleDataSource,
				connectionFactoryLocator(), Encryptors.noOpText());
		socialContext = new SocialContext(usersConnectionRepositiory, new UserCookieGenerator(), facebook());
		usersConnectionRepositiory.setConnectionSignUp(socialContext);
		this.usersConnectionRepositiory = usersConnectionRepositiory;
	}
}