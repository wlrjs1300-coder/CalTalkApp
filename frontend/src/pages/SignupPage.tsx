import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router';
import { signup } from '../api/auth';
import { ApiError, fieldErrorMap } from '../api/errors';
import { FormField } from '../components/common/FormField';
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
        <p className="eyebrow">CalTalk</p>
        <h1>일정 관리 시작하기</h1>
        <p>가입 후 로그인하면 기본 시간대 Asia/Seoul로 시작합니다.</p>
      </section>
      <section className="card auth-card" aria-labelledby="signup-title">
        <h2 id="signup-title">회원가입</h2>
        <p className="muted">이메일과 비밀번호만 입력합니다.</p>
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
            autoComplete="new-password"
            error={form.formState.errors.password?.message}
            {...form.register('password')}
          />
          <FormField
            id="passwordConfirmation"
            label="비밀번호 확인"
            type="password"
            autoComplete="new-password"
            error={form.formState.errors.passwordConfirmation?.message}
            {...form.register('passwordConfirmation')}
          />
          <button className="primary-button" type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? '가입 중…' : '회원가입'}
          </button>
        </form>
        <p className="auth-link">
          이미 계정이 있나요? <Link to="/login">로그인</Link>
        </p>
      </section>
    </main>
  );
}
