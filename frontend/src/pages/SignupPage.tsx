import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router';
import { signup } from '../api/auth';
import { ApiError, fieldErrorMap } from '../api/errors';
import { FormField } from '../components/common/FormField';
import { CalendarIcon, ClockIcon, WarningIcon } from '../components/common/Icons';
import { PasswordField } from '../components/common/PasswordField';
import { signupSchema, type SignupFormValues } from '../features/auth/schemas';

export function SignupPage() {
  const navigate = useNavigate();
  const form = useForm<SignupFormValues>({
    resolver: zodResolver(signupSchema),
    defaultValues: { email: '', password: '', passwordConfirmation: '' },
  });
  const mutation = useMutation({
    mutationFn: (values: SignupFormValues) => signup(values),
    onSuccess: (response) => {
      navigate('/login', { replace: true, state: { signupEmail: response.email } });
    },
  });

  useEffect(() => {
    if (!mutation.error) return;
    for (const [field, message] of Object.entries(fieldErrorMap(mutation.error))) {
      if (field === 'email' || field === 'password' || field === 'passwordConfirmation') {
        form.setError(field, { type: 'server', message });
      }
    }
  }, [form, mutation.error]);

  const errorMessage =
    mutation.error instanceof ApiError
      ? mutation.error.code === 'DUPLICATE_EMAIL'
        ? '이미 가입된 이메일입니다.'
        : mutation.error.message
      : undefined;

  return (
    <main className="auth-page">
      <section className="auth-intro">
        <div className="auth-brand">
          <span className="brand-mark">
            <CalendarIcon />
          </span>
          <span>CalTalk</span>
        </div>
        <div className="auth-message">
          <p className="eyebrow">차분하게 시작하는 하루</p>
          <h1>
            중요한 시간을
            <br />
            놓치지 않도록
          </h1>
          <p>일정을 모아 보고, 겹치는 시간을 미리 확인하는 간결한 캘린더를 시작하세요.</p>
        </div>
        <ul className="auth-benefits" aria-label="가입 후 이용할 수 있는 기능">
          <li>
            <ClockIcon />
            <span>
              <strong>내 시간대에 맞게</strong>처음에는 서울 시간으로 시작해요.
            </span>
          </li>
          <li>
            <WarningIcon />
            <span>
              <strong>저장 전에 한 번 더</strong>겹치는 일정은 바로 알려드려요.
            </span>
          </li>
        </ul>
      </section>
      <section className="card auth-card" aria-labelledby="signup-title">
        <p className="auth-card-kicker">가볍게 시작해 보세요</p>
        <h2 id="signup-title">회원가입</h2>
        <p className="muted">두 가지 정보만 입력하면 바로 시작할 수 있어요.</p>
        {errorMessage ? (
          <div className="alert" role="alert">
            {errorMessage}
          </div>
        ) : null}
        <form onSubmit={form.handleSubmit((values) => mutation.mutate(values))} noValidate>
          <FormField
            id="email"
            label="이메일"
            type="email"
            placeholder="name@example.com"
            autoComplete="email"
            error={form.formState.errors.email?.message}
            {...form.register('email')}
          />
          <PasswordField
            id="password"
            label="비밀번호"
            placeholder="8자 이상 입력하세요"
            hint="8~64자로 입력해 주세요."
            autoComplete="new-password"
            error={form.formState.errors.password?.message}
            {...form.register('password')}
          />
          <PasswordField
            id="passwordConfirmation"
            label="비밀번호 확인"
            placeholder="비밀번호를 한 번 더 입력하세요"
            autoComplete="new-password"
            error={form.formState.errors.passwordConfirmation?.message}
            {...form.register('passwordConfirmation')}
          />
          <div className="timezone-preview" role="note">
            <ClockIcon />
            <span>
              <strong>기본 표시 시간대</strong>아시아 · 서울 (로그인 후 설정에서 변경 가능)
            </span>
          </div>
          <button
            className="primary-button auth-submit"
            type="submit"
            disabled={mutation.isPending}
            aria-busy={mutation.isPending}
          >
            {mutation.isPending ? '가입 중…' : '회원가입'}
          </button>
        </form>
        <p className="auth-link">
          이미 계정이 있나요? <Link to="/login">로그인하기</Link>
        </p>
      </section>
    </main>
  );
}
