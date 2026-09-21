package com.selfdiscipline.testsupport;

import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.OngoingStubbing;
import org.mockito.verification.VerificationMode;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Mockito matchers return {@code null}; Eclipse null-analysis (16778128)
 * treats that as converting {@code T} to {@code @NonNull T}. Keep those
 * conversions in this type so test methods stay clean.
 */
public final class MockitoArgs {
    private MockitoArgs() {
    }

    public static <T> T firstArg(InvocationOnMock inv, Class<T> type) {
        return inv.getArgument(0, type);
    }

    public static <T> OngoingStubbing<T> whenSave(CrudRepository<T, ?> repository, Class<T> type) {
        return (OngoingStubbing<T>) when(repository.save(any(type)));
    }

    public static <T> OngoingStubbing<T> lenientWhenSave(CrudRepository<T, ?> repository, Class<T> type) {
        return (OngoingStubbing<T>) org.mockito.Mockito.lenient().when(repository.save(any(type)));
    }

    public static <T> void stubSaveReturnsArg(CrudRepository<T, ?> repository, Class<T> type) {
        whenSave(repository, type).thenAnswer(inv -> firstArg(inv, type));
    }

    public static <T> T verifySaved(CrudRepository<T, ?> repository) {
        ArgumentCaptor<T> captor = ArgumentCaptor.captor();
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    public static <T> T verifySaved(CrudRepository<T, ?> repository, VerificationMode mode) {
        ArgumentCaptor<T> captor = ArgumentCaptor.captor();
        verify(repository, mode).save(captor.capture());
        return captor.getValue();
    }

    public static <T> void verifySaved(CrudRepository<T, ?> repository, T entity) {
        verify(repository).save(entity);
    }

    public static <T> void verifyNeverSaved(CrudRepository<T, ?> repository, Class<T> type) {
        verify(repository, never()).save(any(type));
    }

    public static <T> void verifyDeleted(CrudRepository<T, ?> repository, T entity) {
        verify(repository).delete(entity);
    }

    public static <T> void verifyNeverSavedMatching(CrudRepository<T, ?> repository, ArgumentMatcher<T> matcher) {
        verify(repository, never()).save(argThat(matcher));
    }

    public static <T> List<T> verifySavedAll(CrudRepository<T, ?> repository) {
        ArgumentCaptor<List<T>> captor = ArgumentCaptor.captor();
        verify(repository).saveAll(captor.capture());
        return captor.getValue();
    }

    public static <T> void verifyNeverSavedAll(CrudRepository<T, ?> repository) {
        verify(repository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    public static <T> T nullableArg(Class<T> type) {
        return nullable(type);
    }
}
