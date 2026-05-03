import {
  AgentKit,
  CdpEvmWalletProvider,
  cdpEvmWalletActionProvider,
  cdpApiActionProvider,
  erc20ActionProvider,
  compoundActionProvider,
  wethActionProvider,
} from "@coinbase/agentkit";

export async function buildAgentKit(userWalletAddress: string): Promise<AgentKit> {
  if (!process.env.CDP_API_KEY_ID || !process.env.CDP_API_KEY_SECRET || !process.env.CDP_WALLET_SECRET) {
    throw new Error("Missing CDP credentials for AgentKit sidecar");
  }

  const walletProvider = await CdpEvmWalletProvider.configureWithWallet({
      apiKeyId: process.env.CDP_API_KEY_ID,
      apiKeySecret: process.env.CDP_API_KEY_SECRET,
      walletSecret: process.env.CDP_WALLET_SECRET,
      address: userWalletAddress as `0x${string}`,
      networkId: "base",
    });

  return AgentKit.from({
    walletProvider,
    actionProviders: [
      cdpApiActionProvider(),
      cdpEvmWalletActionProvider(),
      erc20ActionProvider(),
      compoundActionProvider(),
      wethActionProvider(),
    ],
  });
}
