#!/usr/bin/env node
/**
 * build-default-wordlist.mjs — 为一词库生成「单词|中文翻译|例句|音标」四段格式数据文件。
 *
 * 数据来源与署名
 * ---------------------------------------------------------------------------
 * 例句：Tatoeba (https://tatoeba.org) — 许可证 CC BY 2.0 FR
 *       下载：https://downloads.tatoeba.org/exports/per_language/eng/eng_sentences.tsv.bz2
 *       TSV 三列：句子ID \t 语言 \t 句子文本（en = 英文）
 *       （注意：本脚本刻意不使用 dictionaryapi.dev / Wiktionary 的例句，
 *         它们返回的是未经筛选的历史文献引证，不适合学习场景。）
 * 音标：ECDICT (https://github.com/skywind3000/ECDICT) — 许可证 MIT
 *       下载：https://raw.githubusercontent.com/skywind3000/ECDICT/master/ecdict.csv
 *       只取 CSV 的 word / phonetic 两列。
 *
 * 用法
 * ---------------------------------------------------------------------------
 *   node tools/build-default-wordlist.mjs                 # 下载并生成（写入默认路径）
 *   node tools/build-default-wordlist.mjs --check          # 只校验已有输出文件，不重新生成
 *   node tools/build-default-wordlist.mjs --out <path>     # 自定义输出路径
 *   node tools/build-default-wordlist.mjs --input <path>   # 自定义原始词库（两列格式）
 *   node tools/build-default-wordlist.mjs --cache <dir>    # 下载/解压缓存目录（默认 .runtime/dl）
 *   node tools/build-default-wordlist.mjs --python <exe>   # 用于解压 .bz2 的 Python 解释器
 *   node tools/build-default-wordlist.mjs --sample 20      # 抽样打印条数
 *
 * 环境要求：Node >= 18（需要全局 fetch、WebStreams）、Python 3（标准库 bz2 解压）。
 *   若未显式传 --python，会依次尝试环境变量 DSH_PYTHON、python3、python、py -3。
 *   HTTPS 走 Node 的 fetch（本仓库运行环境里 schannel 被禁用，不要用 PowerShell/curl 发 HTTPS）。
 *
 * 本脚本是幂等的：把原始词库 + 上述两个数据源喂进来，必然产出同一个结果文件。
 */

import fs from 'node:fs'
import path from 'node:path'
import os from 'node:os'
import readline from 'node:readline'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const PROJECT_ROOT = path.resolve(__dirname, '..')

// ── 常量 ────────────────────────────────────────────────────────────────────
const TATOEBA_URL = 'https://downloads.tatoeba.org/exports/per_language/eng/eng_sentences.tsv.bz2'
const ECDICT_URL = 'https://raw.githubusercontent.com/skywind3000/ECDICT/master/ecdict.csv'
const TATOEBA_BZ2 = 'eng_sentences.tsv.bz2'
const TATOEBA_TSV = 'eng_sentences.tsv'
const ECDICT_CSV = 'ecdict.csv'

const MIN_LEN = 25
const MAX_LEN = 70

const DEFAULT_INPUT = path.join(PROJECT_ROOT, 'backend/src/main/resources/wordlists/default.txt')
const DEFAULT_OUT = DEFAULT_INPUT

// ── 命令行参数 ──────────────────────────────────────────────────────────────
function parseArgs(argv) {
  const opts = {
    input: DEFAULT_INPUT,
    out: DEFAULT_OUT,
    cache: path.join(PROJECT_ROOT, '.runtime/dl'),
    python: null,
    check: false,
    sample: 18,
  }
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i]
    const next = () => {
      const v = argv[++i]
      if (v === undefined) throw new Error(`缺少参数值: ${a}`)
      return v
    }
    if (a === '--input') opts.input = path.resolve(next())
    else if (a === '--out') opts.out = path.resolve(next())
    else if (a === '--cache') opts.cache = path.resolve(next())
    else if (a === '--python') opts.python = next()
    else if (a === '--check') opts.check = true
    else if (a === '--sample') opts.sample = Number(next())
    else if (a === '--help' || a === '-h') { console.log(fs.readFileSync(fileURLToPath(import.meta.url), 'utf8').split('*/')[0]); process.exit(0) }
    else throw new Error(`未知参数: ${a}`)
  }
  return opts
}

