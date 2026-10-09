package br.com.securelogin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Configuração do MongoDB.
 *
 * <p>A conexão em si (URI do Atlas, nome do banco) é feita pela autoconfiguração do
 * Spring Boot a partir de {@code spring.data.mongodb.uri}, que por sua vez lê a variável
 * de ambiente {@code MONGODB_URI}. Assim nenhuma credencial fica no código-fonte.</p>
 *
 * <p>{@link EnableMongoAuditing} preenche automaticamente os campos anotados com
 * {@code @CreatedDate} e {@code @LastModifiedDate}.</p>
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
