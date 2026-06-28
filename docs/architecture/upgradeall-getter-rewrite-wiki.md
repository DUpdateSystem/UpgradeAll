# UpgradeAll getter 重构架构设计 Wiki

> 状态：设计草案 / living document
> 日期：2026-06-21 17:27 CST
> 适用范围：UpgradeAll 从旧 Android/Kotlin + Room + hub-app 模型，重构为 Flutter UI + Rust getter core + Lua package repository 模型。
> 设计原则：所有重要代码边界、数据模型、迁移策略和架构决策都必须记录在案；后续实现必须同步更新本文或对应 ADR。

---

## 0. 文档目的

本文是 UpgradeAll 新架构的主设计文档，用来约束后续代码实现、重构计划、迁移策略和 wiki/开发文档。

本文不是单纯的想法记录，而是用于回答这些问题：

1. 为什么放弃旧的 `hub-app` 模型。
2. 新的 `getter` 和 `APP` 边界是什么。
3. 为什么所有 product/domain logic 都进入 Rust getter。
4. 为什么新 UI 使用 Flutter。
5. 为什么 getter backend storage 使用 SQLite。
6. 为什么 package/update 模型采用 Lua package repository，而不是固定模板或旧 Hub。
7. Lua package 脚本如何组织、导入、覆写、生成和校验。
8. 旧数据如何无感迁移。
9. 用户二次开发、AI fork、patch stack 如何不被架构拖累。
10. 哪些决策已经锁定，哪些仍是 open question。

后续规则：

- 每个重要代码模块都应能在本文或后续 ADR 中找到设计依据。
- 每个破坏性决策都应有「为什么不选其他方案」。
- 每个迁移逻辑都应记录数据来源、目标、保留字段和丢弃字段。
- 每个 Lua API / Rust API / Flutter adapter API 都应有边界说明。

---

## 1. 背景：现有 UpgradeAll 的事实基础

### 1.1 当前产品定位

当前 UpgradeAll 是 Android 上的更新检查/下载工具，核心能力包括：

- 检查 Android apps、Magisk modules 等对象的更新。
- 从多个来源获取 release/update 信息，例如 GitHub、GitLab、F-Droid、Google Play、CoolApk、Source List / cloud config。
- 支持用户自定义规则、Hub/App 配置、外部下载器、本地/云备份、日志、安装器等能力。

代码审计来源：

- `/home/xz/workspace/upgradeall-audit/upgradeall-current-context-map.md`
- `settings.gradle`
- `app/build.gradle`
- `app/src/main/AndroidManifest.xml`
- `core/src/main/java/net/xzos/upgradeall/core/database/MetaDatabase.kt`
- `core-getter/rpc/src/main/java/net/xzos/upgradeall/getter/rpc/GetterService.kt`

关键事实：

- 官方 Android applicationId 是 `net.xzos.upgradeall`。
- 当前版本信息：`versionCode = 105`, `versionName = "0.20-alpha.4"`。
- Debug build 使用 `applicationIdSuffix ".debug"`，不能代表正式升级路径。
- 当前 app 仍以 Activity / Fragment / XML / DataBinding / ViewBinding 为主。
- Compose 依赖存在，但不是主 UI 架构。
- `core-getter` 已经有 Rust getter 的 JNI/RPC 集成，但目前仍是过渡形态。

Rewrite 决策更新：`app_flutter/` 是新架构唯一产品 APK 入口；旧 `:app` 原生 UI 暂时保留为参考代码，但不再作为 rewrite 的发布/启动路径。Android CI/release 产物必须来自 Flutter app，旧 native UI 不能继续接收新的产品入口。

### 1.2 当前 Gradle 模块

现有模块：

```text
:app
:core
:core-websdk
:core-utils
:core-shell
:core-downloader
:core-installer
:core-android-utils
:app-backup
:core-getter
:core-websdk:data
:core-getter:provider
:core-getter:rpc
```

当前职责概括：

- `:app`：Android UI、Activity/Fragment、WorkManager、偏好设置、日志、文件管理等。
- `:core`：Room DB、App/Hub/domain 状态、版本比较、更新状态推导、manager 薄壳。
- `:core-websdk`：旧 Web SDK API 与 Rust getter 代理桥接；Kotlin hub RPC server；GooglePlay/CoolApk 回调。
- `:core-downloader`：下载相关 Android/Kotlin 层能力。
- `:core-installer`：安装器相关能力。
- `:core-android-utils`：PackageManager / Android 文件与系统工具。
- `:app-backup`：本地 zip 备份/恢复与 WebDAV 云备份。
- `:core-getter`：JNI/native Rust api_proxy + GetterPort。
- `:core-getter:rpc`：Kotlin WebSocket JSON-RPC client 和 DTO。

### 1.3 当前用户可见功能

新架构必须理解并有意识地处理这些现有功能：

