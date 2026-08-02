import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { ApiError, fieldErrorMap } from '../../api/errors';
import { updateTimezone } from '../../api/user';
import { currentUserQueryKey } from '../auth/authQuery';
import { scheduleKeys } from '../schedule/queries';
import { supportedTimezones } from './timezone';

interface TimezoneFormProps {
  current: string;
}

export function TimezoneForm({ current }: TimezoneFormProps) {
  const [timezone, setTimezone] = useState(current);
  const [notice, setNotice] = useState<string>();
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn: () => updateTimezone(timezone),
    onSuccess: async (user) => {
      queryClient.setQueryData(currentUserQueryKey, user);
      await queryClient.invalidateQueries({ queryKey: scheduleKeys.all });
      setNotice('시간대를 변경했습니다. 일정의 UTC 값은 유지하고 표시 시간만 바뀝니다.');
    },
  });
  const fieldError = fieldErrorMap(mutation.error).timezone;
  const generalError =
    mutation.error instanceof ApiError && !fieldError ? mutation.error.message : undefined;

  return (
    <form
      className="timezone-form"
      onSubmit={(event) => {
        event.preventDefault();
        setNotice(undefined);
        mutation.mutate();
      }}
    >
      <label htmlFor="timezone">표시 시간대</label>
      <div className="timezone-controls">
        <input
          id="timezone"
          list="timezone-options"
          value={timezone}
          aria-invalid={Boolean(fieldError)}
          aria-describedby={fieldError ? 'timezone-error' : undefined}
          onChange={(event) => setTimezone(event.target.value)}
        />
        <datalist id="timezone-options">
          {supportedTimezones(current).map((zone) => (
            <option key={zone} value={zone} />
          ))}
        </datalist>
        <button
          type="submit"
          className="secondary-button"
          disabled={mutation.isPending || timezone.trim() === ''}
        >
          {mutation.isPending ? '변경 중…' : '변경'}
        </button>
      </div>
      {fieldError ? (
        <p id="timezone-error" className="field-error" role="alert">
          {fieldError}
        </p>
      ) : null}
      {generalError ? (
        <p className="field-error" role="alert">
          {generalError}
        </p>
      ) : null}
      {notice ? (
        <p className="success-notice" role="status">
          {notice}
        </p>
      ) : null}
    </form>
  );
}