// ── I/O 工具 ───────────────────────────────────────────────────────────────
function readTextUtf8(p) {
  let buf = fs.readFileSync(p)
  if (buf.length >= 3 && buf[0] === 0xef && buf[1] === 0xbb && buf[2] === 0xbf) buf = buf.subarray(3) // 去 BOM
  return buf.toString('utf8')
}

function writeTextLfNoBom(p, lines) {
  fs.mkdirSync(path.dirname(p), { recursive: true })
  fs.writeFileSync(p, lines.join('\n') + '\n', { encoding: 'utf8' }) // 显式 \n，无 BOM
}

/**
 * 下载（Node fetch + 流式落盘），已存在且非空则跳过。网络中断会重试。
 *
 * `idleTimeoutMs` 是"空闲超时"：只要还有数据在流动就不算超时，但超过这个时间一个字节
 * 都收不到就中止本次尝试。没有这层保护时，一个卡住的连接会让整个脚本永久挂起
 * （GitHub raw 上拉 ECDICT 时就出现过：文件停在 530KB 再无进展）。
 */
async function download(url, dest, attempts = 3, idleTimeoutMs = 20000) {
  if (fs.existsSync(dest) && fs.statSync(dest).size > 0) {
    console.log(`  [cache] ${path.basename(dest)} (${(fs.statSync(dest).size / 1048576).toFixed(1)} MB) 已存在，跳过下载`)
    return dest
  }
  fs.mkdirSync(path.dirname(dest), { recursive: true })
  const tmp = dest + '.part'
  let lastErr = null
  for (let attempt = 1; attempt <= attempts; attempt++) {
    let idleTimer = null
    let out = null
    const controller = new AbortController()
    const resetIdle = () => {
      if (idleTimer) clearTimeout(idleTimer)
      idleTimer = setTimeout(
        () => controller.abort(new Error(`连续 ${idleTimeoutMs}ms 未收到数据，判定连接已卡死`)),
        idleTimeoutMs,
      )
    }
    try {
      fs.rmSync(tmp, { force: true })
      console.log(`  [download] ${url}${attempt > 1 ? ` (第 ${attempt} 次尝试)` : ''}`)
      resetIdle()
      const res = await fetch(url, {
        headers: { 'User-Agent': 'dsh-wordlist-builder/1.0' },
        signal: controller.signal,
      })
      if (!res.ok) throw new Error(`HTTP ${res.status} ${res.statusText} for ${url}`)
      const total = Number(res.headers.get('content-length') || 0)
      // Node 的 fetch 会自动解压 gzip/br，此时 content-length 是"压缩后"的长度，
      // 而 received 统计的是解压后的字节数，两者不可比 —— 直接比对会把一次完整下载
      // 误判成"下载不完整"（ECDICT 的 ecdict.csv 就踩过这个坑：进度跑到 274%）。
      const encoding = res.headers.get('content-encoding')
      const lengthComparable = total > 0 && (!encoding || encoding === 'identity')
      out = fs.createWriteStream(tmp)
      let received = 0
      let lastPct = -1
      let lastLoggedBytes = 0
      for await (const chunk of res.body) {
        resetIdle()
        received += chunk.length
        if (!out.write(chunk)) await new Promise((r) => out.once('drain', r))
        if (lengthComparable) {
          const pct = Math.floor((received / total) * 100)
          if (pct >= lastPct + 25) {
            lastPct = pct
            console.log(`    ${pct}% (${(received / 1048576).toFixed(1)} MB)`)
          }
        } else if (received - lastLoggedBytes >= 8 * 1048576) {
          lastLoggedBytes = received
          console.log(`    ${(received / 1048576).toFixed(1)} MB（服务端为压缩传输，无总长度可比）`)
        }
      }
      await new Promise((r) => out.end(r))
      out = null // 已正常关闭，交给下面的 finally 处理
      if (lengthComparable && received !== total) {
        throw new Error(`下载不完整：${received}/${total} 字节`)
      }
      fs.renameSync(tmp, dest)
      console.log(`    完成 ${(fs.statSync(dest).size / 1048576).toFixed(1)} MB`)
      return dest
    } catch (e) {
      lastErr = e
      const why = controller.signal.aborted && controller.signal.reason
        ? controller.signal.reason.message
        : e.message
      console.log(`    失败：${why}${attempt < attempts ? '，稍后重试…' : ''}`)
      await new Promise((r) => setTimeout(r, 1500 * attempt))
    } finally {
      // 中止/失败时必须销毁写入流并删掉半截文件，否则文件句柄仍被占用，
      // 下一次尝试的 createWriteStream 会在 Windows 上报 EPERM。
      if (out) {
        out.destroy()
        out = null
        await new Promise((r) => setTimeout(r, 200))
      }
      fs.rmSync(tmp, { force: true })
      if (idleTimer) clearTimeout(idleTimer)
    }
  }
  throw new Error(`下载失败（已重试 ${attempts} 次）：${lastErr && lastErr.message}`)
}

