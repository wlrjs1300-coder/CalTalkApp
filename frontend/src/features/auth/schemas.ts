import { z } from 'zod';

const email = z
  .string()
  .trim()
  .min(1, '이메일을 입력해 주세요.')
  .max(254, '이메일은 254자 이하여야 합니다.')
  .email('올바른 이메일 형식으로 입력해 주세요.');

const password = z
  .string()
  .min(1, '비밀번호를 입력해 주세요.')
  .min(8, '비밀번호는 8자 이상이어야 합니다.')
  .max(64, '비밀번호는 64자 이하여야 합니다.')
  .refine((value) => value.trim().length > 0, '비밀번호는 공백만 입력할 수 없습니다.');

export const loginSchema = z.object({ email, password });

export const signupSchema = z
  .object({
    email,
    password,
    passwordConfirmation: password,
  })
  .refine((value) => value.password === value.passwordConfirmation, {
    path: ['passwordConfirmation'],
    message: '비밀번호 확인이 일치하지 않습니다.',
  });

export type LoginFormValues = z.infer<typeof loginSchema>;
export type SignupFormValues = z.infer<typeof signupSchema>;