- Home：模块入口、检查更新、自动检查更新、更新数量展示、普通/简化模式。
- Apps/Magisk：按 app type 展示，包含 Updates/Star/All/Applications 条件 tab，支持添加、编辑、删除、批量更新/忽略。
- Discover：发现 cloud config/source list 中的 app 配置，搜索、刷新、导入。
- Hub Manager：启用/禁用 Hub、applications mode、认证、URL replace、全局设置。
- App Detail：版本选择、查看 changelog/more URL、下载 asset、编辑 App、改 source/Hub 优先级、忽略当前版本。
- File Management：下载任务状态、暂停/继续/重试/删除/安装/打开文件。
- Settings：Backup、Downloader、UI、Updates、Language、Installation。
- Log：分类查看、清空、导出。
- Restore/Migration：恢复/迁移进度页。

这些功能不一定一比一保留旧 UI，但产品语义必须被新架构覆盖或明确标记为 v1 非目标。

---

## 2. 旧架构的问题

### 2.1 `hub-app` 模型已经不够

旧模型大致是：

```text
App
  app_id
  enable_hub_list
  cloud_config

Hub
  GitHub / F-Droid / GooglePlay / CoolApk / Source List
```

这个模型的问题：

1. GitHub/F-Droid/Google Play/CoolApk 本质上不是「包」，而是 provider/source/backend。
2. 同一个 App 可以来自多个来源，但它仍应是同一个更新对象。
3. 不同项目的发布方式差异极大，固定 Hub 模板会无限膨胀。
4. App 的打包、版本选择、asset 选择、校验、安装对象匹配都应是 package 级别逻辑，而不是 Hub 级别逻辑。
5. 旧模型难以表达类似 package manager 的 repository/overlay/override 关系。

结论：新架构放弃 `hub-app` 模型，改为 app/package-centric 模型。

### 2.2 渐进式剥离失败

当前代码已经尝试将部分逻辑迁移到 Rust getter，但仍存在：

- Room 与 Rust JSONL 并存。
- Kotlin AppManager/HubManager 仍承担大量状态/业务逻辑。
- `migrateRoomToRust()` 是一次性倒账，不是正式迁移系统。
- 旧 UI、旧 DB、旧 Hub、Rust getter 的边界复杂交错。
- 兴趣开发无法长期维持这种双架构过渡成本。

结论：新版本从零重构，不继续渐进式剥离。

### 2.3 当前 Room -> Rust 迁移技术债

当前 `migrateRoomToRust()` 的问题：

- 只在 `apps.jsonl` 不存在或为空时执行。
- 从 Room 读取 apps/hubs/extra_hub。
- 没有覆盖 `extra_app`。
- AppEntity 迁移时 Rust 会重新分配 app UUID。
- 没有持续同步或双向同步。
- 它是启动时一次性倒账，不是版本化、事务性、可验证的正式迁移。

结论：正式重构不能沿用该方案。

---

## 3. 新架构总览

### 3.1 核心决策

已锁定决策：

1. 新 UI 使用 Flutter。
2. getter 使用 Rust。
3. 所有 product/domain logic 都放在 getter。
4. Android App 只是 Flutter UI + platform adapter。
5. App 内 getter 形态采用嵌入式 Rust library / FFI 风格，不以 daemon 作为主路径。
6. 平台专用 API 通过 Rust-active platform adapter 暴露给 getter/native bridge；Rust 定义接口并主动调用 Android 实现，Android/Kotlin 只提供平台事实。
7. 后端存储使用 SQLite。
8. 用户通过非标准方式改坏 backend storage 时，getter fail fast 报错，不提供复杂恢复引导。
9. 用户二次开发采用 patch stack/source fork 模式，不设计复杂 runtime customization/plugin 系统。
10. 旧数据迁移必须对普通用户无感自动完成，同时可提供手动导入。

### 3.2 顶层结构

目标结构：

```text
Flutter APP
  - UI rendering
  - navigation
  - Android permission prompts
  - user confirmation flows
  - render getter/platform DTOs
        |
        | FFI / native bridge boundary
        v
Rust getter core + native bridge
  - Rust-active platform adapter interface
  - Android PackageManager inventory calls through platform adapter
  - installer adapter handoff
  - notification adapter handoff
  - SAF/file picker/URI permission handoff
  - app/package model
  - repository/overlay resolution
  - Lua package evaluation
  - update discovery/select/resolve
  - provider/source backends
  - download task state machine
  - SQLite main DB
  - cache DB
  - legacy migration
  - CLI API
```

### 3.3 Getter 必须拥有的能力

必须进入 getter core：

- App/package identity。
- Repository/overlay 管理。
- Lua package loading/evaluation。
- Package update lifecycle。
- Provider/source backend。
- Version parsing/comparison/filtering。
- Release/artifact normalization。
- Update status calculation。
- Download request/action generation。
- Download task state machine。
- Main SQLite storage。
- Cache DB。
- Legacy migration/import。
- Event stream。
- CLI API。
- Diagnostics/error reporting。

