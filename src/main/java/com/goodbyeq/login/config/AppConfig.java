package com.goodbyeq.login.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import javax.crypto.Cipher;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.goodbyeq.authorize.encryption.KeyChain;
import com.goodbyeq.authorize.encryption.KeyChainEntries;
import com.goodbyeq.exception.CryptoException;

@Configuration
@EnableTransactionManagement
@PropertySource("classpath:application.properties")
public class AppConfig {
	private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);


	@Value("${keychain.file:goodbyeq_key.xml}")
	private String keyChainFile = "goodbyeq_key.xml";

	@Bean(name = "keyChain")
	public KeyChain keyChain() {
		try {
			return new KeyChain();
		} catch (CryptoException e) {
			logger.error("KeyChain error");
			return null;
		}
	}

	@Bean(name = "appDBProperties")
	public ApplicationDBProperties appProperties() {
		ApplicationDBProperties bean = new ApplicationDBProperties();
		bean.setConnectionHost("goodbyeq.c9rqdaqzwimk.ap-south-1.rds.amazonaws.com");
		bean.setConnectionClass("com.mysql.cj.jdbc.Driver");
		bean.setConnectionPort("3306");
		bean.setConnectionUser("goodbyeq_master");
		bean.setConnectionPassword("goodbyeq-pass-2764$");
		return bean;
	}

	@Bean(name = "dataSource")
	public DataSource getDataSource() {
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUsername("goodbyeq_master");
		dataSource.setPassword("goodbyeq-pass-2764$");
		dataSource.setUrl((null != "jdbc:mysql://goodbyeq.c9rqdaqzwimk.ap-south-1.rds.amazonaws.com:3306/goodbyeq")
				? "jdbc:mysql://goodbyeq.c9rqdaqzwimk.ap-south-1.rds.amazonaws.com:3306/goodbyeq"
				: getDatabaseURL());

		// Connection Pool configuration
		dataSource.setMinIdle(Integer.parseInt("0"));
		dataSource.setMaxIdle(Integer.parseInt("3"));
		dataSource.setMaxTotal(Integer.parseInt("5"));
		dataSource.setMaxWaitMillis(Integer.parseInt("3000"));
		dataSource.setRemoveAbandonedOnBorrow(Boolean.valueOf("true"));
		dataSource.setInitialSize(Integer.parseInt("2"));
		dataSource.setValidationQuery("select count(1) from GBQ_USER");
		dataSource.setValidationQueryTimeout(Integer.parseInt("100"));

		dataSource.setTestOnCreate(true);
		dataSource.setTestOnReturn(true);
		dataSource.setTestOnBorrow(true);

		return dataSource;
	}

	@Bean(name = "entityManagerFactory")
	public LocalContainerEntityManagerFactoryBean entityManagerFactory() {

		HibernateJpaVendorAdapter hibernateJpa = new HibernateJpaVendorAdapter();
		LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
		hibernateJpa.setDatabasePlatform("org.hibernate.dialect.MySQLDialect");
		hibernateJpa.setShowSql(true);
		Properties jpaProperties = new Properties();
		jpaProperties.setProperty("hibernate.hbm2ddl.auto", "validate");
		try {
			emf.setDataSource(getDataSource());
			emf.setPersistenceProviderClass(HibernatePersistenceProvider.class);
			emf.setPackagesToScan("com.goodbyeq.user.db.bo");
			emf.setPersistenceUnitName("Hibernate");
			emf.setJpaProperties(jpaProperties);
		} catch (Exception e) {
			logger.error("Exception while initializing database entity manager", e);
		}
		return emf;
	}

	@Bean
	public JpaTransactionManager geJpaTransactionManager() {
		JpaTransactionManager txnMgr = new JpaTransactionManager();
		txnMgr.setEntityManagerFactory(entityManagerFactory().getObject());
		return txnMgr;
	}

	private String getDatabaseURL() {
		String url = "jdbc:mysql://" + "goodbyeq.c9rqdaqzwimk.ap-south-1.rds.amazonaws.com" + ":" + "3306" + "/"
				+ "goodbyeq" + "?autoReconnectForPools=true";
		return url;
	}

	@Bean
	public KeyChainEntries keyChainEntries() {
		fixKeyLength();
		KeyChainEntries keyChainEntries = null;
		try {
			// ENC
			// Load the googleKMS encrypted file.
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			URL resource = loader.getResource(keyChainFile);
			// File billliveFile = new File(resource.toURI());
			// InputStream encryptedStream = new FileInputStream(billliveFile);
			StringBuilder contentBuilder = new StringBuilder();

			try (Stream<String> stream = Files.lines(Paths.get(resource.toURI()), StandardCharsets.UTF_8)) {
				stream.forEach(s -> contentBuilder.append(s).append("\n"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			byte[] fileInBytes = contentBuilder.toString().getBytes();
			InputStream plainTextStream = new ByteArrayInputStream(fileInBytes);
			keyChainEntries = keyChain().loadKeyChain(plainTextStream);
		} catch (URISyntaxException | CryptoException e) {
			logger.info("Error while encrypting");
		}
		return keyChainEntries;
	}

	private void fixKeyLength() {
		String errorString = "Failed manually overriding key-length permissions.";
		int newMaxKeyLength;
		try {
			if ((newMaxKeyLength = Cipher.getMaxAllowedKeyLength("AES")) < 256) {
				Class c = Class.forName("javax.crypto.CryptoAllPermissionCollection");
				Constructor con = c.getDeclaredConstructor();
				con.setAccessible(true);
				Object allPermissionCollection = con.newInstance();
				Field f = c.getDeclaredField("all_allowed");
				f.setAccessible(true);
				f.setBoolean(allPermissionCollection, true);

				c = Class.forName("javax.crypto.CryptoPermissions");
				con = c.getDeclaredConstructor();
				con.setAccessible(true);
				Object allPermissions = con.newInstance();
				f = c.getDeclaredField("perms");
				f.setAccessible(true);
				((Map) f.get(allPermissions)).put("*", allPermissionCollection);

				c = Class.forName("javax.crypto.JceSecurityManager");
				f = c.getDeclaredField("defaultPolicy");
				f.setAccessible(true);
				Field mf = Field.class.getDeclaredField("modifiers");
				mf.setAccessible(true);
				mf.setInt(f, f.getModifiers() & ~Modifier.FINAL);
				f.set(null, allPermissions);

				newMaxKeyLength = Cipher.getMaxAllowedKeyLength("AES");
			}
		} catch (Exception e) {
			throw new RuntimeException(errorString, e);
		}
		if (newMaxKeyLength < 256)
			throw new RuntimeException(errorString); // hack failed
	}
}