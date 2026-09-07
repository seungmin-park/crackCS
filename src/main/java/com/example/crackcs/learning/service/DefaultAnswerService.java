package com.example.crackcs.learning.service;

import com.example.crackcs.learning.controller.response.*;
import com.example.crackcs.learning.domain.Answer;
import com.example.crackcs.learning.repository.AnswerRepository;
import com.example.crackcs.evaluation.domain.Evaluation;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import com.example.crackcs.content.question.repository.QuestionRepository;
import com.example.crackcs.content.question.domain.QuestionStatus;
import com.example.crackcs.member.repository.MemberRepository;
import com.example.crackcs.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultAnswerService implements AnswerService {
    private final AnswerRepository answers;
    private final EvaluationRepository evaluations;
    private final MemberRepository members;
    private final QuestionRepository questions;

    @Override
    @Transactional
    public AnswerResponse submit(Long memberId, Long questionId, String requestId, String content) {
        var member = members.findLockedById(memberId).orElseThrow(() -> new MemberNotFoundException(memberId));
        if (!member.isAuthenticatable()) throw new InvalidContentStateException("활성 회원만 답변할 수 있습니다.");
        var existing = answers.findByMemberIdAndRequestId(memberId, requestId);
        if (existing.isPresent()) {
            Answer answer = existing.get();
            if (!answer.getQuestion().getId().equals(questionId) || !answer.getContent().equals(content)) {
                throw new AnswerConflictException();
            }
            return response(answer);
        }
        var question = questions.findById(questionId).filter(q -> q.getStatus() == QuestionStatus.PUBLISHED)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
        var answer = answers.save(Answer.builder().member(member).question(question).requestId(requestId).content(content).build());
        var evaluation = evaluations.save(Evaluation.builder().answer(answer).build());
        return AnswerResponse.from(answer, evaluation);
    }

    @Override
    public AnswerResponse findById(Long memberId, Long answerId) {
        return response(ownedAnswer(memberId, answerId));
    }

    @Override
    public EvaluationResponse findEvaluation(Long memberId, Long answerId) {
        ownedAnswer(memberId, answerId);
        return EvaluationResponse.from(evaluations.findByAnswerId(answerId).orElseThrow());
    }

    @Override
    public Page<AnswerResponse> findAll(Long memberId, Pageable pageable) {
        var latest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Order.desc("submittedAt"), Sort.Order.desc("id")));
        return answers.findByMemberId(memberId, latest).map(this::response);
    }

    private Answer ownedAnswer(Long memberId, Long answerId) {
        return answers.findByIdAndMemberId(answerId, memberId).orElseThrow(() -> new AnswerNotFoundException(answerId));
    }

    private AnswerResponse response(Answer answer) {
        return AnswerResponse.from(answer, evaluations.findByAnswerId(answer.getId()).orElseThrow());
    }
}
