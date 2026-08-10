import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useLocation, useNavigate } from 'react-router';
import { getSocialProviders, login, socialLoginUrl } from '../api/auth';
import type { SocialProvider } from '../api/types';
import { ApiError, fieldErrorMap } from '../api/errors';
import { BrandLogo } from '../components/common/BrandLogo';
import { FormField } from '../components/common/FormField';
import {
  AssistantIcon,
  BellIcon,
  ChatIcon,
  GoogleIcon,
  KakaoIcon,
  NaverIcon,
} from '../components/common/Icons';
import { PasswordField } from '../components/common/PasswordField';
import { currentUserQueryOptions } from '../features/auth/authQuery';
import { DialogShell } from '../features/schedule/components/DialogShell';
import { loginSchema, type LoginFormValues } from '../features/auth/schemas';

const socialProviders: SocialProvider[] = [
  { id: 'google', name: 'Google' },
  { id: 'kakao', name: '카카오' },
  { id: 'naver', name: '네이버' },
];

export function LoginPage() {
  const [accountHelp, setAccountHelp] = useState<'email' | 'password'>();
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();
  const configuredSocialProviders = useQuery({
    queryKey: ['auth', 'social-providers'],
    queryFn: ({ signal }) => getSocialProviders(signal),
    staleTime: 5 * 60 * 1000,
  });
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
  const socialError = new URLSearchParams(location.search).get('social');
  const socialErrorMessage =
    socialError === 'email'
      ? '소셜 계정에서 확인된 이메일을 제공해야 로그인할 수 있어요.'
      : socialError === 'failed'
        ? '간편 로그인에 실패했어요. 잠시 후 다시 시도해 주세요.'
        : undefined;

  return (
    <main className="auth-page">
      <section className="auth-intro">
        <div className="auth-brand">
          <BrandLogo />
          <span className="brand-wordmark">
            Cal<strong>Talk</strong>
          </span>
        </div>
        <div className="auth-message">
          <p className="eyebrow">시간을 더 선명하게</p>
          <h1>
            복잡한 일정을
            <br />
            한눈에 정리하세요
          </h1>
          <p>시간대와 일정 충돌을 저장 전에 정확히 확인해 드려요.</p>
        </div>
        <ul className="auth-benefits" aria-label="CalTalk 주요 기능">
          <li>
            <ChatIcon />
            <span>
              <strong>카톡 하나로 일정 관리</strong>
              <p>카카오톡에서 일정을 바로 확인하고 관리하세요.</p>
            </span>
          </li>
          <li>
            <AssistantIcon />
            <span>
              <strong>내 일정을 이해하는 비서</strong>
              <p>복잡한 메뉴 없이 필요한 일정 관리를 도와드려요.</p>
            </span>
          </li>
          <li>
            <BellIcon />
            <span>
              <strong>중요한 순간을 먼저 챙겨드려요</strong>
              <p>잊기 쉬운 일정도 필요한 순간에 알려드려요.</p>
            </span>
          </li>
        </ul>
      </section>
      <section className="card auth-card" aria-labelledby="login-title">
        <p className="auth-card-kicker">오늘도 계획대로</p>
        <h2 id="login-title">로그인</h2>
        <p className="muted">내 일정을 확인하고 오늘을 계획해 보세요.</p>
        {errorMessage || socialErrorMessage ? (
          <div className="alert" role="alert">
            {errorMessage ?? socialErrorMessage}
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
        <section className="social-login" aria-label="간편 로그인">
          <div className="social-login-divider">
            <span>또는 간편 로그인</span>
          </div>
          <div className="social-login-buttons">
            {socialProviders.map((provider) => {
              const isConfigured = configuredSocialProviders.data?.some(
                ({ id }) => id === provider.id,
              );
              const content = (
                <>
                  <span className="social-login-icon" aria-hidden="true">
                    {provider.id === 'kakao' ? (
                      <KakaoIcon />
                    ) : provider.id === 'naver' ? (
                      <NaverIcon />
                    ) : (
                      <GoogleIcon />
                    )}
                  </span>
                  <span>
                    {provider.name}로 계속하기
                    {!isConfigured ? <small>준비 중</small> : null}
                  </span>
                </>
              );

              return isConfigured ? (
                <a
                  key={provider.id}
                  className={`social-login-button social-login-${provider.id}`}
                  href={socialLoginUrl(provider.id)}
                  aria-label={`${provider.name}로 로그인`}
                >
                  {content}
                </a>
              ) : (
                <span
                  key={provider.id}
                  className={`social-login-button social-login-${provider.id} is-disabled`}
                  aria-disabled="true"
                >
                  {content}
                </span>
              );
            })}
          </div>
        </section>
        <div className="auth-account-actions">
          <p className="auth-link">
            <span>CalTalk이 처음이신가요?</span>
            <Link to="/signup">새 계정 만들기</Link>
          </p>
          <nav className="auth-recovery" aria-label="계정 찾기">
            <span className="auth-recovery-label">로그인이 어려우신가요?</span>
            <span className="auth-recovery-links">
              <button type="button" onClick={() => setAccountHelp('email')}>
                이메일 찾기
              </button>
              <span className="auth-recovery-separator" aria-hidden="true" />
              <button type="button" onClick={() => setAccountHelp('password')}>
                비밀번호 찾기
              </button>
            </span>
          </nav>
        </div>
        <p className="auth-mobile-footer">오늘의 일정을 CalTalk과 가볍게 시작하세요.</p>
      </section>
      {accountHelp ? (
        <DialogShell
          title={accountHelp === 'email' ? '이메일을 잊으셨나요?' : '비밀번호를 잊으셨나요?'}
          description="CalTalk 계정 이용에 필요한 내용을 안내합니다."
          onClose={() => setAccountHelp(undefined)}
        >
          <div className="account-help-content">
            {accountHelp === 'email' ? (
              <>
                <strong>CalTalk 아이디는 가입할 때 사용한 이메일입니다.</strong>
                <p>평소 사용하는 이메일 주소를 확인한 뒤 다시 로그인해 주세요.</p>
              </>
            ) : (
              <>
                <strong>비밀번호 재설정 기능을 준비하고 있습니다.</strong>
                <p>현재는 새 계정을 만들거나, 기억나는 비밀번호로 다시 시도해 주세요.</p>
              </>
            )}
          </div>
          <div className="dialog-actions">
            <button
              type="button"
              className="primary-button compact-button"
              onClick={() => setAccountHelp(undefined)}
            >
              확인
            </button>
          </div>
        </DialogShell>
      ) : null}
    </main>
  );
}