/** 用 Python 标准库 bz2 解压（Node 无内置 bzip2）。 */
function bunzip2(bz2Path, tsvPath, pythonExe) {
  if (fs.existsSync(tsvPath) && fs.statSync(tsvPath).size > 0) {
    console.log(`  [cache] ${path.basename(tsvPath)} (${(fs.statSync(tsvPath).size / 1048576).toFixed(1)} MB) 已存在，跳过解压`)
    return tsvPath
  }
  const candidates = pythonExe ? [pythonExe] : [process.env.DSH_PYTHON, 'python3', 'python', 'py'].filter(Boolean)
  const code = [
    'import bz2, shutil, sys',
    'src, dst = sys.argv[1], sys.argv[2]',
    'with bz2.open(src, "rb") as f, open(dst, "wb") as g: shutil.copyfileobj(f, g, 1 << 20)',
  ].join('\n')
  let lastErr = null
  for (const exe of candidates) {
    const args = exe === 'py' ? ['-3', '-c', code, bz2Path, tsvPath] : ['-c', code, bz2Path, tsvPath]
    console.log(`  [bunzip2] ${exe} ...`)
    const r = spawnSync(exe, args, { stdio: ['ignore', 'inherit', 'pipe'] })
    if (r.status === 0 && fs.existsSync(tsvPath)) {
      console.log(`    完成 ${(fs.statSync(tsvPath).size / 1048576).toFixed(1)} MB`)
      return tsvPath
    }
    lastErr = r.error || new Error(`${exe} 退出码 ${r.status}: ${String(r.stderr || '').slice(0, 300)}`)
    fs.rmSync(tsvPath, { force: true })
  }
  throw new Error(`解压失败（需要 Python 3 的 bz2 模块）: ${lastErr && lastErr.message}`)
}

// ── 原始词库解析 ───────────────────────────────────────────────────────────
function parseSourceWordlist(text) {
  const rows = []
  const rawLines = text.split(/\r?\n/)
  for (let i = 0; i < rawLines.length; i++) {
    const line = rawLines[i]
    if (!line.trim()) continue
    if (line.startsWith('#')) continue
    const parts = line.split('|')
    if (parts.length < 2) throw new Error(`原始词库第 ${i + 1} 行不是「单词|翻译」格式: ${line}`)
    rows.push({ word: parts[0].trim(), translation: parts[1] })
  }
  return rows
}

