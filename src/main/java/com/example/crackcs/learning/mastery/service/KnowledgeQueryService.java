package com.example.crackcs.learning.mastery.service;

import com.example.crackcs.learning.mastery.service.result.KnowledgeStatesResult;

public interface KnowledgeQueryService {
    KnowledgeStatesResult knowledgeStates(Long memberId);
}
