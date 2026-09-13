package com.inspiredandroid.kai.ninerouter

import kotlinx.serialization.Serializable

@Serializable
data class NineProviderMeta(
  val id: String,
  val alias: String,
  val aliases: List<String> = emptyList(),
  val baseUrl: String = "",
  val validateUrl: String = "",
  val category: String = "apikey",
  val needsAccountId: Boolean = false,
  val format: String = "openai",
  val displayName: String = "",
  val isCustom: Boolean = false,
)

object NineRouterRegistry {
  val allBuiltIn: List<NineProviderMeta> = listOf(
    NineProviderMeta("a6api","a6api",listOf(),"https://a6api.com/v1/chat/completions","https://a6api.com/v1/models","apikey",false,"openai"),
    NineProviderMeta("agentrouter","agentrouter",listOf(),"https://agentrouter.org/v1/messages","","freeTier",false,"claude"),
    NineProviderMeta("alibaba","ali",listOf(),"https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions","https://dashscope-intl.aliyuncs.com/compatible-mode/v1/models","apikey",false,"openai"),
    NineProviderMeta("alicode-intl","alicode-intl",listOf(),"https://coding-intl.dashscope.aliyuncs.com/v1/chat/completions","","apikey",false,"openai"),
    NineProviderMeta("alicode","alicode",listOf(),"https://coding.dashscope.aliyuncs.com/v1/chat/completions","","apikey",false,"openai"),
    NineProviderMeta("alims-intl","alims-intl",listOf(),"https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions","","apikey",false,"openai"),
    NineProviderMeta("alitp-intl","alitp-intl",listOf(),"https://token-plan.ap-southeast-1.maas.aliyuncs.com/compatible-mode/v1/chat/completions","","apikey",false,"openai"),
    NineProviderMeta("anthropic","anthropic",listOf(),"https://api.anthropic.com/v1/messages","","apikey",false,"claude"),
    NineProviderMeta("antigravity","ag",listOf(),"","","oauth",false,"antigravity"),
    NineProviderMeta("api-airforce","af",listOf("airforce"),"https://api.airforce/v1/chat/completions","https://api.airforce/v1/models","freeTier",false,"openai"),
    NineProviderMeta("assemblyai","assemblyai",listOf("aai"),"https://api.assemblyai.com/v1/audio/transcriptions","https://api.assemblyai.com/v1/account","apikey",false,"assemblyai"),
    NineProviderMeta("aws-polly","polly",listOf(),"","","apikey",false,"aws-polly"),
    NineProviderMeta("azure","azure",listOf(),"","","apikey",false,"openai"),
    NineProviderMeta("baidu","qianfan",listOf("qianfan", "ernie", "baidu-qianfan"),"https://qianfan.baidubce.com/v2/chat/completions","https://qianfan.baidubce.com/v2/models","apikey",false,"openai"),
    NineProviderMeta("baseten","baseten",listOf(),"https://inference.baseten.co/v1/chat/completions","https://inference.baseten.co/v1/models","apikey",false,"openai"),
    NineProviderMeta("bazaarlink","bzl",listOf("bazaar-link"),"https://bazaarlink.ai/api/v1/chat/completions","https://bazaarlink.ai/api/v1/models","freeTier",false,"openai"),
    NineProviderMeta("black-forest-labs","black-forest-labs",listOf("bfl"),"","","apikey",false,"openai"),
    NineProviderMeta("blackbox","blackbox",listOf("bb"),"https://api.blackbox.ai/v1/chat/completions","","apikey",false,"openai"),
    NineProviderMeta("bluesminds","bm",listOf("blue-sminds"),"https://api.bluesminds.com/v1/chat/completions","https://api.bluesminds.com/v1/models","apikey",false,"openai"),
    NineProviderMeta("brave-search","brave",listOf(),"","","apikey",false,"openai"),
    NineProviderMeta("byteplus","byteplus",listOf("bpm"),"https://ark.ap-southeast.bytepluses.com/api/coding/v3/chat/completions","","freeTier",false,"openai"),
    NineProviderMeta("bytez","bytez",listOf(),"https://api.bytez.com/models/v2/openai/v1/chat/completions","https://api.bytez.com/models/v2/openai/v1/models","apikey",false,"openai"),
    NineProviderMeta("cartesia","cartesia",listOf(),"","","apikey",false,"cartesia"),
    NineProviderMeta("cerebras","cerebras",listOf(),"https://api.cerebras.ai/v1/chat/completions","https://api.cerebras.ai/v1/models","apikey",false,"openai"),
    NineProviderMeta("chutes","chutes",listOf("ch"),"https://llm.chutes.ai/v1/chat/completions","https://llm.chutes.ai/v1/models","apikey",false,"openai"),
    NineProviderMeta("claude","cc",listOf(),"https://api.anthropic.com/v1/messages","","oauth",false,"claude"),
    NineProviderMeta("cline","cl",listOf(),"https://api.cline.bot/api/v1/chat/completions","","oauth",false,"openai"),
    NineProviderMeta("clinepass","clinepass",listOf(),"https://api.cline.bot/api/v1/chat/completions","","oauth",false,"openai"),
    NineProviderMeta("cloudflare-ai","cloudflare-ai",listOf("cf"),"https://api.cloudflare.com/client/v4/accounts/{accountId}/ai/v1/chat/completions","","freeTier",true,"openai"),
    NineProviderMeta("codebuddy-cn","cbcn",listOf(),"https://copilot.tencent.com/v2/chat/completions","","oauth",false,"openai"),
    NineProviderMeta("codebuddy-intl","cbai",listOf(),"https://www.codebuddy.ai/v2/chat/completions","","oauth",false,"openai"),
    NineProviderMeta("codestral","codestral",listOf(),"https://codestral.mistral.ai/v1/chat/completions","https://codestral.mistral.ai/v1/models","apikey",false,"openai"),
    NineProviderMeta("codex","cx",listOf(),"https://chatgpt.com/backend-api/codex/responses","","oauth",false,"openai-responses"),
    NineProviderMeta("cohere","cohere",listOf(),"https://api.cohere.ai/v1/chat/completions","https://api.cohere.ai/v1/models","apikey",false,"openai"),
    NineProviderMeta("comfyui","comfyui",listOf(),"","","apikey",false,"openai"),
    NineProviderMeta("commandcode","commandcode",listOf("cmc"),"https://api.commandcode.ai/alpha/generate","","apikey",false,"commandcode"),
    NineProviderMeta("coqui","coqui",listOf(),"","","freeTier",false,"coqui"),
    NineProviderMeta("cursor","cu",listOf(),"https://api2.cursor.sh","","oauth",false,"cursor"),
    NineProviderMeta("databricks","databricks",listOf(),"https://adb-0000000000000000.0.azuredatabricks.net/serving-endpoints","https://adb-0000000000000000.0.azuredatabricks.net/serving-endpoints","apikey",false,"openai"),
    NineProviderMeta("deepgram","deepgram",listOf("dg"),"https://api.deepgram.com/v1/listen","","apikey",false,"deepgram"),
    NineProviderMeta("deepinfra","deepinfra",listOf(),"https://api.deepinfra.com/v1/openai/chat/completions","https://api.deepinfra.com/v1/openai/models","apikey",false,"openai"),
    NineProviderMeta("deepseek","deepseek",listOf("ds"),"https://api.deepseek.com/chat/completions","https://api.deepseek.com/models","apikey",false,"openai"),
    NineProviderMeta("devin-cli","dv",listOf("devin"),"devin://acp/stdio","","free",false,"openai"),
    NineProviderMeta("edge-tts","edge-tts",listOf(),"","","freeTier",false,"edge-tts"),
    NineProviderMeta("elevenlabs","el",listOf(),"","","apikey",false,"elevenlabs"),
    NineProviderMeta("exa","exa",listOf(),"","","apikey",false,"openai"),
    NineProviderMeta("fal-ai","fal-ai",listOf("fal"),"","","apikey",false,"openai"),
    NineProviderMeta("featherless","featherless",listOf("fl"),"https://api.featherless.ai/v1/chat/completions","https://api.featherless.ai/v1/models","apikey",false,"openai"),
    NineProviderMeta("firecrawl","firecrawl",listOf(),"","","apikey",false,"openai"),
    NineProviderMeta("fireworks","fireworks",listOf(),"https://api.fireworks.ai/inference/v1/chat/completions","https://api.fireworks.ai/inference/v1/models","apikey",false,"openai"),
    NineProviderMeta("fish-audio","fish",listOf(),"","","apikey",false,"fish-audio"),
    NineProviderMeta("freebuff","fb",listOf(),"https://www.codebuff.com/api/v1/chat/completions","","free",false,"openai"),
    NineProviderMeta("gemini-cli","gc",listOf(),"https://cloudcode-pa.googleapis.com/v1internal","","free",false,"gemini-cli"),
    NineProviderMeta("gemini","gemini",listOf(),"https://generativelanguage.googleapis.com/v1beta/models","","freeTier",false,"gemini"),
    NineProviderMeta("github","gh",listOf(),"https://api.githubcopilot.com/chat/completions","","oauth",false,"openai"),
    NineProviderMeta("gitlab","gitlab",listOf(),"https://gitlab.com/api/v4/chat/completions","","oauth",false,"openai"),
    NineProviderMeta("glm-cn","glm-cn",listOf(),"https://open.bigmodel.cn/api/coding/paas/v4/chat/completions","","apikey",false,"openai"),
    NineProviderMeta("glm","glm",listOf(),"https://api.z.ai/api/anthropic/v1/messages","","apikey",false,"claude"),
    NineProviderMeta("google-pse","gpse",listOf(),"","","apikey",false,"openai"),
    NineProviderMeta("google-tts","google-tts",listOf(),"","","freeTier",false,"google-tts"),
    NineProviderMeta("grok-cli","gcli",listOf("grok-build", "gb"),"","","oauth",false,"openai-responses"),
    NineProviderMeta("grok-web","grok-web",listOf("gw"),"https://grok.com/rest/app-chat/conversations/new","","webCookie",false,"grok-web"),
    NineProviderMeta("groq","groq",listOf(),"https://api.groq.com/openai/v1/chat/completions","https://api.groq.com/openai/v1/models","apikey",false,"openai"),
    NineProviderMeta("heroku","heroku",listOf(),"https://us.inference.heroku.com/v1/chat/completions","https://us.inference.heroku.com/v1/models","apikey",false,"openai"),
    NineProviderMeta("huggingface","huggingface",listOf("hf"),"","","apikey",false,"openai"),
    NineProviderMeta("hyperbolic","hyperbolic",listOf("hyp"),"https://api.hyperbolic.xyz/v1/chat/completions","https://api.hyperbolic.xyz/v1/models","apikey",false,"openai"),
    NineProviderMeta("iflow","if",listOf(),"https://apis.iflow.cn/v1/chat/completions","","oauth",false,"openai"),
    NineProviderMeta("inworld","inworld",listOf(),"","","apikey",false,"inworld"),
    NineProviderMeta("jina-ai","jina",listOf(),"","","apikey",false,"openai"),
    NineProviderMeta("jina-reader","jina-reader",listOf(),"","","apikey",false,"openai"),
    NineProviderMeta("kilo-gateway","kgw",listOf("kilo-gateway", "kilogateway"),"https://api.kilo.ai/api/gateway/chat/completions","https://api.kilo.ai/api/gateway/models","freeTier",false,"openai"),
    NineProviderMeta("kilocode","kc",listOf(),"https://api.kilo.ai/api/openrouter/chat/completions","","oauth",false,"openai"),
    NineProviderMeta("kimchi","kimchi",listOf(),"https://llm.kimchi.dev/openai/v1/chat/completions","","freeTier",false,"openai"),
    NineProviderMeta("kimi","kimi",listOf("kimi-coding", "kmc"),"https://api.kimi.com/coding/v1/messages","","oauth",false,"claude"),
    NineProviderMeta("kiro","kr",listOf(),"https://runtime.us-east-1.kiro.dev/generateAssistantResponse","","free",false,"kiro"),
    NineProviderMeta("linkup","linkup",listOf(),"","","apikey",false,"openai"),
    NineProviderMeta("llm7","llm7",listOf("llm-7"),"https://api.llm7.io/v1/chat/completions","https://api.llm7.io/v1/models","apikey",false,"openai"),
    NineProviderMeta("local-device","local-device",listOf(),"","","freeTier",false,"local-device"),
    NineProviderMeta("mimo-free","mmf",listOf(),"https://api.xiaomimimo.com/api/free-ai/openai/chat","","free",false,"openai"),
    NineProviderMeta("minimax-cn","minimax-cn",listOf(),"https://api.minimaxi.com/anthropic/v1/messages","","apikey",false,"claude"),
    NineProviderMeta("minimax","minimax",listOf(),"https://api.minimax.io/anthropic/v1/messages","","apikey",false,"claude"),
    NineProviderMeta("mistral","mistral",listOf(),"https://api.mistral.ai/v1/chat/completions","https://api.mistral.ai/v1/models","apikey",false,"openai"),
    NineProviderMeta("mmf","mmf",listOf(),"https://api.xiaomimimo.com/api/free-ai/openai/chat","","apikey",false,"openai"),
    NineProviderMeta("morph","morph",listOf("morphllm"),"https://api.morphllm.com/v1/chat/completions","https://api.morphllm.com/v1/models","apikey",false,"openai"),
    NineProviderMeta("nanobanana","nanobanana",listOf("nb"),"https://api.nanobananaapi.ai/v1/chat/completions","https://api.nanobananaapi.ai/v1/models","apikey",false,"openai"),
    NineProviderMeta("nanogpt","nanogpt",listOf(),"https://nano-gpt.com/api/v1/chat/completions","https://nano-gpt.com/api/v1/models","apikey",false,"openai"),
    NineProviderMeta("nebius","nebius",listOf(),"https://api.studio.nebius.ai/v1/chat/completions","https://api.studio.nebius.ai/v1/models","apikey",false,"openai"),
    NineProviderMeta("nscale","nscale",listOf(),"https://inference.api.nscale.com/v1/chat/completions","https://inference.api.nscale.com/v1/models","apikey",false,"openai"),
    NineProviderMeta("nvidia","nvidia",listOf(),"https://integrate.api.nvidia.com/v1/chat/completions","https://integrate.api.nvidia.com/v1/models","freeTier",false,"nvidia-tts"),
    NineProviderMeta("ollama-local","ollama-local",listOf(),"http://localhost:11434/api/chat","","apikey",false,"ollama"),
    NineProviderMeta("ollama-search","ollama-search",listOf(),"","","apikey",false,"openai"),
    NineProviderMeta("ollama","ollama",listOf(),"https://ollama.com/api/chat","https://ollama.com/api/tags","freeTier",false,"ollama"),
    NineProviderMeta("openai","openai",listOf(),"https://api.openai.com/v1/chat/completions","","apikey",false,"openai"),
    NineProviderMeta("opencode-go","opencode-go",listOf("ocg"),"https://opencode.ai/zen/go/v1/chat/completions","","apikey",false,"openai"),
    NineProviderMeta("opencode","oc",listOf(),"https://opencode.ai","","free",false,"openai"),
    NineProviderMeta("openrouter","openrouter",listOf(),"https://openrouter.ai/api/v1/chat/completions","","freeTier",false,"openai"),
    NineProviderMeta("ovhcloud","ovh",listOf(),"https://oai.endpoints.kepler.ai.cloud.ovh.net/v1/chat/completions","https://oai.endpoints.kepler.ai.cloud.ovh.net/v1/models","apikey",false,"openai"),
    NineProviderMeta("perplexity-agent","perplexity-agent",listOf("pplx-agent", "pplx-responses"),"https://api.perplexity.ai/v1/responses","https://api.perplexity.ai/v1/models","apikey",false,"openai-responses"),
    NineProviderMeta("perplexity-web","perplexity-web",listOf("pw"),"https://www.perplexity.ai/rest/sse/perplexity_ask","","webCookie",false,"perplexity-web"),
    NineProviderMeta("perplexity","perplexity",listOf("pplx"),"https://api.perplexity.ai/chat/completions","https://api.perplexity.ai/models","apikey",false,"openai"),
    NineProviderMeta("playht","playht",listOf(),"","","apikey",false,"playht"),
    NineProviderMeta("poolside","poolside",listOf("ps"),"https://inference.poolside.ai/v1/chat/completions","https://inference.poolside.ai/v1/models","freeTier",false,"openai"),
    NineProviderMeta("predibase","predibase",listOf(),"https://serving.app.predibase.com/v1/chat/completions","https://serving.app.predibase.com/v1/models","apikey",false,"openai"),
    NineProviderMeta("qoder","qd",listOf(),"https://api3.qoder.sh/algo/api/v2/service/pro/sse/agent_chat_generation","","oauth",false,"openai"),
    NineProviderMeta("qwen","qw",listOf(),"https://portal.qwen.ai/v1/chat/completions","","oauth",false,"openai"),
    NineProviderMeta("recraft","recraft",listOf(),"","","apikey",false,"openai"),
    NineProviderMeta("runwayml","runwayml",listOf("runway"),"","","apikey",false,"openai"),
    NineProviderMeta("sambanova","samba",listOf("sambanova-ai"),"https://api.sambanova.ai/v1/chat/completions","https://api.sambanova.ai/v1/models","apikey",false,"openai"),
    NineProviderMeta("sdwebui","sdwebui",listOf(),"","","apikey",false,"openai"),
    NineProviderMeta("searchapi","searchapi",listOf(),"","","apikey",false,"openai"),
    NineProviderMeta("searxng","searxng",listOf(),"","","freeTier",false,"openai"),
    NineProviderMeta("selfhosted-embedding","selfhosted-embedding",listOf(),"","","apikey",false,"openai"),
    NineProviderMeta("selfhosted-stt","selfhosted-stt",listOf(),"","","apikey",false,"openai"),
    NineProviderMeta("selfhosted-tts","selfhosted-tts",listOf(),"","","apikey",false,"openai-speech"),
    NineProviderMeta("serper","serper",listOf(),"","","apikey",false,"openai"),
    NineProviderMeta("siliconflow","siliconflow",listOf(),"https://api.siliconflow.com/v1/chat/completions","https://api.siliconflow.com/v1/models","apikey",false,"openai"),
    NineProviderMeta("stability-ai","stability-ai",listOf("stability"),"","","apikey",false,"openai"),
    NineProviderMeta("tavily","tavily",listOf(),"","","apikey",false,"openai"),
    NineProviderMeta("tencent","hunyuan",listOf("hunyuan", "tencent-hunyuan"),"https://api.hunyuan.cloud.tencent.com/v1/chat/completions","https://api.hunyuan.cloud.tencent.com/v1/models","apikey",false,"openai"),
    NineProviderMeta("together","together",listOf(),"https://api.together.xyz/v1/chat/completions","https://api.together.xyz/v1/models","apikey",false,"openai"),
    NineProviderMeta("tokenrouter","tokenrouter",listOf("tr"),"https://api.tokenrouter.com/v1/chat/completions","https://api.tokenrouter.com/v1/models","apikey",false,"openai"),
    NineProviderMeta("topaz","topaz",listOf(),"","","apikey",false,"openai"),
    NineProviderMeta("tortoise","tortoise",listOf(),"","","freeTier",false,"tortoise"),
    NineProviderMeta("trae","tr",listOf("marscode"),"https://core-normal.trae.ai/api/remote/v1","","oauth",false,"openai"),
    NineProviderMeta("upstage","upstage",listOf(),"https://api.upstage.ai/v1/chat/completions","https://api.upstage.ai/v1/models","apikey",false,"openai"),
    NineProviderMeta("venice","venice",listOf("vn"),"https://api.venice.ai/api/v1/chat/completions","https://api.venice.ai/api/v1/models","apikey",false,"openai"),
    NineProviderMeta("vercel-ai-gateway","vercel-ai-gateway",listOf("vercel"),"https://ai-gateway.vercel.sh/v1/chat/completions","","apikey",false,"openai"),
    NineProviderMeta("vertex-partner","vertex-partner",listOf("vxp"),"https://aiplatform.googleapis.com","","apikey",false,"openai"),
    NineProviderMeta("vertex","vertex",listOf("vx"),"https://aiplatform.googleapis.com","","freeTier",false,"vertex"),
    NineProviderMeta("volcengine-ark","volcengine-ark",listOf("ark"),"https://ark.cn-beijing.volces.com/api/coding/v3/chat/completions","","apikey",false,"openai"),
    NineProviderMeta("volcengine","volcengine",listOf(),"https://ark.cn-beijing.volces.com/api/v3/chat/completions","https://ark.cn-beijing.volces.com/api/v3/models","apikey",false,"openai"),
    NineProviderMeta("voyage-ai","voyage-ai",listOf(),"","","apikey",false,"openai"),
    NineProviderMeta("wandb","wandb",listOf(),"https://api.inference.wandb.ai/v1/chat/completions","https://api.inference.wandb.ai/v1/models","apikey",false,"openai"),
    NineProviderMeta("windsurf","ws",listOf(),"https://server.codeium.com/exa.language_server_pb.LanguageServerService/GetChatMessage","","oauth",false,"openai"),
    NineProviderMeta("xai","xai",listOf(),"https://api.x.ai/v1/chat/completions","https://api.x.ai/v1/models","oauth",false,"openai"),
    NineProviderMeta("xiaomi-mimo","xiaomi-mimo",listOf("mimo"),"https://api.xiaomimimo.com/v1/chat/completions","https://api.xiaomimimo.com/v1/models","apikey",false,"openai"),
    NineProviderMeta("xiaomi-tokenplan","xiaomi-tokenplan",listOf("xmtp"),"https://token-plan-sgp.xiaomimimo.com/v1/chat/completions","","apikey",false,"openai"),
    NineProviderMeta("xquik","xquik",listOf(),"","https://xquik.com/api/v1/credits","apikey",false,"openai"),
    NineProviderMeta("youcom","youcom",listOf(),"","","apikey",false,"openai"),
    NineProviderMeta("zcode","zc",listOf(),"https://api.z.ai/api/anthropic/v1/messages","","apikey",false,"claude"),
    NineProviderMeta("zed","zd",listOf(),"https://cloud.zed.dev/completions","","oauth",false,"openai"),
    NineProviderMeta("zenmux","zenmux",listOf(),"https://zenmux.ai/v1/chat/completions","https://zenmux.ai/v1/models","apikey",false,"openai"),
  )
  val all: List<NineProviderMeta> get() = allBuiltIn
  private val byId = allBuiltIn.associateBy { it.id }
  private val byAlias = buildMap { for (p in allBuiltIn) { put(p.alias, p); for (a in p.aliases) put(a, p) } }

