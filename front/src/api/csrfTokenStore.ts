export type CsrfToken = { token: string; headerName: string };

let cachedCsrfToken: CsrfToken | undefined;

export async function getCsrfToken(fetchToken: () => Promise<CsrfToken>): Promise<CsrfToken> {
  if (!cachedCsrfToken) cachedCsrfToken = await fetchToken();
  return cachedCsrfToken;
}

export function clearCsrfToken(): void {
  cachedCsrfToken = undefined;
}