// ── 例句筛选 ───────────────────────────────────────────────────────────────
// 只允许 ASCII 可见字符（含空格）与常规标点；任何非 ASCII 一律剔除。
const ALLOWED_CHARS = /^[\x20-\x7e]+$/
const URL_RE = /(https?:|www\.|\.[a-z]{2,4}\/)/i
// 剔除标注类字符：方括号/花括号/尖括号/竖线/下划线/反引号/星号/反斜杠/at/井号/波浪号/脱字符
const BAD_CHARS_RE = /[\[\]{}<>|_`*\\@#~^]/

/**
 * 粗俗/冒犯性词汇，命中即整句剔除。
 * Tatoeba 是未经审核过滤的开放语料：实测 academic 的首选例句是
 * "Fuck your academic career."，学习应用不能收录这类内容。
 * 只拦明显的脏话与侮辱语，不拦 kill / drug / hate 这类本身属于正常词汇的词。
 */
const PROFANITY_RE =
  /\b(fuck|fucking|fucked|fucker|shit|bullshit|bitch|bastard|asshole|arsehole|dick|cunt|piss|whore|slut|crap|damn|goddamn|porn|porno|sexy|rape|raped|suicide|nazi|retard|retarded)\b/i

/**
 * 非首词的大写词通常是人名/地名。Tatoeba 大量使用其虚构角色名
 * （Tom / Sami / Mary / Mayuko …），重复且与词义无关，所以打分时降权。
 * 这里只降权、不过滤，避免牺牲例句覆盖率。
 */
const HARMLESS_CAPITALIZED = new Set([
  // 第一人称缩写（去掉撇号后比较）
  'Im', 'Ill', 'Ive', 'Id',
  // 语言 / 星期 / 月份 / 节日等常见无害大写词
  'English', 'French', 'Spanish', 'Chinese', 'Japanese', 'German', 'Italian', 'Russian',
  'Arabic', 'Korean', 'Portuguese', 'Hindi',
  'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday',
  'January', 'February', 'March', 'April', 'May', 'June', 'July', 'August',
  'September', 'October', 'November', 'December',
  'Christmas', 'God', 'Earth', 'Internet',
])

/** 句子里是否出现疑似专有名词（只看非首词）。 */
function hasProperNoun(sentence) {
  return sentence.split(/\s+/).slice(1).some((token) => {
    const core = token.replace(/[^A-Za-z]/g, '')
    if (core.length < 2) return false
    if (!/^[A-Z]/.test(core)) return false
    return !HARMLESS_CAPITALIZED.has(core)
  })
}

/** 构造「词形」正则：仅允许常见屈折后缀，且后缀不跨越词干。 */
function wordRegex(word) {
  const w = word.toLowerCase().replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return new RegExp(`\\b(?:${w}(?:s|es|ed|d|ing|ly|er|est|ies|ied)?)\\b`, 'i')
}

function isAcceptable(sentence) {
  if (sentence.length < MIN_LEN || sentence.length > MAX_LEN) return false
  if (!ALLOWED_CHARS.test(sentence)) return false
  if (!/^[A-Z]/.test(sentence)) return false
  if (!/[.?!]$/.test(sentence)) return false
  if (/\d/.test(sentence)) return false // 年份/文献引证/编号一律剔除
  if (URL_RE.test(sentence)) return false
  if (BAD_CHARS_RE.test(sentence)) return false
  if (PROFANITY_RE.test(sentence)) return false // 脏话/冒犯性内容整句剔除
  return true
}

/**
 * 从 Tatoeba TSV 中为每个单词挑选最适合学习的一句。
 * 策略：按「是否原形命中 → 句子长度」排序取最优；同一批结果内保证不重复。
 */
async function pickExamples(words, tsvPath) {
  const lower = words.map((w) => w.toLowerCase())
  const regexes = lower.map((w) => wordRegex(w))
  const exact = lower.map((w) => new RegExp(`\\b${w.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\b`, 'i'))
  const best = new Array(words.length).fill(null) // { sentence, score }

  // score 越大越好
  const scoreOf = (sentence, i) => {
    const isExact = exact[i].test(sentence)
    let s = 0
    if (isExact) s += 1000
    s += (MAX_LEN - sentence.length) // 越短越好
    if (/[?!]$/.test(sentence)) s -= 5 // 陈述句优先（标点/大小写更规范）
    if (hasProperNoun(sentence)) s -= 60 // 含人名的句子降权，但不排除（保覆盖率）
    return s
  }

  const rl = readline.createInterface({ input: fs.createReadStream(tsvPath, 'utf8'), crlfDelay: Infinity })
  let scanned = 0
  for await (const line of rl) {
    if (!line) continue
    scanned++
    // TSV: 句子ID \t 语言 \t 句子文本 —— 只处理英文行
    const firstTab = line.indexOf('\t')
    if (firstTab < 0) continue
    const secondTab = line.indexOf('\t', firstTab + 1)
    if (secondTab < 0) continue
    if (line.slice(firstTab + 1, secondTab).trim() !== 'eng') continue
    const sentence = line.slice(secondTab + 1).replace(/[\r\n]+/g, ' ').trim()
    if (!sentence) continue
    if (!isAcceptable(sentence)) continue
    let hit = false
    for (let i = 0; i < regexes.length; i++) {
      if (!regexes[i].test(sentence)) continue
      hit = true
      const s = scoreOf(sentence, i)
      if (!best[i] || s > best[i].score) best[i] = { sentence, score: s }
    }
    if (!hit) continue
  }
  // 第二轮去重：若同一句被多个词选中，只保留给「更需要」的词（原形命中优先、其次短句）
  const bySentence = new Map()
  for (let i = 0; i < best.length; i++) {
    if (!best[i]) continue
    const arr = bySentence.get(best[i].sentence) || []
    arr.push({ i, score: best[i].score })
    bySentence.set(best[i].sentence, arr)
  }
  const used = new Set()
  for (const [sentence, arr] of bySentence) {
    if (arr.length === 1) { used.add(sentence); continue }
    arr.sort((a, b) => b.score - a.score)
    for (const { i } of arr.slice(1)) best[i] = null // 冲突时让给更优的词，该词例句留空
    used.add(sentence)
  }
  return { examples: best.map((b) => (b ? b.sentence : '')), scanned, usedSentences: used.size }
}

// ── 音标（ECDICT CSV） ─────────────────────────────────────────────────────
/** 流式扫描 ECDICT CSV，返回 word -> phonetic（只取 word/phonetic 两列）。 */
async function loadPhonetics(csvPath) {
  const map = new Map()
  const rl = readline.createInterface({ input: fs.createReadStream(csvPath, 'utf8'), crlfDelay: Infinity })
  let headerChecked = false
  let n = 0
  for await (const line of rl) {
    if (!line) continue
    if (!headerChecked) {
      headerChecked = true
      if (/^word,phonetic/i.test(line)) continue // 表头
    }
    n++
    const comma = line.indexOf(',')
    if (comma <= 0) continue
    const word = line.slice(0, comma).trim().toLowerCase()
    if (!word) continue
    const rest = line.slice(comma + 1)
    // phonetic 是第二列，遇到下一个未加引号的逗号即结束
    let phoneticRaw
    if (rest.startsWith('"')) {
      let i = 1, cur = ''
      while (i < rest.length) {
        if (rest[i] === '"') { if (rest[i + 1] === '"') { cur += '"'; i += 2; continue } break }
        cur += rest[i]; i++
      }
      phoneticRaw = cur
    } else {
      const c = rest.indexOf(',')
      phoneticRaw = c < 0 ? rest : rest.slice(0, c)
    }
    if (map.has(word)) continue
    map.set(word, phoneticRaw.trim())
  }
  return { map, rows: n }
}

/** 允许出现在音标里的字符（IPA、重音符号、常用拉丁扩展）。 */
const PHONETIC_ALLOWED =
  /^[\x20-\x7e\u0250-\u02ff\u02b0-\u02ff\u0300-\u036f\u1d00-\u1d7f\u2018-\u201d\u00e6\u00f0\u00f8\u014b\u0153\u0279\u0281\u0283\u0292\u03b8\u03b2]+$/

/**
 * ECDICT 的源数据里混进了西里尔字母的"形近字"（历史编码问题）。直接输出会把错字符
 * 显示给用户，所以先归一化成对应的 IPA，再走白名单校验。
 *
 * 已实测确认的两处（对 default.txt 的 108 个词逐一核对过码点）：
 *   ә U+04D9 → ə U+0259    schwa：abandon = ә'bændәn → /əˈbændən/
 *   є U+0454 → e            SQUARE 元音：air = єә → /eə/，与牛津/剑桥学习词典写法一致
 *
 * 表里没有的非常规字符不会被"放行"，而是被丢弃并计数上报（见 cleanPhonetic），
 * 避免静默产出错误音标。
 */
const PHONETIC_HOMOGLYPHS = new Map([
  ['\u04d9', '\u0259'],
  ['\u0454', 'e'],
])

/** 被白名单拒绝的字符 -> 出现次数，用于运行结束后上报。 */
const droppedPhoneticChars = new Map()

function normalizePhonetic(raw) {
  let s = String(raw)
  for (const [from, to] of PHONETIC_HOMOGLYPHS) s = s.split(from).join(to)
  return s
}

/** 清洗音标：归一化形近字，去空格/竖线，统一输出为 /.../ 形式。 */
function cleanPhonetic(p) {
  if (!p) return ''
  let s = normalizePhonetic(p).replace(/[\r\n\t|]/g, '').trim()
  if (!s) return ''
  // 去掉外层方括号/斜杠，统一输出为 /.../ 形式
  s = s.replace(/^[[/]+/, '').replace(/[/\]]+$/, '').trim()
  if (!s) return ''
  if (!PHONETIC_ALLOWED.test(s)) {
    for (const ch of s) {
      if (!PHONETIC_ALLOWED.test(ch)) {
        droppedPhoneticChars.set(ch, (droppedPhoneticChars.get(ch) || 0) + 1)
      }
    }
    return ''
  }
  return `/${s}/`
}

/** 报告被丢弃的音标字符，便于发现新的编码/音标源问题。 */
function reportDroppedPhoneticChars() {
  if (droppedPhoneticChars.size === 0) return
  const detail = [...droppedPhoneticChars.entries()]
    .map(([ch, n]) => `'${ch}' U+${ch.codePointAt(0).toString(16).toUpperCase().padStart(4, '0')} ×${n}`)
    .join('，')
  console.log(`  [warn] 含未识别字符而被丢弃的音标字符：${detail}`)
  console.log('         （如需支持，请在 PHONETIC_HOMOGLYPHS 里补一条经过核对的映射）')
}

// ── 校验 ───────────────────────────────────────────────────────────────────
const PIPES = (line) => (line.match(/\|/g) || []).length

function selfCheck(originalRows, outLines, { sample = 18, quiet = false } = {}) {
  const log = quiet ? () => {} : console.log
  const problems = []
  const dataLines = outLines.filter((l) => l && !l.startsWith('#'))
  const parsed = dataLines.map((l) => l.split('|'))

  if (dataLines.length !== originalRows.length) {
    problems.push(`行数不一致：原始 ${originalRows.length} 行，输出 ${dataLines.length} 行`)
  }
  const n = Math.min(parsed.length, originalRows.length)
  let orderOk = true
  let translationOk = true
  for (let i = 0; i < n; i++) {
    if (parsed[i][0] !== originalRows[i].word) {
      orderOk = false
      problems.push(`第 ${i + 1} 行单词不一致：原始「${originalRows[i].word}」vs 输出「${parsed[i][0]}」`)
    }
    if (parsed[i][1] !== originalRows[i].translation) {
      translationOk = false
      problems.push(`第 ${i + 1} 行翻译不一致：「${originalRows[i].translation}」vs「${parsed[i][1]}」`)
    }
  }
  const badPipe = dataLines.map((l, i) => [i + 1, l]).filter(([, l]) => PIPES(l) !== 3)
  if (badPipe.length) problems.push(`以下行不是恰好 3 个 '|'：${badPipe.slice(0, 5).map(([i]) => i).join(', ')}`)
  const badField = parsed.filter((p) => p.length !== 4)
  if (badField.length) problems.push(`以下行不是 4 字段：${badField.length} 行`)
  const nonAsciiExample = parsed.filter((p) => p[2] && !/^[\x20-\x7e]+$/.test(p[2]))
  if (nonAsciiExample.length) problems.push(`例句含非 ASCII 字符：${nonAsciiExample.length} 行`)

  const withEx = parsed.filter((p) => p[2] && p[2].trim()).length
  const withPh = parsed.filter((p) => p[3] && p[3].trim()).length

  log('\n===== 自检报告 =====')
  log(`原始单词数            : ${originalRows.length}`)
  log(`输出数据行数          : ${dataLines.length}`)
  log(`单词顺序一致          : ${orderOk ? '是' : '否'}`)
  log(`中文翻译逐行一致      : ${translationOk ? '是' : '否'}`)
  log(`每行恰好 3 个 '|'     : ${badPipe.length === 0 ? '是' : `否（${badPipe.length} 行异常）`}`)
  log(`例句覆盖              : ${withEx}/${parsed.length} (${((withEx / parsed.length) * 100).toFixed(1)}%)`)
  log(`音标覆盖              : ${withPh}/${parsed.length} (${((withPh / parsed.length) * 100).toFixed(1)}%)`)
  log(`异常项                : ${problems.length === 0 ? '无' : ''}`)
  for (const p of problems) log(`  - ${p}`)

  const missingEx = parsed.filter((p) => !p[2] || !p[2].trim()).map((p) => p[0])
  if (missingEx.length) log(`无合格例句的词        : ${missingEx.join(', ')}`)
  const missingPh = parsed.filter((p) => !p[3] || !p[3].trim()).map((p) => p[0])
  if (missingPh.length) log(`无音标的词            : ${missingPh.join(', ')}`)

  if (sample > 0) {
    log(`\n===== 随机抽样 ${Math.min(sample, parsed.length)} 条 =====`)
    // 固定种子式抽样，保证可复现
    const idx = []
    for (let i = 0; i < parsed.length; i++) idx.push(i)
    let seed = 20240607
    const rnd = () => { seed = (seed * 1103515245 + 12345) & 0x7fffffff; return seed / 0x7fffffff }
    for (let i = idx.length - 1; i > 0; i--) { const j = Math.floor(rnd() * (i + 1)); [idx[i], idx[j]] = [idx[j], idx[i]] }
    for (const i of idx.slice(0, Math.min(sample, parsed.length))) {
      const p = parsed[i]
      log(`${String(i + 1).padStart(3)}. ${p[0]} | ${p[1]} | ${p[2] || '(无例句)'} | ${p[3] || '(无音标)'}`)
    }
  }
  return { problems, withEx, withPh, total: parsed.length }
}

// ── 主流程 ─────────────────────────────────────────────────────────────────
async function main() {
  const opts = parseArgs(process.argv.slice(2))

  if (opts.check) {
    const originalRows = parseSourceWordlist(readTextUtf8(opts.input))
    const outLines = readTextUtf8(opts.out).split(/\r?\n/)
    while (outLines.length && outLines[outLines.length - 1] === '') outLines.pop()
    const header = outLines.filter((l) => l.startsWith('#')).length
    console.log(`校验文件: ${opts.out}（含 ${header} 行注释）`)
    const res = selfCheck(originalRows, outLines, { sample: opts.sample })
    process.exit(res.problems.length ? 1 : 0)
  }

  console.log('原始词库 :', opts.input)
  console.log('输出文件 :', opts.out)
  console.log('缓存目录 :', opts.cache)

  const originalRows = parseSourceWordlist(readTextUtf8(opts.input))
  console.log(`解析到 ${originalRows.length} 个单词（保留顺序，只新增例句/音标两列）`)

  // 1. 数据源
  console.log('\n[1/4] 准备数据源')
  const bz2 = await download(TATOEBA_URL, path.join(opts.cache, TATOEBA_BZ2))
  const tsv = bunzip2(bz2, path.join(opts.cache, TATOEBA_TSV), opts.python)
  // ECDICT 只提供音标，属于"锦上添花"：例句才是复习页的核心内容，
  // 因此音标源不可用时必须降级继续，而不是让整次构建失败。
  let csv = null
  try {
    // 音标源只试 2 次：实测 GitHub raw 上这个文件会中途卡死，不值得为可选的
    // 音标列反复消耗时间，卡住就降级留空（脚本仍会如实写出处的注释）。
    csv = await download(ECDICT_URL, path.join(opts.cache, ECDICT_CSV), 2)
  } catch (e) {
    console.log(`  [warn] ECDICT 音标源不可用，音标列将留空：${e.message}`)
  }

  // 2. 例句
  console.log('\n[2/4] 从 Tatoeba 挑选例句')
  const words = originalRows.map((r) => r.word)
  const { examples, scanned } = await pickExamples(words, tsv)
  console.log(`  扫描英文句子 ${scanned} 行，命中例句 ${examples.filter(Boolean).length}/${words.length}`)

  // 3. 音标
  console.log('\n[3/4] 从 ECDICT 读取音标')
  let phonetics = words.map(() => '')
  if (csv) {
    const { map: phoneticMap, rows } = await loadPhonetics(csv)
    console.log(`  读取词条 ${rows} 行，音标表 ${phoneticMap.size} 条`)
    phonetics = words.map((w) => cleanPhonetic(phoneticMap.get(w.toLowerCase()) || ''))
  } else {
    console.log('  跳过：无 ECDICT 数据，音标列为空')
  }
  reportDroppedPhoneticChars()
  console.log(`  音标覆盖 ${phonetics.filter(Boolean).length}/${words.length}`)

  // 4. 写出
  console.log('\n[4/4] 写出四段格式')
  const lines = [
    '# 默认词库：单词|中文翻译|例句|音标',
    '# 例句来源：Tatoeba (https://tatoeba.org)，许可证 CC BY 2.0 FR',
    csv
      ? '# 音标来源：ECDICT (https://github.com/skywind3000/ECDICT)，许可证 MIT'
      : '# 音标：未获取（ECDICT 下载失败），本文件音标列为空',
    '# 生成脚本：tools/build-default-wordlist.mjs（可重复运行以复现同样结果）',
  ]
  for (let i = 0; i < originalRows.length; i++) {
    const safe = (s) => String(s || '').replace(/[|\r\n]/g, ' ').replace(/\s+/g, ' ').trim()
    lines.push([safe(originalRows[i].word), safe(originalRows[i].translation), safe(examples[i]), safe(phonetics[i])].join('|'))
  }
  writeTextLfNoBom(opts.out, lines)
  console.log(`  已写入 ${opts.out}（UTF-8 / LF / 无 BOM，${lines.length} 行）`)

  selfCheck(originalRows, lines, { sample: opts.sample })
}

main().catch((e) => {
  console.error('\n构建失败:', e && e.stack ? e.stack : e)
  process.exit(1)
})
