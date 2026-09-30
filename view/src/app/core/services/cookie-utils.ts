const TOKEN_COOKIE = 'auth_token';
const ROLE_COOKIE = 'auth_role';

function secureFlag(): string {
  return typeof location !== 'undefined' && location.protocol === 'https:' ? '; Secure' : '';
}

export function setTokenCookie(token: string): void {
  document.cookie = `${TOKEN_COOKIE}=${encodeURIComponent(token)}; path=/; max-age=86400; SameSite=Lax${secureFlag()}`;
}

export function getTokenCookie(): string | null {
  const match = document.cookie.match(new RegExp(`(?:^|; )${TOKEN_COOKIE}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : null;
}

export function removeTokenCookie(): void {
  document.cookie = `${TOKEN_COOKIE}=; path=/; max-age=0; SameSite=Lax${secureFlag()}`;
}

export function setRoleCookie(rolId: number): void {
  document.cookie = `${ROLE_COOKIE}=${rolId}; path=/; max-age=86400; SameSite=Lax${secureFlag()}`;
}

export function getRoleCookie(): number | null {
  const match = document.cookie.match(new RegExp(`(?:^|; )${ROLE_COOKIE}=([^;]*)`));
  return match ? parseInt(match[1], 10) || null : null;
}

export function removeRoleCookie(): void {
  document.cookie = `${ROLE_COOKIE}=; path=/; max-age=0; SameSite=Lax${secureFlag()}`;
}
