/** Optional account Secrets was retired by the owner during the DigitalOcean migration. */
import type { YourLifeOneSecretDetail, YourLifeOneSecretSummary, SaveYourLifeOneSecretInput } from '../../types/domain';
function retired(): never { throw new Error('Account Secrets is no longer available.'); }
export async function listYourLifeOneSecrets(): Promise<{ accountVersion: number; secrets: YourLifeOneSecretSummary[] }> { return retired(); }
export const revealYourLifeOneSecret: (secretId: string) => Promise<YourLifeOneSecretDetail> = async () => retired();
export const saveYourLifeOneSecret: (requestId: string, input: SaveYourLifeOneSecretInput) => Promise<YourLifeOneSecretSummary> = async () => retired();
export const setYourLifeOneSecretArchived: (requestId: string, secretId: string, archived: boolean) => Promise<YourLifeOneSecretSummary> = async () => retired();
