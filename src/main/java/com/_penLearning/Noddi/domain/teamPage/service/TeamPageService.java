package com._penLearning.Noddi.domain.teamPage.service;

import com._penLearning.Noddi.domain.teamPage.repository.TeamPageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamPageService {

    private final TeamPageRepository teamPageRepository;


}
