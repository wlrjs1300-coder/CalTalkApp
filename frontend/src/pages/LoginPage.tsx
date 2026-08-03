import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useLocation, useNavigate } from 'react-router';
import { login } from '../api/auth';
import { ApiError, fieldErrorMap } from '../api/errors';
import { FormField } from '../components/common/FormField';
import { CalendarIcon, ClockIcon, WarningIcon } from '../components/common/Icons';
import { PasswordField } from '../components/common/PasswordField';
import { currentUserQueryOptions } from '../features/auth/authQuery';
import { loginSchema, type LoginFormValues } from '../features/auth/schemas';

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();
  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  });

  const mutation = useMutation({
    mutationFn: (values: LoginFormValues) => login(values),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: currentUserQueryOptions.queryKey });
      await queryClient.fetchQuery(currentUserQueryOptions);
      const destination =
        typeof location.state === 'object' &&
        location.state !== null &&
        'from' in location.state &&
        typeof location.state.from === 'string' &&
        location.state.from.startsWith('/')
          ? location.state.from
          : '/';
      navigate(destination, { replace: true });
    },
  });

  useEffect(() => {
    if (!mutation.error) return;
    for (const [field, message] of Object.entries(fieldErrorMap(mutation.error))) {
      if (field === 'email' || field === 'password') {
        form.setError(field, { type: 'server', message });
      }
    }
  }, [form, mutation.error]);

  const errorMessage =
    mutation.error instanceof ApiError
      ? mutation.error.code === 'INVALID_CREDENTIALS'
        ? '이메일 또는 비밀번호를 확인해 주세요.'
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
          <p className="eyebrow">시간을 더 선명하게</p>
          <h1>
            복잡한 일정을
            <br />
            한눈에 정리하세요
          </h1>
          <p>시간대가 달라도 정확하게, 겹치는 일정은 저장 전에 한 번 더 확인해 드려요.</p>
        </div>
        <ul className="auth-benefits" aria-label="CalTalk 주요 기능">
          <li>
            <ClockIcon />
            <span>
              <strong>정확한 시간 관리</strong>내 시간대에 맞춰 일정을 확인하세요.
            </span>
          </li>
          <li>
            <WarningIcon />
            <span>
              <strong>겹침 사전 확인</strong>놓치기 쉬운 일정 충돌을 알려드려요.
            </span>
          </li>
        </ul>
      </section>
      <section className="card auth-card" aria-labelledby="login-title">
        <p className="auth-card-kicker">다시 만나 반가워요</p>
        <h2 id="login-title">로그인</h2>
        <p className="muted">내 일정을 확인하고 오늘을 계획해 보세요.</p>
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
            placeholder="비밀번호를 입력하세요"
            autoComplete="current-password"
            error={form.formState.errors.password?.message}
            {...form.register('password')}
          />
          <button
            className="primary-button auth-submit"
            type="submit"
            disabled={mutation.isPending}
            aria-busy={mutation.isPending}
          >
            {mutation.isPending ? '로그인 중…' : '로그인'}
          </button>
        </form>
        <p className="auth-link">
          CalTalk이 처음인가요? <Link to="/signup">새 계정 만들기</Link>
        </p>
      </section>
    </main>
  );
}