### 3.4 APP/platform adapter 保留的能力

保留在 Flutter/Android adapter：

- Android PackageManager installed app scanning exposed as raw facts through the Rust-active platform adapter (ADR-0009)。
- Android installed version lookup。
- APK install / package installer / Shizuku/root installer。
- Android permission request。
- Notification / foreground service integration。
- SAF/file picker/URI permission。
- Activity/UI navigation。
- Android-specific file opening intents。
- Theme/localization/user-facing UI preferences。

---

## 4. Package-centric 模型

### 4.1 Package path

Package 主身份使用 UpgradeAll 自己的可读 repository-local package path，不使用 UUID 作为主身份，也不在 Lua table 里重复声明 `id` 字段。

示例：

```text
android/app/org.fdroid.fdroid
android/app/com.termux
android/magisk/zygisk-next
generic/tool/example-tool
```

设计理由：

- UUID 对用户无意义。
- package path 应可读、可 diff、可手写、可在 issue/文档中引用。
- Android 和 Magisk 迁移可以自然映射。

旧数据映射：

- 旧 Android app：`android/app/<packageName>`。
- 旧 Magisk module：`android/magisk/<moduleId>`。

### 4.2 APP/package-centric，而不是 hub-centric

用户界面和 getter 的用户可见概念应围绕 App/package，而不是 Hub。

旧 Hub 的概念拆分为：

- repository：一组 package Lua 文件和 reusable modules。
- provider/source：GitHub、F-Droid、Google Play、CoolApk 等访问后端。
- package：一个可维护更新单元。
- installed target：本机安装对象，如 Android package 或 Magisk module。
- user state：enabled、ignore、source priority、favorite、overrides 等用户状态。

CLI/UI 命名建议：

- UI：Apps / Modules / Repositories / Sources。
- CLI：可以使用 `getter app ...` 面向用户。
- Rust 内部：使用 `Package` / `ResolvedPackage`。

### 4.3 多来源同一 package

同一个 Android App 如果可来自 F-Droid、GitHub、Google Play，它应是同一个 package 的多个 source/provider，而不是多个 package。

例如：

```lua
#!/bin/upa-lua v1
-- repo/official/android/app/org.fdroid.fdroid/9999.lua
-- package path: android/app/org.fdroid.fdroid
return android_app {
  installed = android.package("org.fdroid.fdroid"),
  sources = {
    fdroid.package { package_name = "org.fdroid.fdroid" },
    github.release { repo = "f-droid/fdroidclient" },
  },
}
```

For F-Droid sources, display metadata such as app name/description comes from the self-describing F-Droid catalog rather than duplicated generated Lua fields.

source priority 可以来自 package 默认值，也可以被 user state 覆盖。

---

## 5. Repository / overlay 模型

### 5.1 Repository 类型

新架构使用 repository/overlay 模型，参考 Portage/emerge 的 overlay 思路。

Repository 可以是：

- official：官方包定义仓库。
- community：社区包定义仓库。
- autogen：默认的自动生成包仓库。
- local：用户手写/覆盖仓库。

### 5.2 Priority 规则

优先级规则：

- 数字越大优先级越高。
- getter resolved view 只看最高优先级 package。
- 用户可以通过 `repo/metadata.jsonc`、UI 或 CLI 修改 repo priority。

默认建议：

```text
local       100   用户手写覆盖，默认最高
official      0   官方仓库
community     0   或用户配置
autogen      -1   根据已安装应用/显式 autogen 生成的 fallback
```

`repo/metadata.jsonc` 还可以包含 `generated_repository`，默认值是 `autogen`。初始配置文件应把这个默认值写成注释，用户可取消注释后改成其它已有 alias。实际运行 autogen 时，如果目标是默认 `autogen` 且 `repo/autogen/` 不存在，getter 创建它；如果用户配置的是非 `autogen` alias，则目标目录必须已经存在，否则 autogen apply 报配置错误。`generated_repository` 只决定 autogen 输出目标，package resolution 仍然只看 priority。

注意：`local` 只是默认最高，用户可以自己改优先级。

### 5.3 local 与 autogen 的区别

`local`：

- 用户手写/编辑。
- 用于明确覆盖上游 package。
- 默认 priority 最高。
- 普通清理按钮不应删除 `local`。

`autogen` / configured generated repository：

- 用户点击“从已安装应用生成”或显式选择 provider autogen 后产生。
- 是低优先级 fallback。
- 上游 official package 出现后，official 会覆盖它。
- 清理按钮只作用于 configured generated repository。

### 5.4 首次旧数据迁移与 autogen 的区别

旧数据迁移是特殊情况：

