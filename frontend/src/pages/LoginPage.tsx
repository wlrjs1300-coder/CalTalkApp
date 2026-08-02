import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useLocation, useNavigate } from 'react-router';
import { login } from '../api/auth';
import { ApiError, fieldErrorMap } from '../api/errors';
import { FormField } from '../components/common/FormField';
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
        <p className="eyebrow">CalTalk</p>
        <h1>일정을 정리하는 첫 화면</h1>
        <p>로그인하면 서버 세션에서 인증 상태를 복원합니다.</p>
      </section>
      <section className="card auth-card" aria-labelledby="login-title">
        <h2 id="login-title">로그인</h2>
        <p className="muted">가입한 이메일로 계속하세요.</p>
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
            autoComplete="email"
            error={form.formState.errors.email?.message}
            {...form.register('email')}
          />
          <FormField
            id="password"
            label="비밀번호"
            type="password"
            autoComplete="current-password"
            error={form.formState.errors.password?.message}
            {...form.register('password')}
          />
          <button className="primary-button" type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? '로그인 중…' : '로그인'}
          </button>
        </form>
        <p className="auth-link">
          계정이 없나요? <Link to="/signup">회원가입</Link>
        </p>
      </section>
    </main>
  );
}
