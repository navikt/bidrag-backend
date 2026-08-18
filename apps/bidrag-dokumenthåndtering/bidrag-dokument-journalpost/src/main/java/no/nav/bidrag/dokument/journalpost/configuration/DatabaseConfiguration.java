package no.nav.bidrag.dokument.journalpost.configuration;

import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles.INTEGRATION_DB2;
import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles.NAIS;

import java.util.Properties;
import javax.sql.DataSource;
import no.nav.bidrag.dokument.journalpost.entity.Journalsak;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@Profile({NAIS, INTEGRATION_DB2})
@EnableTransactionManagement
public class DatabaseConfiguration {

  @Value("${BIDRAG_DB_HOST}")
  private String jdbcHost;

  @Value("${BIDRAG_DB_PORT}")
  private String jdbcPort;

  @Value("${BIDRAG_DB_NAME}")
  private String jdbcDBName;

  @Value("${BIDRAG_DB_SCHEMA}")
  private String jdbcDBSchemaName;

  @Value("${BIDRAG_DB_USERNAME}")
  private String username;

  @Value("${BIDRAG_DB_PASSWORD}")
  private String password;

  @Bean
  public DataSource dataSource() {
    return DataSourceBuilder.create()
        .url(
            "jdbc:db2://"
                + jdbcHost
                + ":"
                + jdbcPort
                + "/"
                + jdbcDBName
                + ":currentSchema="
                + jdbcDBSchemaName
                + ";")
        .username(username)
        .password(password)
        .driverClassName("com.ibm.db2.jcc.DB2Driver")
        .build();
  }

  @Bean
  public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
    LocalContainerEntityManagerFactoryBean entityManagerFactoryBean =
        new LocalContainerEntityManagerFactoryBean();
    entityManagerFactoryBean.setDataSource(dataSource());
    entityManagerFactoryBean.setPackagesToScan(Journalsak.class.getPackage().getName());

    JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    entityManagerFactoryBean.setJpaVendorAdapter(vendorAdapter);
    entityManagerFactoryBean.setJpaProperties(initJpaProperties());

    return entityManagerFactoryBean;
  }

  private Properties initJpaProperties() {
    Properties properties = new Properties();
    properties.setProperty("hibernate.hbm2ddl.auto", "none");
    properties.setProperty("hibernate.dialect", "org.hibernate.dialect.DB2Dialect");

    return properties;
  }
}