  fun getAll(config: NineRouterConfig): List<NineProviderMeta> {
    val builtIn = allBuiltIn.filterNot { it.id in config.deletedProviderIds }
    return builtIn + config.customProviders
  }

  fun find(idOrAlias: String, config: NineRouterConfig? = null): NineProviderMeta? {
    if (config != null) {
      val custom = config.customProviders.firstOrNull {
        it.id.equals(idOrAlias, ignoreCase = true) || it.alias.equals(idOrAlias, ignoreCase = true)
      }
      if (custom != null) return custom
      if (idOrAlias in config.deletedProviderIds) return null
    }
    return byId[idOrAlias] ?: byAlias[idOrAlias]
  }

  fun find(idOrAlias: String): NineProviderMeta? = find(idOrAlias, null)
  fun findByModelPrefix(prefix: String): NineProviderMeta? = find(prefix)

  fun getDefaultModelsForProvider(providerId: String): List<Pair<String, String>> = when (providerId) {
    "cloudflare-ai" -> listOf(
      "cf/@cf/meta/llama-3.2-1b-instruct" to "Cloudflare Llama 3.2 1B",
      "cf/@cf/meta/llama-3.3-70b-instruct-fp8-fast" to "Cloudflare Llama 3.3 70B",
      "cf/@cf/qwen/qwen2.5-coder-32b-instruct" to "Cloudflare Qwen 2.5 Coder",
      "cf/@cf/deepseek-ai/deepseek-r1-distill-qwen-32b" to "Cloudflare R1 Distill Qwen",
    )
    "deepseek" -> listOf(
      "deepseek/deepseek-chat" to "DeepSeek Chat (V3)",
      "deepseek/deepseek-reasoner" to "DeepSeek Reasoner (R1)",
    )
    "openai" -> listOf(
      "openai/gpt-4o" to "OpenAI GPT-4o",
      "openai/gpt-4o-mini" to "OpenAI GPT-4o Mini",
    )
    "groq" -> listOf(
      "groq/llama-3.3-70b-versatile" to "Groq Llama 3.3 70B",
    )
    "openrouter" -> listOf(
      "openrouter/auto" to "OpenRouter Auto",
      "openrouter/meta-llama/llama-3.2-3b-instruct:free" to "OpenRouter Llama 3.2 3B (free)",
      "openrouter/deepseek/deepseek-r1:free" to "OpenRouter DeepSeek R1 (free)",
    )
    "opencode" -> listOf(
      "oc/muse-spark-1.2-contributor-free" to "Muse Spark 1.2 (Responses)",
      "oc/muse-spark-1.3-contributor-free" to "Muse Spark 1.3 (Responses)",
      "oc/mimo-v2.5-free" to "MiMo 2.5 Free",
      "oc/deepseek-v4-flash-free" to "DeepSeek V4 Flash Free",
      "oc/ling-3.0-flash-fin-free" to "Ling 3.0 Flash Fin Free",
      "oc/nemotron-3-ultra-free" to "Nemotron 3 Ultra Free",
      "oc/nemotron-3.5-lightning-free" to "Nemotron 3.5 Lightning Free",
      "oc/big-pickle" to "Big Pickle (Free)",
      "oc/auto" to "OpenCode Free (Auto)",
    )
    "opencode-go" -> listOf(
      "ocg/deepseek-flash" to "DeepSeek V4.1 Flash",
      "ocg/glm-5.3-flash" to "GLM 5.3 Flash (Vision)",
      "ocg/glm-5.3" to "GLM 5.3",
      "ocg/glm-5.2" to "GLM 5.2",
      "ocg/glm-5.1" to "GLM 5.1",
      "ocg/kimi-k2.7-code" to "Kimi K2.7 Code",
      "ocg/kimi-k2.6" to "Kimi K2.6",
      "ocg/kimi-k3" to "Kimi K3",
      "ocg/deepseek-v4-pro" to "DeepSeek V4 Pro",
      "ocg/deepseek-v4-flash" to "DeepSeek V4 Flash",
      "ocg/deepseek-v4-flash-vision-exp" to "DeepSeek V4 Flash Vision (Exp)",
      "ocg/longcat-2.0" to "LongCat 2.0",
      "ocg/mimo-v2.5" to "MiMo V2.5",
      "ocg/mimo-v2.5-pro" to "MiMo V2.5 Pro",
      "ocg/minimax-m3" to "MiniMax M3",
      "ocg/minimax-m2.7" to "MiniMax M2.7",
      "ocg/minimax-m2.5" to "MiniMax M2.5",
      "ocg/qwen3.8-max" to "Qwen 3.8 Max",
      "ocg/qwen3.8-flash" to "Qwen 3.8 Flash",
      "ocg/qwen3.7-max" to "Qwen 3.7 Max",
      "ocg/qwen3.7-plus" to "Qwen 3.7 Plus",
      "ocg/qwen3.6-plus" to "Qwen 3.6 Plus",
      "ocg/hy4-preview" to "Hy4 Preview",
      "ocg/hy3" to "Hy3 True Hybrid Reasoning",
      "ocg/grok-4.6" to "Grok 4.6 (Responses)",
      "ocg/gpt-5.6-luna" to "GPT 5.6 Luna (Responses)",
      "ocg/muse-spark-1.2-contributor" to "Muse Spark 1.2 Contributor (Go)",
      "ocg/muse-spark-1.3-contributor" to "Muse Spark 1.3 Contributor (Go)",
    )
    "freebuff" -> listOf(
      "fb/auto" to "Freebuff (Auto)",
    )
    "gemini" -> listOf(
      "gemini/gemini-2.0-flash" to "Gemini 2.0 Flash",
    )
    "anthropic" -> listOf(
      "anthropic/claude-3-5-sonnet-20241022" to "Claude 3.5 Sonnet",
      "anthropic/claude-3-5-haiku-20241022" to "Claude 3.5 Haiku",
    )
    "qwen" -> listOf(
      "qwen/qwen-max" to "Qwen Max",
      "qwen/qwen-plus" to "Qwen Plus",
      "qwen/qwen-turbo" to "Qwen Turbo",
    )
    "kimi" -> listOf(
      "kimi/moonshot-v1-8k" to "Moonshot Kimi 8K",
      "kimi/moonshot-v1-32k" to "Moonshot Kimi 32K",
    )
    "glm", "glm-cn" -> listOf(
      "glm/glm-4-plus" to "GLM 4 Plus",
      "glm/glm-4-flash" to "GLM 4 Flash (Free)",
    )
    "minimax", "minimax-cn" -> listOf(
      "minimax/MiniMax-Text-01" to "MiniMax Text 01",
    )
    "mistral" -> listOf(
      "mistral/mistral-large-latest" to "Mistral Large",
      "mistral/mistral-small-latest" to "Mistral Small",
      "mistral/codestral-latest" to "Codestral",
    )
    else -> {
      val meta = find(providerId)
      if (meta != null) listOf("${meta.alias}/default" to "${meta.id} (default)") else emptyList()
    }
  }
}
