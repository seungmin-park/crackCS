package com.example.crackcs.content.knowledge.chunk.config;

import com.example.crackcs.content.knowledge.chunk.service.KnowledgeChunkPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class KnowledgeChunkConfiguration {

    @Bean
    public KnowledgeChunkPolicy knowledgeChunkPolicy() {
        return new KnowledgeChunkPolicy(1000, 150, "paragraph-1000-overlap-150-v1");
    }
}
