package artifixal.agrest.common;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Listener allowing Liquibase migrations in tests.
 */
public class LiquibaseTestExecutionListener extends AbstractTestExecutionListener{

    private static final AtomicBoolean migrationRun=new AtomicBoolean(false);
    private SpringLiquibase springLiquibase;
    private BeanFactory beanFactory;

    @Override
    public void prepareTestInstance(TestContext testContext) throws Exception{
        ApplicationContext context=testContext.getApplicationContext();
        beanFactory=context;

        if(springLiquibase==null){
            try{
                springLiquibase=beanFactory.getBean(SpringLiquibase.class);
            }catch(NoSuchBeanDefinitionException e){
                // Manually create if auto-config skipped
                springLiquibase=createSpringLiquibase(context);
            }
        }
        if(!migrationRun.get()){
            springLiquibase.afterPropertiesSet();
            migrationRun.set(true);
        }
    }

    private SpringLiquibase createSpringLiquibase(ApplicationContext context){
        Environment env=context.getEnvironment();
        SpringLiquibase liquibase=new SpringLiquibase();
        liquibase.setChangeLog(env.getProperty("spring.liquibase.change-log","classpath:db/changelog/db.changelog-master-test.yml"));
        liquibase.setDropFirst(env.getProperty("spring.liquibase.drop-first",Boolean.class,false));
        liquibase.setDefaultSchema(env.getProperty("spring.liquibase.default-schema","public"));
        try{
            DataSource ds=beanFactory.getBean(DataSource.class);
            liquibase.setDataSource(ds);
        }catch(NoSuchBeanDefinitionException e){
            DriverManagerDataSource manualDs=new DriverManagerDataSource();
            manualDs.setUrl(env.getProperty("spring.liquibase.url","jdbc:postgresql://localhost:5432/agresttest"));
            manualDs.setUsername(env.getProperty("spring.liquibase.user","postgres"));
            manualDs.setPassword(env.getProperty("spring.liquibase.password","password"));
            manualDs.setDriverClassName(env.getProperty("spring.liquibase.driver-class-name","org.postgresql.Driver"));
            liquibase.setDataSource(manualDs);
        }
        return liquibase;
    }

    @Override
    public int getOrder(){
        return Integer.MAX_VALUE;
    }
}
