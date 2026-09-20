---
name: reviewing-product-bugs
description: >-
  Use when the user asks to hunt obvious bugs, review this dashboard for P0-P3
  defects, 审查/找 bug/缺陷清单, or scan JWT, heat, streak, SSRF, XSS, pomodoro, diary.
  Do not use for style-only PR review.
---

# 审查本仓库的产品与安全缺陷

只做**自律仪表盘**的产品/安全缺陷清单。不是风格审查，不是测试覆盖报告，默认**只出清单、不改代码**。

## 原则

- 每条必须能指出文件、用户可见后果、复现路径。
- 证据来自当前代码，不要凭记忆复读旧清单。
- 用 [hotspots.md](hotspots.md) 的「已修特征」跳过已修项；特征还在就不要再报。
- 测试缺口、烟测不连 Mongo、命名/格式问题：不是产品 bug。

## 分级

| 级 | 含义 |
| --- | --- |
| P0 | 未授权伪造身份、任意代码执行、无需登录的严重数据破坏 |
| P1 | 越权、SSRF、XSS、可丢数据、登录后必现 500 |
| P2 | 统计/热力算错、弱校验、全表扫描、可抢占无主数据 |
| P3 | 加固（token 存储）、跨模块 userId 不一致 |

## 流程

1. 读 [hotspots.md](hotspots.md)。
2. 只扫热区文件，不要全仓漫游。
3. 对每条候选：对照已修特征；标分级；写复现。
4. 按下面模板输出。用户说「修」之后才改代码。

## 输出模板

```markdown
# 缺陷清单

## P0
### 标题
- 文件：path:line
- 为何这级：……
- 复现：……

## P1
（同上）

## P2
（同上）

## P3
（同上）

## 已确认仍成立的已修项（不要当新 bug）
- 一句话 + 对应已修特征
```

某级没有就写「无」。不要把「建议加测试」写成缺陷。
