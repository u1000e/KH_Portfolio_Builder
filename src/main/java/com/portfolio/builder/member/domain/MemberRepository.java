package com.portfolio.builder.member.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByGithubId(String githubId);

    Optional<Member> findByGithubUsername(String githubUsername);

    List<Member> findByStatus(Member.Status status);

    List<Member> findByPosition(String position);

    List<Member> findByBranch(String branch);
}