- 首启迁移必须无感。
- 迁移可以一次性生成 `local` package 文件，以保留用户旧配置。
- 该行为只发生一次。

普通 installed autogen：

- 是用户主动点击按钮触发。
- 生成到 `generated_repository` 指定的仓库，默认 `autogen`。
- 不是首启迁移的一部分。

---

## 6. Repository 文件布局

Accepted layout now has a getter data directory with `repo/` and `rc/` as siblings:

```text
<data-dir>/
  main.db
  cache.db
  repo/
    metadata.jsonc
    official/
      .metadata/
        metadata.jsonc
        autogen/
          metadata.jsonc
          android.lua
      luaclass/
        github_android_apk.lua
        fdroid_android.lua
      android/
        app/
          org.fdroid.fdroid/
            metadata.jsonc
            Manifest
            1.20.0.lua
            9999.lua
            files/
              helper-data.json
    autogen/
      android/
        app/
          org.fdroid.fdroid/
            metadata.jsonc
            .autogen.jsonc
            Manifest
            1.20.0.lua
  rc/
    hook/
      10-http-rewrite.lua
```

`repo/metadata.jsonc` is getter-owned local repository registry/config, not publishable repository metadata:

```jsonc
{
  "version": 1,
  // Autogen writes to "autogen" by default. Uncomment and change this
  // if generated packages should target another existing repository alias.
  // "generated_repository": "autogen",
  "priority": {
    "local": 100,
    "official": 0,
    "autogen": -1
  }
}
```

Repository self metadata lives under `repo/<alias>/.metadata/metadata.jsonc`. Shared Lua classes/helpers live under `luaclass/`, not `lib/`. Repository-level autogen scripts/metadata live under `.metadata/autogen/`. Runtime/local policy hooks live under top-level `rc/hook/`, not under `repo/`.

### 6.1 Package directories

A package is a directory that directly contains `metadata.jsonc`. Package identity is derived from the repository-local directory path, for example:

```text
repo/official/android/app/org.fdroid.fdroid/ -> android/app/org.fdroid.fdroid
repo/official/android/magisk/zygisk-next/   -> android/magisk/zygisk-next
```

A package directory contains `metadata.jsonc`, optional generated-package `.autogen.jsonc`, optional `Manifest`, direct child version scripts such as `1.20.0.lua` or `9999.lua`, and optional package-local helper files under `files/`. There is no `versions/` subdirectory. Lua package files do not declare a duplicate package id; getter derives identity from the package directory path.

### 6.2 luaclass/

`luaclass/` contains reusable Lua modules/classes.

注意：这里的角色类似 Gentoo eclass，但项目语法里不需要真的叫 eclass。

原则：

- 不限定 helper 里写什么。
- 只抽象重复代码。
- 可以提供高层 helper，例如 `github_android_apk { ... }` 或 `fdroid.package { ... }`。
- package 文件通过 Lua `require()` 导入。

示例：

```lua
local github_android = require("luaclass.github_android_apk")
```

### 6.3 Repository autogen scripts

`.metadata/autogen/` contains repository-level Lua generators/templates for producing package directories from installed inventory or structured provider/catalog input. Generated package output is ordinary package directories plus a package-local `.autogen.jsonc` ownership record, not a repo-level generation table.

Example generated package output:

```text
repo/autogen/android/app/org.fdroid.fdroid/
  metadata.jsonc
  .autogen.jsonc
  Manifest
  1.20.0.lua
```

`.autogen.jsonc` records generator identity, input facts, generated file hashes, and cleanup/refresh ownership state. It is not security trust and does not replace package `Manifest`.

---

## 7. Lua package API

### 7.1 语言选择

内嵌语言：Lua。

优先实现：`mlua`。

理由：

- Rust 集成成熟。
- 语言小，适合作为嵌入式脚本。
- 支持 metatable，可实现继承/override/object helper。
- 适合 ebuild/eclass-like 的可编程 package definition。
- AI 和用户都比较容易读写。

### 7.2 不发明自定义语法

原则：

- 尽可能使用 Lua 原生语法。
- 不维护复杂自定义语法。
- 不引入新的 DSL parser。
- package override/object 行为用 Lua table/metatable/helper 实现。

### 7.3 Parent package import

父包导入使用 host helper：

```lua
local base = package_from("official", "android/app/org.fdroid.fdroid")
```

理由：

- package path 里有 `/`、`.`、`-` 等字符。
- Lua 原生 `require()` 会把 `.` 当模块路径分隔。
- parent package import may use an explicit repository alias to avoid priority/recursion ambiguity.
- 这是 host function，不是新语法。

Reusable module 仍使用 Lua `require()`：

```lua
local github = require("luaclass.github")
```

### 7.4 Lua/Rust boundary / Lua/Rust 边界

Lua package scripts 在边界返回 JSON-like object/table。

原则：

