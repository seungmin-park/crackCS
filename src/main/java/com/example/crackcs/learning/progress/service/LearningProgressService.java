package com.example.crackcs.learning.progress.service;

import com.example.crackcs.learning.progress.service.result.LearningProgressResult;

public interface LearningProgressService {
    LearningProgressResult progress(Long memberId);
}
