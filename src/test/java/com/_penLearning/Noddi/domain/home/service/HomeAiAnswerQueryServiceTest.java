package com._penLearning.Noddi.domain.home.service;

import com._penLearning.Noddi.domain.home.code.HomeErrorCode;
import com._penLearning.Noddi.domain.home.repository.HomeAiAnswerDetailRepository;
import com._penLearning.Noddi.domain.home.repository.HomeAiAnswerRepository;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerSourceRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeAiAnswerQueryServiceTest {

    @Mock
    private HomeAiAnswerRepository homeAiAnswerRepository;

    @Mock
    private HomeAiAnswerDetailRepository homeAiAnswerDetailRepository;

    @Mock
    private QaAnswerSourceRepository qaAnswerSourceRepository;

    private HomeAiAnswerQueryService service;

    @BeforeEach
    void setUp() {
        service = new HomeAiAnswerQueryService(
                homeAiAnswerRepository,
                homeAiAnswerDetailRepository,
                qaAnswerSourceRepository
        );
    }

    @Test
    void rejectsInvalidPageRequestBeforeAccessingRepositories() {
        assertThatThrownBy(() -> service.getUnreadAnswerCards(
                1L,
                2L,
                -1,
                10
        )).isInstanceOf(GeneralException.class)
                .satisfies(exception -> assertThat(
                        ((GeneralException) exception).getErrorCode()
                ).isEqualTo(HomeErrorCode.INVALID_PAGE_REQUEST));

        assertThatThrownBy(() -> service.getUnreadAnswerCards(
                1L,
                2L,
                0,
                51
        )).isInstanceOf(GeneralException.class)
                .satisfies(exception -> assertThat(
                        ((GeneralException) exception).getErrorCode()
                ).isEqualTo(HomeErrorCode.INVALID_PAGE_REQUEST));

        verifyNoInteractions(
                homeAiAnswerRepository,
                homeAiAnswerDetailRepository,
                qaAnswerSourceRepository
        );
    }

    @Test
    void rejectsUsersWhoAreNotProjectMembersBeforeLoadingCards() {
        when(homeAiAnswerRepository.existsProjectMembership(2L, 1L))
                .thenReturn(false);

        assertThatThrownBy(() -> service.getUnreadAnswerCards(
                1L,
                2L,
                0,
                10
        )).isInstanceOf(GeneralException.class)
                .satisfies(exception -> assertThat(
                        ((GeneralException) exception).getErrorCode()
                ).isEqualTo(HomeErrorCode.NOT_PROJECT_MEMBER));

        verify(homeAiAnswerRepository, never())
                .findUnreadAiAnswerNotifications(
                        anyLong(),
                        anyLong(),
                        any(),
                        any(),
                        any()
                );
        verifyNoInteractions(
                homeAiAnswerDetailRepository,
                qaAnswerSourceRepository
        );
    }
}