- Lua↔Rust crossing 视为 RPC/serialization boundary。
- Lua 返回 plain data。
- Rust validate/deserialize 成 typed structs。
- 如果 mlua 能直接把 Lua table 映射到 Rust struct，可以作为实现细节。
- 概念上不暴露可变 Rust domain object 给 Lua。

好处：

- Lua API 简单。
- cache/debug 输出可检查。
- 不绑定 Rust 内部对象生命周期。
- 错误模型清晰。

错误分层：

1. Lua runtime error：脚本执行失败。
2. Schema validation error：Lua 返回 table，但字段不符合 schema。
3. Domain error：schema 合法，但语义不成立。

### 7.5 Package 文件示例

官方 package：

```lua
#!/bin/upa-lua v1
-- repo/official/android/app/org.fdroid.fdroid/9999.lua
-- package path: android/app/org.fdroid.fdroid
local github_android = require("luaclass.github_android_apk")

return github_android.package {
  name = "F-Droid",
  android_package = "org.fdroid.fdroid",
  owner = "f-droid",
  repo = "fdroidclient",
  asset = {
    include = "[.]apk$",
  },
}
```

本地 override：

```lua
local base = package_from("official", "android/org.fdroid.fdroid")

return base:override(function(pkg)
  pkg.name = "F-Droid Custom"
  pkg.source_priority = { "github", "fdroid" }

  local parent_select = pkg.select
  function pkg:select(ctx, candidates, installed, user_state)
    local selected = parent_select(self, ctx, candidates, installed, user_state)
    selected.channel = "custom"
    return selected
  end
end)
```

---

## 8. Override API

### 8.1 为什么需要 override helper

用户如果想修改上游 package，不应复制整个上游文件。

目标：

- 用户可以引用父包。
- 用户只改需要改的字段或 hook。
- 上游更新时，用户 patch 尽量不冲突。

### 8.2 Table override

适合简单字段替换：

```lua
local base = package_from("official", "android/org.fdroid.fdroid")

return base:override {
  name = "F-Droid Custom",
  source_priority = { "github", "fdroid" },
}
```

语义：

- getter/lib 克隆 base package。
- 表中出现的字段替换父字段。
- 简单、直观。
- 不适合复杂函数覆写。

### 8.3 Function override

适合复杂逻辑：

```lua
local base = package_from("official", "android/org.fdroid.fdroid")

return base:override(function(pkg)
  pkg.name = "F-Droid Custom"

  local parent_select = pkg.select
  function pkg:select(ctx, candidates, installed, user_state)
    local selected = parent_select(self, ctx, candidates, installed, user_state)
    selected.channel = "custom"
    return selected
  end
end)
```

语义：

- getter/lib 克隆 base package。
- 用户函数修改 clone。
- 可以替换字段，也可以替换 hook。
- 可以调用父函数。

### 8.4 推荐策略

建议同时支持 table override 和 function override。

文档推荐：

- 简单 metadata 修改用 table override。
- 非平凡修改用 function override。

注意：override helper 是 Lua helper/module 问题，不是 Rust API 问题。Rust 只关心最终返回的 JSON-like package object 是否符合 schema。

---

## 9. Package lifecycle phases

### 9.1 参考 emerge，但不照搬

Gentoo ebuild phase 包括：

```text
pkg_pretend
pkg_setup
src_unpack
src_prepare
src_configure
src_compile
src_test
src_install
pkg_preinst
pkg_postinst
```

UpgradeAll 不是源码编译系统，因此不复制 `src_compile/src_install` 这些名字。

参考点是：

- package 文件提供一组生命周期 hook。
- 默认 hook 由 reusable module 提供。
- package 可以 override hook。
- getter 按固定顺序执行。

### 9.2 新 phase 名称

采用 app-centric 命名：

```text
preflight
setup
match
discover
prepare
select
<resolve or make_actions>
post_update
```

`plan` 这个名字过于模糊，已拒绝。

推荐替代：

- `resolve`：把 selected candidate 解析成可执行 actions。
- `make_actions`：更直白，返回 action list。

目前建议：`resolve`。

### 9.3 Phase 语义

#### preflight(ctx)

用途：

- 预检查。
- 检查平台是否支持。
- 检查权限声明。
- 检查 provider/backend 可用性。
- 检查明显不兼容的 user state。

参考 Gentoo：`pkg_pretend`。

#### setup(ctx)

用途：

- 初始化 package evaluation。
- 解析 provider config。
- 检查 auth 是否存在。
- 确定默认 source priority。

参考 Gentoo：`pkg_setup`。

#### match(ctx, installed_item)

用途：

- 判断一个 installed inventory item 是否匹配本 package。
- 替代旧 `checkAppAvailable` 的一部分语义。

#### discover(ctx)

用途：

- 查询 provider/source。
- 返回 release candidates。

替代旧：

- `getAppReleaseList`
- `getAppUpdate`

