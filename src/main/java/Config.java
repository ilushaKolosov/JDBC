import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jasypt.util.text.AES256TextEncryptor;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import javax.sql.DataSource;
import java.io.InputStream;

public class Config {
    private DataSource dataSource;

    public Config() {
        LoaderOptions loaderOptions = new LoaderOptions();
        Constructor constructor = new Constructor(ConfigProperties.class, loaderOptions);
        Yaml yaml = new Yaml(constructor);

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application.yaml")) {
            if (inputStream == null) {
                throw new IllegalArgumentException("application.yaml not found in the classpath");
            }
            ConfigProperties config = yaml.load(inputStream);
            DatabaseConfig databaseConfig = config.getDatabase();

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(databaseConfig.getUrl());
            hikariConfig.setUsername(databaseConfig.getUsername());
            hikariConfig.setPassword(decryptPassword(databaseConfig.getPassword()));
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            this.dataSource = new HikariDataSource(hikariConfig);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String decryptPassword(String encryptedPassword) {
        AES256TextEncryptor textEncryptor = new AES256TextEncryptor();
        textEncryptor.setPassword("encryptionKey"); // Replace with your encryption key
        return textEncryptor.decrypt(encryptedPassword);
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public static void main(String[] args) {
        Config config = new Config();
        System.out.println("DataSource initialized: " + (config.getDataSource() != null));
    }
}