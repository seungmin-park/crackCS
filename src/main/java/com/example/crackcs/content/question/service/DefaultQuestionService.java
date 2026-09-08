package com.example.crackcs.content.question.service;

import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.question.domain.QuestionOrigin;
import com.example.crackcs.content.question.domain.QuestionStatus;
import com.example.crackcs.exception.QuestionNotFoundException;
import com.example.crackcs.content.question.repository.QuestionRepository;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.exception.TopicNotFoundException;
import com.example.crackcs.content.topic.repository.TopicRepository;
import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.concept.repository.ConceptRepository;
import com.example.crackcs.content.question.domain.QuestionConceptAssignment;
import com.example.crackcs.exception.ConceptNotFoundException;
import com.example.crackcs.exception.InvalidContentStateException;
import com.example.crackcs.exception.MemberNotFoundException;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultQuestionService implements QuestionService {

    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final ConceptRepository conceptRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public Question create(Long creatorMemberId, Long topicId, QuestionDifficulty difficulty, String content,
                           String referenceAnswer) {
        Topic topic = findActiveTopic(topicId);
        Member creator = findAdmin(creatorMemberId);
        Question question = Question.builder()
                .topic(topic)
                .createdByMember(creator)
                .difficulty(difficulty)
                .content(content)
                .referenceAnswer(referenceAnswer)
                .build();

        return questionRepository.save(question);
    }

    @Override
    public Page<Question> findAll(
            Long topicId,
            QuestionStatus status,
            QuestionDifficulty difficulty,
            QuestionOrigin origin,
            Pageable pageable
    ) {
        return questionRepository.findAllByConditions(topicId, status, difficulty, origin, pageable);
    }

    @Override
    public Question findById(Long questionId) {
        return findQuestion(questionId);
    }

    @Override
    @Transactional
    public Question update(
            Long questionId,
            Long topicId,
            QuestionDifficulty difficulty,
            String content,
            String referenceAnswer
    ) {
        Question question = findQuestion(questionId);
        Topic topic = findActiveTopic(topicId);
        question.update(topic, difficulty, content, referenceAnswer);

        return question;
    }

    @Override
    @Transactional
    public Question replaceConcepts(Long questionId, List<QuestionConceptData> concepts) {
        Question question = findQuestion(questionId);
        List<QuestionConceptAssignment> assignments = concepts.stream()
                .map(data -> toAssignment(question, data))
                .toList();
        question.replaceConcepts(assignments);
        return question;
    }

    @Override
    @Transactional
    public Question review(Long questionId, Long reviewerMemberId) {
        Question question = findQuestion(questionId);
        question.review(findAdmin(reviewerMemberId));
        return question;
    }

    @Override
    @Transactional
    public Question publish(Long questionId) {
        Question question = findQuestion(questionId);
        question.publish();
        questionRepository.findAllByVersionSeriesIdAndStatus(
                        question.getVersionSeriesId(), QuestionStatus.PUBLISHED
                ).stream()
                .filter(previous -> !previous.getId().equals(question.getId()))
                .forEach(Question::retire);
        return question;
    }

    @Override
    @Transactional
    public Question retire(Long questionId) {
        Question question = findQuestion(questionId);
        question.retire();
        return question;
    }

    @Override
    @Transactional
    public Question createNextVersion(
            Long questionId,
            Long creatorMemberId,
            QuestionDifficulty difficulty,
            String content,
            String referenceAnswer
    ) {
        Question source = findQuestion(questionId);
        int nextVersion = questionRepository.findMaxVersion(source.getVersionSeriesId()) + 1;
        return questionRepository.save(source.createNextVersion(
                nextVersion,
                findAdmin(creatorMemberId),
                difficulty,
                content,
                referenceAnswer
        ));
    }

    private QuestionConceptAssignment toAssignment(Question question, QuestionConceptData data) {
        Concept concept = conceptRepository.findById(data.conceptId())
                .orElseThrow(() -> new ConceptNotFoundException(data.conceptId()));
        if (!concept.isActive()) {
            throw new InvalidContentStateException("비활성 Concept은 문제에 연결할 수 없습니다.");
        }
        if (!question.hasSameTopicAs(concept)) {
            throw new InvalidContentStateException("문제와 같은 Topic의 Concept만 연결할 수 있습니다.");
        }
        return new QuestionConceptAssignment(concept, data.weight(), data.required());
    }

    private Question findQuestion(Long questionId) {
        return questionRepository.findAdminById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
    }

    private Topic findTopic(Long topicId) {
        return topicRepository.findById(topicId)
                .orElseThrow(() -> new TopicNotFoundException(topicId));
    }

    private Topic findActiveTopic(Long topicId) {
        Topic topic = findTopic(topicId);
        if (!topic.isActive()) {
            throw new InvalidContentStateException("비활성 Topic에는 Question을 연결할 수 없습니다.");
        }
        return topic;
    }

    private Member findAdmin(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
        if (member.getRole() != MemberRole.ADMIN || !member.isAuthenticatable()) {
            throw new InvalidContentStateException("활성 ADMIN 회원만 문제를 등록하거나 검수할 수 있습니다.");
        }
        return member;
    }
}