#### prepare(ctx, candidates)

用途：

- 将 provider-specific release 规范化为 canonical candidates。
- 过滤 prerelease。
- 过滤 arch/variant。
- 提取/规范化 version。
- 处理 changelog/asset metadata。

参考 Gentoo：`src_prepare`。

#### select(ctx, candidates, installed, user_state)

用途：

- 从 candidates 中选择应更新的版本和 artifact。
- 应用 version compare。
- 应用 ignore/pin/source priority。

#### resolve(ctx, selected)

用途：

- 将 selected candidate 转成可执行动作。
- 返回 DownloadRequest / InstallAction / warnings。

示例输出：

```lua
return {
  actions = {
    {
      type = "download",
      url = selected.artifact.url,
      file_name = selected.artifact.name,
      headers = {},
    },
    {
      type = "install",
      installer = "android_package",
      file = selected.artifact.name,
    },
  },
  warnings = {},
}
```

#### post_update(ctx, result)

用途：

- 可选的更新后 message / metadata。
- 应尽量少用。
- 大部分状态变更应由 Rust core 处理。

---

## 10. Permissions / network model

### 10.1 默认无 Lua 原生网络

默认情况下，Lua package script 不获得 Lua 标准库/第三方库形式的直接网络能力。

网络请求通过 getter 暴露的 host API 执行，例如：

```lua
local body = http_get(url, {
  headers = { Accept = "application/json" },
  cache = true,
})
```

`cache` 默认是 `false`。普通 package evaluation 不默认安装 `http_get` 或 provider host API；需要 provider/network 的 getter operation/runtime 必须显式安装 transport/provider host functions，并由 getter 拥有 permission、Manifest、provider、cache、diagnostic policy。Generic/custom Lua 通过 `cache = true` 主动把单次 HTTP 请求纳入 getter-owned HTTP/source cache；getter 负责 cache key、持久化、revalidation、stale diagnostics 和 secret redaction，Lua 只表达该请求是否应缓存。v1 generic HTTP 请求形状保持很小：URL string，加可选 options table，其中只接受 string-to-string `headers` 与 boolean `cache`。

标准 provider module/class 默认调用 provider-specific host API，例如 `getter.provider.fdroid.update_candidates(...)` 和 `getter.provider.github.release_candidates(...)`，由 Rust getter provider operations 负责 release/catalog 获取、解析、cache provenance、diagnostics 和 candidate normalization，而不是在 Lua 中直接用 `http_get` 解析 provider payload。

### 10.2 自由网络权限

如果 package 需要超出标准 provider module 的任意 upstream 访问，它必须声明自由网络权限，getter 才向该 Lua 环境暴露对应 host HTTP 能力。

该权限用于类似 live/9999 包或特殊 upstream 逻辑。

UI 行为：

- 在 App detail 的 source/version 层显示黄色 warning tag。
- 该 tag 只提示，不阻止使用。

### 10.3 不做脚本超时

不对 Lua 脚本本身设置 runtime timeout/fuel limit。

理由：

- 停机问题无法一般解决。
- 脚本速度受本地机器、网络、provider 等影响。
- 网络操作使用正常 network timeout。

### 10.4 v1 暂不强制校验

v1 暂不做 repo/script/artifact 强校验。

理由：

- 先信任 Git 仓库。
- 校验系统会显著增加复杂度。
- 可以先保留 schema 字段，后续再 enforce。

---

## 11. Storage model

### 11.1 Main SQLite DB

主 DB 存储权威用户状态和 getter 状态。

建议内容：

- repositories registry。
- repo priority。
- enabled apps/packages。
- user source priority override。
- legacy ignore/mark version state mapped into `pin_version`.
- pins / version baselines。
- favorites/star。
- migration records。
- settings。
- credentials references。
- later ADR-accepted operation-specific durable records; ADR-0011 keeps runtime task state process-memory only and excludes it from main/cache DB persistence。

### 11.2 Cache DB

缓存 DB 单独文件，不与主 DB 混用。

缓存内容：

- evaluated package metadata。
- version/release candidates。
- selected latest version。
- asset metadata。
- provider response cache。
- search index。
- validation result。

Cache key 应包含：

```text
repository alias and verified repository metadata/revision facts
package path and Lua dependency/file hashes
Lua API version
getter version or package API version
platform target
permissions/network mode
```

### 11.3 Repo files

package Lua source files 存在本地文件夹中。

SQLite 只记录 repo registry/path/revision/priority 等元信息。

Android 上 repo sync 可以先采用 archive zip/tar 或 bundled repo snapshot，避免直接依赖完整 git CLI。

---

## 12. URL rewrite / bashrc-like hooks

旧 `extra_hub` 的 URL replace 语义保留，但改为全局策略。

要求：

