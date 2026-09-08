package com.example.crackcs.evaluation.retrieval;

public interface KnowledgeRetrievalService {

    RetrievalResult retrieve(RetrievalQuery query, int limit);
}
