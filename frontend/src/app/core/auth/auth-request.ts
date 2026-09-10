const PUBLIC_AUTH_PATHS = ['/api/auth/login', '/api/auth/register', '/api/auth/refresh'];

export function isApiRequest(url: string): boolean {
  return url.startsWith('/api/');
}

export function isPublicAuthRequest(url: string): boolean {
  const path = url.split('?')[0];
  return PUBLIC_AUTH_PATHS.some((publicPath) => path.endsWith(publicPath));
}