- 是全局的，不散落到每个 source。
- 可按 package/repository scope 区分。
- 参考 emerge bashrc 的精神：全局 hook 根据上下文做调整。

Accepted hook location is top-level runtime config, `rc/hook/*.lua`. Hooks wrap public getter host functions and call original unhooked entrypoints through `getter_builtin.<name>`. Plain package evaluation does not install `http_get`; provider/runtime operations that need network install it deliberately and own permission, Manifest, provider, cache, and diagnostic policy.

示例：

```lua
#!/bin/upa-lua v1
local upstream_http_get = getter_builtin.http_get

function http_get(url, opts)
  local rewritten = url:gsub("https://github.com/", "https://mirror.example/github/")
  return upstream_http_get(rewritten, opts)
end
```

Hooks are loaded before each Lua execution environment in deterministic filename order. Enabled hook load/init failure fails the current Lua execution. Hooks do not bypass Manifest validation: non-`allow_free_network` package scripts still require response-body SHA-512 membership in the package `Manifest`.

---

## 13. Legacy migration

### 13.1 迁移原则

旧数据迁移必须无感自动完成。

但迁移是有限/简单迁移，不追求完整复刻旧语义。

可以丢弃：

- API key。
- auth token。
- 复杂 Hub 配置。
- 无法可靠映射的特殊规则。

必须保留：

- saved apps 的基本 identity。
- Android package / Magisk module installed id。
- legacy ignore version / mark version 能力映射为 `pin_version`，如果可映射。
- user-visible tracked app 列表。
- 常见 source/cloud config 能力，如果可内置转换。

### 13.2 迁移输入

旧 Room DB：

- `app`
- `hub`
- `extra_app`
- `extra_hub`

Room DB 信息：

- name：`app_metadata_database.db`
- version：17
- migrations：6->17

### 13.3 迁移输出

输出到：

- getter main SQLite user state。
- 必要时生成 `local` repo package Lua 文件。

迁移生成 `local` 是特殊情况，只做一次。

普通 installed autogen 不写 `local`，而写 `repo/metadata.jsonc` 的 `generated_repository` 目标，默认 `autogen`。

### 13.4 迁移匹配策略

建议流程：

1. 使用 bundled official repo snapshot 做本地匹配，不依赖首启联网。
2. 能匹配 official package 的旧 App：写入 user state，指向 official package。
3. 不能匹配但常见类型可转换：生成 `local` package Lua。
4. 稀有情况：迁移 installed id list，状态为 missing package，提示用户自己写或提交 issue。
5. 迁移完成后记录 migration_runs。

### 13.5 迁移 UX

- 普通用户无感进入新 App。
- 迁移失败时进入 migration/recovery 页面。
- 单个 package 无法匹配不应阻塞整个 App。
- 该 package 显示 missing/needs package script 状态。

实现进展：Android/Flutter 侧已有 no-UI legacy migration adapter 负责定位、复制并 checkpoint 旧 Room SQLite triplet；Flutter 产品 APK 通过 slim getter/native bridge 调用 Rust `importLegacyRoomDatabase` / `legacyReportList`。Room 表读取、字段映射、migration record、tracked package 写入和 sanitized report 仍由 getter-owned Rust code 完成，Flutter/Kotlin 不解析 Room 行。

---

## 14. Installed autogen UX

### 14.1 生成流程

用户点击“从已安装应用生成”：

1. Flutter 调用 getter/native bridge 的 installed-autogen preview 操作。
2. Rust platform adapter 主动调用 Android PackageManager adapter，取得 installed inventory 原始事实。
3. getter 找出可生成的候选列表；F-Droid 命中的候选由 getter 生成 minimal package directory：`metadata.jsonc`、带 provider source SHA-512 provenance 的 `Manifest`、以及调用 `luaclass.fdroid_android` 的小型 `9999.lua`。
4. UI 展示 getter-owned preview DTO。
5. 用户 yes/no 确认。
6. getter 写入 configured generated repository，默认 `repo/autogen/`。
7. 后续 update check 通过 getter/provider-backed runtime 安装 `getter.provider.*` 后执行生成的 F-Droid Lua；普通 read-model package eval 不作为 provider-module 生成包的验证路径。
8. 生成后不会自动消失。

实现进展：Flutter 产品 APK 通过 `app_flutter/android/getter_bridge` 打包一个 slim native bridge library，包含 Rust `api_proxy`、`NativeLib` 和 Android installed-inventory facts provider。`api_proxy` 已提供 installed-autogen preview/apply JNI entrypoints；它们调用 Rust-active platform adapter 扫描 Android PackageManager 原始事实，再调用 getter-owned `getter-operations` 执行 installed-autogen preview/apply。Flutter 已新增 installed-autogen 页面和 `MethodChannelGetterAdapter`，只渲染 getter-owned preview/apply DTO 并把用户接受的 package path 传回 getter；不能引入 Dart-led installed inventory scanner 或在 Dart/Kotlin 中生成 package path。

