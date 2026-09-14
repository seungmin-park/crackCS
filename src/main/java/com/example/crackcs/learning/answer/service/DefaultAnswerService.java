package com.example.crackcs.learning.answer.service;

import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionStatus;
import com.example.crackcs.content.question.repository.QuestionRepository;
import com.example.crackcs.evaluation.domain.Evaluation;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import com.example.crackcs.exception.AnswerConflictException;
import com.example.crackcs.exception.AnswerNotFoundException;
import com.example.crackcs.exception.InvalidContentStateException;
import com.example.crackcs.exception.MemberNotFoundException;
import com.example.crackcs.exception.QuestionNotFoundException;
import com.example.crackcs.learning.answer.domain.Answer;
import com.example.crackcs.learning.answer.repository.AnswerRepository;
import com.example.crackcs.learning.answer.service.result.AnswerEvaluationResult;
import com.example.crackcs.learning.answer.service.result.AnswerResult;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.repository.MemberRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public AnswerResult submit(Long memberId, Long questionId, String requestId, String content) {
        Member member = members.findLockedById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
        if (!member.isAuthenticatable()) {
            throw new InvalidContentStateException("활성 회원만 답변할 수 있습니다.");
        }
        Optional<Answer> existing = answers.findByMemberIdAndRequestId(memberId, requestId);
        if (existing.isPresent()) {
            Answer answer = existing.get();
            if (!answer.getQuestion().getId().equals(questionId) || !answer.getContent().equals(content)) {
                throw new AnswerConflictException();
            }
            return response(answer);
        }
        Question question = questions.findById(questionId)
                .filter(candidate -> candidate.getStatus() == QuestionStatus.PUBLISHED)
                .filter(candidate -> candidate.isUnrestrictedOrOwnedBy(member))
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
        Answer answer = answers.save(Answer.builder()
                .member(member)
                .question(question)
                .requestId(requestId)
                .content(content)
                .build());
        Evaluation evaluation = evaluations.save(Evaluation.builder().answer(answer).build());
        return AnswerResult.from(answer, evaluation);
    }

    @Override
    public AnswerResult findById(Long memberId, Long answerId) {
        return response(ownedAnswer(memberId, answerId));
    }

    @Override
    public AnswerEvaluationResult findEvaluation(Long memberId, Long answerId) {
        ownedAnswer(memberId, answerId);
        return AnswerEvaluationResult.from(evaluations.findByAnswerId(answerId).orElseThrow());
    }

    @Override
    public Page<AnswerResult> findAll(Long memberId, Pageable pageable) {
        Pageable latest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Order.desc("submittedAt"), Sort.Order.desc("id")));
        Page<Answer> answerPage = answers.findByMemberId(memberId, latest);
        List<Long> answerIds = answerPage.getContent().stream().map(Answer::getId).toList();
        Map<Long, Evaluation> evaluationByAnswerId = evaluations.findAllDetailsByAnswerIds(answerIds).stream()
                .collect(Collectors.toMap(evaluation -> evaluation.getAnswer().getId(), Function.identity()));
        return answerPage.map(answer -> AnswerResult.from(answer, evaluationByAnswerId.get(answer.getId())));
    }

    private Answer ownedAnswer(Long memberId, Long answerId) {
        return answers.findByIdAndMemberId(answerId, memberId).orElseThrow(() -> new AnswerNotFoundException(answerId));
    }

    private AnswerResult response(Answer answer) {
        return AnswerResult.from(answer, evaluations.findByAnswerId(answer.getId()).orElseThrow());
    }
}
