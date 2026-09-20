package com.selfdiscipline.service;

import com.selfdiscipline.exception.ApiException;
import com.selfdiscipline.model.Diary;
import com.selfdiscipline.model.User;
import com.selfdiscipline.repository.DiaryRepository;
import com.selfdiscipline.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiaryServiceTest {

    @Mock
    private DiaryRepository diaryRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DiaryService diaryService;

    private User alice;

    @BeforeEach
    void setUp() {
        alice = new User();
        alice.setId("u1");
        alice.setUsername("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
    }

    @Test
    void listMergesMongoIdAndLegacyUsernameRows() {
        Diary byId = diary("d1", "u1", "2026-09-20");
        Diary legacy = diary("d2", "alice", "2026-09-19");
        when(diaryRepository.findByUserIdOrderByDiaryDateDesc("u1")).thenReturn(List.of(byId));
        when(diaryRepository.findByUserIdOrderByDiaryDateDesc("alice")).thenReturn(List.of(legacy));

        List<Diary> diaries = diaryService.listDiaries("alice");

        assertEquals(2, diaries.size());
        assertEquals("d1", diaries.get(0).getId());
        assertEquals("d2", diaries.get(1).getId());
    }

    @Test
    void createWritesMongoUserId() {
        when(diaryRepository.findByUserIdAndDiaryDate("u1", "2026-09-20")).thenReturn(Optional.empty());
        when(diaryRepository.findByUserIdAndDiaryDate("alice", "2026-09-20")).thenReturn(Optional.empty());
        when(diaryRepository.save(any(Diary.class))).thenAnswer(inv -> inv.getArgument(0));

        Diary input = new Diary();
        input.setDiaryDate("2026-09-20");
        input.setContent("hello");
        Diary saved = diaryService.createOrUpdateDiary("alice", input);

        assertEquals("u1", saved.getUserId());
        ArgumentCaptor<Diary> captor = ArgumentCaptor.forClass(Diary.class);
        verify(diaryRepository).save(captor.capture());
        assertEquals("u1", captor.getValue().getUserId());
    }

    @Test
    void updateMigratesLegacyUsernameOwnerToMongoId() {
        Diary legacy = diary("d1", "alice", "2026-09-20");
        when(diaryRepository.findByUserIdAndDiaryDate("u1", "2026-09-20")).thenReturn(Optional.empty());
        when(diaryRepository.findByUserIdAndDiaryDate("alice", "2026-09-20")).thenReturn(Optional.of(legacy));
        when(diaryRepository.save(any(Diary.class))).thenAnswer(inv -> inv.getArgument(0));

        Diary input = new Diary();
        input.setDiaryDate("2026-09-20");
        input.setContent("updated");
        Diary saved = diaryService.createOrUpdateDiary("alice", input);

        assertEquals("u1", saved.getUserId());
        assertEquals("updated", saved.getContent());
    }

    @Test
    void getAndDeleteAcceptLegacyUsernameOwner() {
        Diary legacy = diary("d1", "alice", "2026-09-20");
        when(diaryRepository.findById("d1")).thenReturn(Optional.of(legacy));

        assertNotNull(diaryService.getDiary("alice", "d1"));
        diaryService.deleteDiary("alice", "d1");
        verify(diaryRepository).deleteById("d1");
    }

    @Test
    void getRejectsOtherUsersDiary() {
        Diary other = diary("d1", "u-other", "2026-09-20");
        when(diaryRepository.findById("d1")).thenReturn(Optional.of(other));

        assertNull(diaryService.getDiary("alice", "d1"));
    }

    @Test
    void deleteMissingOrForeignDiaryThrowsNotFound() {
        when(diaryRepository.findById("missing")).thenReturn(Optional.empty());
        ApiException missing = assertThrows(ApiException.class, () -> diaryService.deleteDiary("alice", "missing"));
        assertEquals("日记不存在", missing.getMessage());

        Diary other = diary("d1", "u-other", "2026-09-20");
        when(diaryRepository.findById("d1")).thenReturn(Optional.of(other));
        assertThrows(ApiException.class, () -> diaryService.deleteDiary("alice", "d1"));
        verify(diaryRepository, never()).deleteById(any());
    }

    @Test
    void unknownMoodFallsBackToNeutralOnSave() {
        when(diaryRepository.findByUserIdAndDiaryDate("u1", "2026-09-20")).thenReturn(Optional.empty());
        when(diaryRepository.findByUserIdAndDiaryDate("alice", "2026-09-20")).thenReturn(Optional.empty());
        when(diaryRepository.save(any(Diary.class))).thenAnswer(inv -> inv.getArgument(0));

        Diary input = new Diary();
        input.setDiaryDate("2026-09-20");
        input.setContent("hello");
        input.setMood("angry");
        Diary saved = diaryService.createOrUpdateDiary("alice", input);

        assertEquals("neutral", saved.getMood());
    }

    private static Diary diary(String id, String userId, String date) {
        Diary diary = new Diary();
        diary.setId(id);
        diary.setUserId(userId);
        diary.setDiaryDate(date);
        diary.setContent("note");
        return diary;
    }
}