### 14.2 清理流程

用户点击“清除不存在的应用”：

1. Flutter 调用 getter/native bridge 的 installed-autogen cleanup preview 操作。
2. Rust platform adapter 主动调用 Android PackageManager adapter，取得当前 installed inventory 原始事实。
3. getter 计算将删除列表。
4. UI 展示 getter-owned preview DTO。
5. 用户 yes/no 确认。
6. getter 清理 configured generated repository 中不再安装且 ownership checks 通过的 generated package directory contents。

普通清理按钮只作用于 configured generated repository，不删除 `local`。

---

## 15. Patch stack / user fork 模型

### 15.1 不设计复杂 runtime customization

决策：用户二次开发采用 patch stack/source fork，不做复杂 runtime plugin/customization 框架。

原因：

- 无法预测用户如何修改软件。
- 为任意 customization 设计稳定 runtime API 会显著拖累兴趣项目维护。
- Flutter 本身不是为了用户 runtime custom UI 设计的。

### 15.2 仍需降低 rebase 成本

参考 Linux kernel 的模块分离思想：

- subsystem 目录清晰。
- API 边界明确。
- generated files 不手改。
- 上游经常变的代码和用户常改代码尽量分离。
- repository/package Lua 文件天然适合 patch stack。

### 15.3 稳定性承诺层级

建议承诺：

- Rust internal API：不稳定。
- Lua package boundary schema：相对稳定。
- ResolvedPackage / UpdateCandidate / UpdateAction schema：稳定。
- Platform RPC API：相对稳定。
- CLI user-facing commands：稳定。
- Individual package Lua scripts：可变。

---

## 16. Flutter APP 边界

Flutter APP 负责：

- Home / App list / App detail / Settings / Log / Migration UI。
- Android platform adapter。
- 展示 getter 状态和事件。
- 用户确认流程，如 autogen list yes/no、cleanup list yes/no。
- 显示 free-network yellow tag。

Flutter APP 不负责：

- provider/source logic。
- package update selection。
- version comparison。
- storage migration。
- download task state machine。
- repository resolution。
- Lua evaluation。

---

## 17. CLI 方向

getter CLI 应围绕 app/package，而不是 hub。

建议命令：

```bash
getter app list
getter app show android/org.fdroid.fdroid
getter app check android/org.fdroid.fdroid
getter app update android/org.fdroid.fdroid
getter app sources android/org.fdroid.fdroid

getter repo list
getter repo sync
getter repo eval official

getter template list
getter template run android_installed_app --input ...

getter storage validate
getter legacy migrate
```

CLI 是验证 getter core 独立性的关键：

如果 CLI 无法完成核心更新流程，说明逻辑仍然泄漏在 Flutter/Android APP 里。

---

## 18. 非目标

v1 非目标：

- 不做复杂 runtime UI customization framework。
- 不做 Wasm plugin runtime。
- 不做完整旧 auth/API key 迁移。
- 不强制 repo/script/artifact 校验。
- 不做 Lua script timeout/fuel limit。
- 不保证任意用户 fork 不冲突。
- 不继续维护旧 hub-app 逻辑模型。

---

## 19. Open questions

仍需决策：

1. `plan` 替代 phase 最终名字：`resolve` 还是 `make_actions`。
2. template conflict policy：目标文件存在时 skip、overwrite、还是询问。
4. repo priority 默认值精确设定。
5. URL rewrite hook 的最终 Lua schema。
6. Android repo sync v1 使用 bundled snapshot、zip/tar archive，还是 git/libgit2。
7. main DB/cache DB 具体 schema。
8. legacy migration 的字段级 mapping。
9. Flutter UI route/page 具体信息架构。
10. provider/source host API 细节。

---

## 20. Documentation policy

从本文开始，UpgradeAll 重构文档采用以下规则：

1. 每个重要架构决策写入 wiki 或 ADR。
2. 每个新模块必须有 README 或 docs section，说明职责和非职责。
3. 每个跨边界 API 必须有 schema 文档。
4. 每个迁移步骤必须有 source/target mapping 文档。
5. 每个 Lua host API 必须有示例。
6. 每个用户可见破坏性行为必须有 UX 说明。
7. 每次设计变更必须更新本文或后续 ADR。

推荐后续文档拆分：

```text
docs/
  architecture/
    upgradeall-getter-rewrite-wiki.md
    adr/
      0001-app-centric-lua-package-repository-model.md
      0002-getter-flutter-platform-boundary.md
      0003-legacy-room-migration.md
      0004-sqlite-main-db-and-cache-db.md
      0005-lua-package-api.md
  lua-api/
    package-lifecycle.md
    repository-layout.md
    templates.md
    permissions.md
  migration/
    legacy-room-mapping.md
  app/
    flutter-ui-feature-parity.md
```
