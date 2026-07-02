import 'dart:async';

import 'package:flutter/material.dart';

import 'getter_adapter.dart';
import 'legacy_migration_platform.dart';
import 'native_getter_adapter.dart';

void main() {
  runApp(
    const UpgradeAllApp(
      getter: MethodChannelGetterAdapter(),
      legacyMigrationPlatform: MethodChannelLegacyMigrationPlatform(),
    ),
  );
}

@visibleForTesting
class AppKeys {
  static const homeRoute = ValueKey<String>('route.home');
  static const appsRoute = ValueKey<String>('route.apps');
  static const appDetailRoute = ValueKey<String>('route.app_detail');
  static const repositoriesRoute = ValueKey<String>('route.repositories');
  static const downloadsRoute = ValueKey<String>('route.downloads');
  static const logsRoute = ValueKey<String>('route.logs');
  static const settingsRoute = ValueKey<String>('route.settings');
  static const migrationRoute = ValueKey<String>('route.migration');
  static const installedAutogenRoute = ValueKey<String>(
    'route.installed_autogen',
  );

  static const openApps = ValueKey<String>('action.open_apps');
  static const openRepositories = ValueKey<String>('action.open_repositories');
  static const openDownloads = ValueKey<String>('action.open_downloads');
  static const openLogs = ValueKey<String>('action.open_logs');
  static const openSettings = ValueKey<String>('action.open_settings');
  static const openMigration = ValueKey<String>('action.open_migration');
  static const openInstalledAutogen = ValueKey<String>(
    'action.open_installed_autogen',
  );
  static const openFirstApp = ValueKey<String>('action.open_first_app');
  static const startLegacyMigration = ValueKey<String>(
    'action.start_legacy_migration',
  );
  static const previewInstalledAutogen = ValueKey<String>(
    'action.preview_installed_autogen',
  );
  static const refreshDefaultFdroidCatalogCache = ValueKey<String>(
    'action.refresh_default_fdroid_catalog_cache',
  );
  static const previewInstalledFdroidAutogen = ValueKey<String>(
    'action.preview_installed_fdroid_autogen',
  );
  static const previewGithubAutogen = ValueKey<String>(
    'action.preview_github_autogen',
  );
  static const applyInstalledAutogen = ValueKey<String>(
    'action.apply_installed_autogen',
  );
  static const installedAutogenConfirmDialog = ValueKey<String>(
    'dialog.installed_autogen_confirm',
  );
  static const cancelInstalledAutogenApply = ValueKey<String>(
    'action.cancel_installed_autogen_apply',
  );
  static const confirmInstalledAutogenApply = ValueKey<String>(
    'action.confirm_installed_autogen_apply',
  );
  static const githubAutogenOwnerField = ValueKey<String>(
    'input.github_autogen_owner',
  );
  static const githubAutogenRepoField = ValueKey<String>(
    'input.github_autogen_repo',
  );
  static const githubAutogenAndroidPackageField = ValueKey<String>(
    'input.github_autogen_android_package',
  );
  static const githubAutogenDisplayNameField = ValueKey<String>(
    'input.github_autogen_display_name',
  );
  static const updateCheckStatus = ValueKey<String>(
    'state.update_check_status',
  );
  static const updateCheckError = ValueKey<String>('state.update_check_error');

  static const updateSummary = ValueKey<String>('state.update_summary');
  static const getterStatus = ValueKey<String>('state.getter_status');
  static const appsList = ValueKey<String>('state.apps_list');
  static const repositoriesList = ValueKey<String>('state.repositories_list');
  static const downloadsList = ValueKey<String>('state.downloads_list');
  static const downloadsEmpty = ValueKey<String>('state.downloads_empty');
  static const taskEventsList = ValueKey<String>('state.task_events_list');
  static const logsEmpty = ValueKey<String>('state.logs_empty');
  static const settingsShell = ValueKey<String>('state.settings_shell');
  static const migrationReady = ValueKey<String>('state.migration_ready');
  static const migrationStatus = ValueKey<String>('state.migration_status');
  static const migrationBridgeUnavailable = ValueKey<String>(
    'state.migration_bridge_unavailable',
  );
  static const migrationImported = ValueKey<String>('state.migration_imported');
  static const migrationError = ValueKey<String>('state.migration_error');
  static const migrationReportsList = ValueKey<String>(
    'state.migration_reports_list',
  );
  static const installedAutogenReady = ValueKey<String>(
    'state.installed_autogen_ready',
  );
  static const installedAutogenBridgeUnavailable = ValueKey<String>(
    'state.installed_autogen_bridge_unavailable',
  );
  static const installedAutogenPreview = ValueKey<String>(
    'state.installed_autogen_preview',
  );
  static const installedAutogenCandidatesList = ValueKey<String>(
    'state.installed_autogen_candidates_list',
  );
  static const installedAutogenSkipsList = ValueKey<String>(
    'state.installed_autogen_skips_list',
  );
  static const installedAutogenDiagnosticsList = ValueKey<String>(
    'state.installed_autogen_diagnostics_list',
  );
  static const installedAutogenScanStats = ValueKey<String>(
    'state.installed_autogen_scan_stats',
  );
  static const fdroidCatalogRefreshStatus = ValueKey<String>(
    'state.fdroid_catalog_refresh_status',
  );
  static const fdroidCatalogRefreshDiagnosticsList = ValueKey<String>(
    'state.fdroid_catalog_refresh_diagnostics_list',
  );
  static const installedAutogenApplied = ValueKey<String>(
    'state.installed_autogen_applied',
  );
  static const installedAutogenError = ValueKey<String>(
    'state.installed_autogen_error',
  );

  static ValueKey<String> checkPackageUpdate(String packageId) =>
      ValueKey<String>('action.check_update.$packageId');
  static ValueKey<String> appRow(String packageId) =>
      ValueKey<String>('state.app.$packageId');
  static ValueKey<String> repoRow(String repositoryId) =>
      ValueKey<String>('state.repository.$repositoryId');
  static ValueKey<String> downloadTaskRow(String taskId) =>
      ValueKey<String>('state.download_task.$taskId');
  static ValueKey<String> taskEventRow(int cursor) =>
      ValueKey<String>('state.task_event.$cursor');
  static ValueKey<String> autogenCandidateRow(String packageId) =>
      ValueKey<String>('state.autogen_candidate.$packageId');
  static ValueKey<String> autogenConfirmCandidateRow(String packageId) =>
      ValueKey<String>('state.autogen_confirm_candidate.$packageId');
  static ValueKey<String> autogenSkipRow(String packageId) =>
      ValueKey<String>('state.autogen_skip.$packageId');
  static ValueKey<String> autogenDiagnosticRow(int index) =>
      ValueKey<String>('state.autogen_diagnostic.$index');
  static ValueKey<String> fdroidCatalogRefreshDiagnosticRow(int index) =>
      ValueKey<String>('state.fdroid_catalog_refresh_diagnostic.$index');
  static ValueKey<String> autogenAppliedRow(String packageId) =>
      ValueKey<String>('state.autogen_applied.$packageId');
}

class UpgradeAllApp extends StatelessWidget {
  const UpgradeAllApp({
    super.key,
    this.getter = const FakeGetterAdapter(),
    this.legacyMigrationPlatform = const NoopLegacyMigrationPlatform(),
  });

  final GetterAdapter getter;
  final LegacyMigrationPlatform legacyMigrationPlatform;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'UpgradeAll',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
        useMaterial3: true,
      ),
      routes: <String, WidgetBuilder>{
        '/': (context) => HomePage(getter: getter),
        '/apps': (context) => AppsPage(getter: getter),
        '/repositories': (context) => RepositoriesPage(getter: getter),
        '/downloads': (context) => DownloadsPage(getter: getter),
        '/logs': (context) => const LogsPage(),
        '/settings': (context) => const SettingsPage(),
        '/migration': (context) => MigrationPage(
          getter: getter,
          legacyMigrationPlatform: legacyMigrationPlatform,
        ),
        '/autogen': (context) => InstalledAutogenPage(getter: getter),
      },
      onGenerateRoute: (settings) {
        if (settings.name == '/apps/detail') {
          final app = settings.arguments! as AppSummary;
          return MaterialPageRoute<void>(
            builder: (context) => AppDetailPage(app: app, getter: getter),
            settings: settings,
          );
        }
        return null;
      },
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key, required this.getter});

  final GetterAdapter getter;

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  late final Future<GetterSnapshot> _snapshot = widget.getter.loadSnapshot();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      key: AppKeys.homeRoute,
      appBar: AppBar(title: const Text('UpgradeAll')),
      body: FutureBuilder<GetterSnapshot>(
        future: _snapshot,
        builder: (context, snapshot) {
          final data = snapshot.data;
          return ListView(
            padding: const EdgeInsets.all(16),
            children: <Widget>[
              Card(
                key: AppKeys.updateSummary,
                child: ListTile(
                  title: const Text('Updates'),
                  subtitle: Text('${data?.updateCount ?? 0} updates available'),
                ),
              ),
              Card(
                key: AppKeys.getterStatus,
                child: ListTile(
                  title: const Text('Getter core'),
                  subtitle: Text(
                    snapshot.hasError
                        ? 'Getter snapshot unavailable'
                        : data?.status ?? 'Loading getter snapshot...',
                  ),
                ),
              ),
              const SizedBox(height: 16),
              const _RouteButton(
                key: AppKeys.openApps,
                icon: Icons.apps,
                label: 'Apps',
                routeName: '/apps',
              ),
              const _RouteButton(
                key: AppKeys.openRepositories,
                icon: Icons.source,
                label: 'Repositories',
                routeName: '/repositories',
              ),
              const _RouteButton(
                key: AppKeys.openDownloads,
                icon: Icons.download,
                label: 'Downloads',
                routeName: '/downloads',
              ),
              const _RouteButton(
                key: AppKeys.openLogs,
                icon: Icons.receipt_long,
                label: 'Logs',
                routeName: '/logs',
              ),
              const _RouteButton(
                key: AppKeys.openSettings,
                icon: Icons.settings,
                label: 'Settings',
                routeName: '/settings',
              ),
              const _RouteButton(
                key: AppKeys.openMigration,
                icon: Icons.move_down,
                label: 'Legacy migration',
                routeName: '/migration',
              ),
              const _RouteButton(
                key: AppKeys.openInstalledAutogen,
                icon: Icons.auto_fix_high,
                label: 'Installed autogen',
                routeName: '/autogen',
              ),
            ],
          );
        },
      ),
    );
  }
}

class AppsPage extends StatefulWidget {
  const AppsPage({super.key, required this.getter});

  final GetterAdapter getter;

  @override
  State<AppsPage> createState() => _AppsPageState();
}

class _AppsPageState extends State<AppsPage> {
  late final Future<GetterSnapshot> _snapshot = widget.getter.loadSnapshot();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      key: AppKeys.appsRoute,
      appBar: AppBar(title: const Text('Apps')),
      body: FutureBuilder<GetterSnapshot>(
        future: _snapshot,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Center(child: Text('Loading getter apps...'));
          }
          if (snapshot.hasError) {
            return const Center(child: Text('Getter apps unavailable'));
          }
          final apps = snapshot.data?.apps ?? const <AppSummary>[];
          return ListView.builder(
            key: AppKeys.appsList,
            itemCount: apps.length,
            itemBuilder: (context, index) {
              final app = apps[index];
              return ListTile(
                key: AppKeys.appRow(app.id),
                title: Text(app.name),
                subtitle: Text('${app.id} • ${app.installedVersion}'),
                trailing: app.hasFreeNetworkWarning
                    ? const Chip(
                        label: Text('Network'),
                        backgroundColor: Colors.amber,
                      )
                    : null,
                onTap: () {
                  Navigator.of(
                    context,
                  ).pushNamed('/apps/detail', arguments: app);
                },
              );
            },
          );
        },
      ),
    );
  }
}

class AppDetailPage extends StatefulWidget {
  const AppDetailPage({super.key, required this.app, required this.getter});

  final AppSummary app;
  final GetterAdapter getter;

  @override
  State<AppDetailPage> createState() => _AppDetailPageState();
}

class _AppDetailPageState extends State<AppDetailPage> {
  bool _checkingUpdate = false;
  String? _status;
  String? _error;

  Future<void> _checkForUpdate() async {
    if (_checkingUpdate) return;
    setState(() {
      _checkingUpdate = true;
      _status = 'Checking for updates...';
      _error = null;
    });

    try {
      final result = await widget.getter.checkPackageForUpdate(
        widget.app.id,
        installedVersion: _knownVersion(widget.app.installedVersion),
      );
      final action = result.action;
      if (action == null) {
        if (!mounted) return;
        setState(() {
          _status = 'No update task available: ${result.update.status}';
        });
        return;
      }

      final task = await widget.getter.submitRuntimeAction(action.actionId);
      if (!mounted) return;
      setState(() {
        _status = 'Submitted runtime task ${task.taskId}';
      });
      await Navigator.of(context).pushNamed('/downloads');
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _status = null;
        _error = error.toString();
      });
    } finally {
      if (mounted) {
        setState(() {
          _checkingUpdate = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final app = widget.app;
    return Scaffold(
      key: AppKeys.appDetailRoute,
      appBar: AppBar(title: Text(app.name)),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: <Widget>[
          Text(app.id, style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 12),
          Text('Installed: ${app.installedVersion}'),
          Text('Latest: ${app.latestVersion}'),
          const SizedBox(height: 16),
          FilledButton.icon(
            key: AppKeys.checkPackageUpdate(app.id),
            onPressed: _checkingUpdate ? null : _checkForUpdate,
            icon: const Icon(Icons.system_update_alt),
            label: Text(
              _checkingUpdate ? 'Checking update...' : 'Check update',
            ),
          ),
          if (_status != null)
            Padding(
              padding: const EdgeInsets.only(top: 12),
              child: Text(key: AppKeys.updateCheckStatus, _status!),
            ),
          if (_error != null)
            Padding(
              padding: const EdgeInsets.only(top: 12),
              child: Text(key: AppKeys.updateCheckError, _error!),
            ),
          if (app.hasFreeNetworkWarning)
            const Padding(
              padding: EdgeInsets.only(top: 12),
              child: Chip(
                label: Text('Network access required'),
                backgroundColor: Colors.amber,
              ),
            ),
        ],
      ),
    );
  }
}

String? _knownVersion(String version) {
  final normalized = version.trim();
  return normalized.isEmpty || normalized == 'unknown' ? null : normalized;
}

class RepositoriesPage extends StatefulWidget {
  const RepositoriesPage({super.key, required this.getter});

  final GetterAdapter getter;

  @override
  State<RepositoriesPage> createState() => _RepositoriesPageState();
}

class _RepositoriesPageState extends State<RepositoriesPage> {
  late final Future<GetterSnapshot> _snapshot = widget.getter.loadSnapshot();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      key: AppKeys.repositoriesRoute,
      appBar: AppBar(title: const Text('Repositories')),
      body: FutureBuilder<GetterSnapshot>(
        future: _snapshot,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Center(child: Text('Loading getter repositories...'));
          }
          if (snapshot.hasError) {
            return const Center(child: Text('Getter repositories unavailable'));
          }
          final repositories =
              snapshot.data?.repositories ?? const <RepositorySummary>[];
          return ListView.builder(
            key: AppKeys.repositoriesList,
            itemCount: repositories.length,
            itemBuilder: (context, index) {
              final repository = repositories[index];
              return ListTile(
                key: AppKeys.repoRow(repository.id),
                title: Text(repository.id),
                subtitle: Text('Priority ${repository.priority}'),
              );
            },
          );
        },
      ),
    );
  }
}

class DownloadsPage extends StatefulWidget {
  const DownloadsPage({super.key, required this.getter});

  final GetterAdapter getter;

  @override
  State<DownloadsPage> createState() => _DownloadsPageState();
}

class _DownloadsPageState extends State<DownloadsPage> {
  late Future<List<RuntimeTaskSnapshot>> _tasks = widget.getter
      .listRuntimeTasks();
  StreamSubscription<RuntimeNotificationEnvelope>? _notificationSubscription;

  @override
  void initState() {
    super.initState();
    _notificationSubscription = widget.getter
        .runtimeNotificationEnvelopes()
        .listen((notification) {
          if (notification.kind == 'task_changed') {
            _reloadTasks();
          }
        }, onError: (_) {});
  }

  @override
  void dispose() {
    _notificationSubscription?.cancel();
    super.dispose();
  }

  void _reloadTasks() {
    if (!mounted) return;
    setState(() {
      _tasks = widget.getter.listRuntimeTasks();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      key: AppKeys.downloadsRoute,
      appBar: AppBar(title: const Text('Downloads')),
      body: FutureBuilder<List<RuntimeTaskSnapshot>>(
        future: _tasks,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return const Center(
              child: Text(
                key: AppKeys.downloadsEmpty,
                'Runtime tasks unavailable',
              ),
            );
          }
          final tasks = snapshot.data ?? const <RuntimeTaskSnapshot>[];
          if (tasks.isEmpty) {
            return const Center(
              child: Text(key: AppKeys.downloadsEmpty, 'No runtime tasks yet'),
            );
          }
          return ListView.builder(
            key: AppKeys.downloadsList,
            padding: const EdgeInsets.all(16),
            itemCount: tasks.length,
            itemBuilder: (context, index) {
              final task = tasks[index];
              return Card(
                child: ListTile(
                  key: AppKeys.downloadTaskRow(task.taskId),
                  title: Text(task.packageId),
                  subtitle: Text(_runtimeTaskSubtitle(task)),
                  trailing: _TaskCapabilitiesChips(
                    capabilities: task.capabilities,
                  ),
                ),
              );
            },
          );
        },
      ),
    );
  }
}

String _runtimeTaskSubtitle(RuntimeTaskSnapshot task) {
  final parts = <String>[task.status, task.phase.category];
  final downloaded = task.downloadedFile;
  if (downloaded != null) {
    parts.add('${downloaded.fileName} (${downloaded.sizeBytes} bytes)');
  } else {
    final progress = task.progress;
    if (progress != null) {
      final total = progress.total;
      parts.add(
        total == null
            ? '${progress.current} ${progress.unit}'
            : '${progress.current}/$total ${progress.unit}',
      );
    }
  }
  return parts.join(' • ');
}

class _TaskCapabilitiesChips extends StatelessWidget {
  const _TaskCapabilitiesChips({required this.capabilities});

  final RuntimeTaskCapabilities capabilities;

  @override
  Widget build(BuildContext context) {
    final labels = <String>[
      if (capabilities.cancel) 'Cancel',
      if (capabilities.pause) 'Pause',
      if (capabilities.resume) 'Resume',
      if (capabilities.retry) 'Retry',
    ];
    if (labels.isEmpty) return const SizedBox.shrink();
    return Wrap(
      spacing: 4,
      children: labels.map((label) => Chip(label: Text(label))).toList(),
    );
  }
}

class InstalledAutogenPage extends StatefulWidget {
  const InstalledAutogenPage({super.key, required this.getter});

  final GetterAdapter getter;

  @override
  State<InstalledAutogenPage> createState() => _InstalledAutogenPageState();
}

enum _AutogenApplyTarget { installed, installedFdroid, github }

class _InstalledAutogenPageState extends State<InstalledAutogenPage> {
  InstalledAutogenPreview? _preview;
  FdroidCatalogCacheRefreshResult? _fdroidRefresh;
  InstalledAutogenApplyResult? _applyResult;
  GetterError? _error;
  bool _running = false;
  _AutogenApplyTarget _applyTarget = _AutogenApplyTarget.installed;
  String _githubOwner = '';
  String _githubRepo = '';
  String _githubAndroidPackage = '';
  String _githubDisplayName = '';

  Future<void> _refreshDefaultFdroidCatalogCache() async {
    setState(() {
      _running = true;
      _preview = null;
      _fdroidRefresh = null;
      _applyResult = null;
      _error = null;
    });
    try {
      final refresh = await widget.getter.refreshDefaultFdroidCatalogCache();
      if (!mounted) return;
      setState(() {
        _fdroidRefresh = refresh;
        _running = false;
      });
    } on GetterBridgeException catch (error) {
      if (!mounted) return;
      setState(() {
        _error = error.error;
        _running = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _error = GetterError(
          code: 'bridge.fdroid_catalog_refresh_error',
          message: 'F-Droid catalog refresh bridge failed',
          detail: error.toString(),
        );
        _running = false;
      });
    }
  }

  Future<void> _previewInstalledAutogen() {
    return _runPreview(
      () => widget.getter.previewInstalledAutogen(),
      applyTarget: _AutogenApplyTarget.installed,
    );
  }

  Future<void> _previewInstalledFdroidAutogen() {
    return _runPreview(
      () => widget.getter.previewInstalledFdroidAutogen(),
      applyTarget: _AutogenApplyTarget.installedFdroid,
    );
  }

  Future<void> _previewGithubAutogen() {
    final owner = _githubOwner.trim();
    final repo = _githubRepo.trim();
    final androidPackage = _githubAndroidPackage.trim();
    final displayName = _githubDisplayName.trim();
    if (owner.isEmpty || repo.isEmpty || androidPackage.isEmpty) {
      setState(() {
        _preview = null;
        _applyResult = null;
        _error = const GetterError(
          code: 'bridge.github_autogen_input_error',
          message: 'GitHub autogen input is incomplete',
          detail: 'Owner, repository, and Android package are required',
        );
      });
      return Future<void>.value();
    }
    return _runPreview(
      () => widget.getter.previewGithubAutogen(
        GithubAutogenPreviewInput(
          owner: owner,
          repo: repo,
          androidPackage: androidPackage,
          displayName: displayName.isEmpty ? null : displayName,
        ),
      ),
      applyTarget: _AutogenApplyTarget.github,
    );
  }

  Future<void> _runPreview(
    Future<InstalledAutogenPreview> Function() previewer, {
    required _AutogenApplyTarget applyTarget,
  }) async {
    setState(() {
      _running = true;
      _preview = null;
      _applyTarget = applyTarget;
      _error = null;
      _applyResult = null;
    });
    try {
      final preview = await previewer();
      if (!mounted) return;
      setState(() {
        _preview = preview;
        _applyTarget = applyTarget;
        _running = false;
      });
    } on GetterBridgeException catch (error) {
      if (!mounted) return;
      setState(() {
        _error = error.error;
        _running = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _error = GetterError(
          code: 'bridge.installed_autogen_error',
          message: 'Installed autogen bridge failed',
          detail: error.toString(),
        );
        _running = false;
      });
    }
  }

  Future<void> _confirmAndApplyInstalledAutogen() async {
    final preview = _preview;
    if (preview == null) return;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => _InstalledAutogenApplyDialog(preview: preview),
    );
    if (confirmed != true || !mounted) return;
    await _applyInstalledAutogen(preview);
  }

  Future<void> _applyInstalledAutogen(InstalledAutogenPreview preview) async {
    setState(() {
      _running = true;
      _error = null;
    });
    try {
      final acceptedPackageIds = preview.candidates
          .map((candidate) => candidate.packageId)
          .toList(growable: false);
      final result = switch (_applyTarget) {
        _AutogenApplyTarget.installed =>
          await widget.getter.applyInstalledAutogen(
            preview,
            acceptedPackageIds: acceptedPackageIds,
          ),
        _AutogenApplyTarget.installedFdroid =>
          await widget.getter.applyInstalledFdroidAutogen(
            preview,
            acceptedPackageIds: acceptedPackageIds,
          ),
        _AutogenApplyTarget.github => await widget.getter.applyGithubAutogen(
          preview,
          acceptedPackageIds: acceptedPackageIds,
        ),
      };
      if (!mounted) return;
      setState(() {
        _preview = null;
        _applyResult = result;
        _running = false;
      });
    } on GetterBridgeException catch (error) {
      if (!mounted) return;
      setState(() {
        _error = error.error;
        _running = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _error = GetterError(
          code: 'bridge.installed_autogen_error',
          message: 'Installed autogen bridge failed',
          detail: error.toString(),
        );
        _running = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final preview = _preview;
    final applyResult = _applyResult;
    final canUseBridge = widget.getter.supportsInstalledAutogen;
    return Scaffold(
      key: AppKeys.installedAutogenRoute,
      appBar: AppBar(title: const Text('Installed autogen')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: <Widget>[
          ElevatedButton.icon(
            key: AppKeys.previewInstalledAutogen,
            onPressed: _running || !canUseBridge
                ? null
                : _previewInstalledAutogen,
            icon: const Icon(Icons.manage_search),
            label: Text(_running ? 'Working…' : 'Preview installed autogen'),
          ),
          const SizedBox(height: 8),
          ElevatedButton.icon(
            key: AppKeys.refreshDefaultFdroidCatalogCache,
            onPressed: _running || !canUseBridge
                ? null
                : _refreshDefaultFdroidCatalogCache,
            icon: const Icon(Icons.sync),
            label: Text(
              _running ? 'Working…' : 'Refresh F-Droid catalog cache',
            ),
          ),
          const SizedBox(height: 8),
          ElevatedButton.icon(
            key: AppKeys.previewInstalledFdroidAutogen,
            onPressed: _running || !canUseBridge
                ? null
                : _previewInstalledFdroidAutogen,
            icon: const Icon(Icons.apps_outage),
            label: Text(
              _running ? 'Working…' : 'Preview installed F-Droid autogen',
            ),
          ),
          if (!canUseBridge)
            const Padding(
              padding: EdgeInsets.only(top: 12),
              child: Text(
                key: AppKeys.installedAutogenBridgeUnavailable,
                'Getter installed-autogen bridge is not connected',
              ),
            ),
          if (preview == null && _error == null)
            const Padding(
              padding: EdgeInsets.only(top: 16),
              child: Text(
                key: AppKeys.installedAutogenReady,
                'Ready to preview installed app fallback packages',
              ),
            ),
          if (_error != null)
            Padding(
              padding: const EdgeInsets.only(top: 16),
              child: Text(
                key: AppKeys.installedAutogenError,
                _formatGetterError(_error!),
              ),
            ),
          if (_fdroidRefresh != null) ...<Widget>[
            const SizedBox(height: 16),
            Text(
              key: AppKeys.fdroidCatalogRefreshStatus,
              'F-Droid catalog ${_fdroidRefresh!.source}: ${_fdroidRefresh!.appCount} apps, ${_fdroidRefresh!.releaseCount} releases',
            ),
            Text('Cache key: ${_fdroidRefresh!.cacheKey}'),
            if (_fdroidRefresh!.diagnostics.isNotEmpty) ...<Widget>[
              const SizedBox(height: 8),
              ListView.builder(
                key: AppKeys.fdroidCatalogRefreshDiagnosticsList,
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                itemCount: _fdroidRefresh!.diagnostics.length,
                itemBuilder: (context, index) {
                  final diagnostic = _fdroidRefresh!.diagnostics[index];
                  return ListTile(
                    key: AppKeys.fdroidCatalogRefreshDiagnosticRow(index),
                    title: Text(diagnostic.code),
                    subtitle: Text(diagnostic.message),
                  );
                },
              ),
            ],
          ],
          if (preview != null) ...<Widget>[
            const SizedBox(height: 16),
            Text(
              key: AppKeys.installedAutogenPreview,
              '${preview.summary.candidateCount} candidates, ${preview.summary.skippedCount} skipped',
            ),
            if (preview.scanStats != null)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Text(
                  key: AppKeys.installedAutogenScanStats,
                  'Seen ${preview.scanStats!.totalSeen}, returned ${preview.scanStats!.returned}, filtered system ${preview.scanStats!.filteredSystem}, filtered self ${preview.scanStats!.filteredSelf}',
                ),
              ),
            const SizedBox(height: 16),
            Text('Candidates', style: Theme.of(context).textTheme.titleMedium),
            ListView.builder(
              key: AppKeys.installedAutogenCandidatesList,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: preview.candidates.length,
              itemBuilder: (context, index) {
                final candidate = preview.candidates[index];
                return ListTile(
                  key: AppKeys.autogenCandidateRow(candidate.packageId),
                  title: Text(candidate.displayName),
                  subtitle: Text(
                    '${candidate.packageId} • ${candidate.outputRelativePath}',
                  ),
                );
              },
            ),
            if (preview.skipped.isNotEmpty) ...<Widget>[
              const SizedBox(height: 16),
              Text('Skipped', style: Theme.of(context).textTheme.titleMedium),
              ListView.builder(
                key: AppKeys.installedAutogenSkipsList,
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                itemCount: preview.skipped.length,
                itemBuilder: (context, index) {
                  final skipped = preview.skipped[index];
                  return ListTile(
                    key: AppKeys.autogenSkipRow(skipped.packageId),
                    title: Text(skipped.packageId),
                    subtitle: Text(
                      '${skipped.reason}${skipped.coveringRepoId == null ? '' : ' • ${skipped.coveringRepoId}'}',
                    ),
                  );
                },
              ),
            ],
            if (preview.diagnostics.isNotEmpty) ...<Widget>[
              const SizedBox(height: 16),
              Text(
                'Diagnostics',
                style: Theme.of(context).textTheme.titleMedium,
              ),
              ListView.builder(
                key: AppKeys.installedAutogenDiagnosticsList,
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                itemCount: preview.diagnostics.length,
                itemBuilder: (context, index) {
                  final diagnostic = preview.diagnostics[index];
                  return ListTile(
                    key: AppKeys.autogenDiagnosticRow(index),
                    title: Text(diagnostic.code),
                    subtitle: Text(diagnostic.message),
                  );
                },
              ),
            ],
            const SizedBox(height: 16),
            ElevatedButton.icon(
              key: AppKeys.applyInstalledAutogen,
              onPressed: _running || preview.candidates.isEmpty
                  ? null
                  : _confirmAndApplyInstalledAutogen,
              icon: const Icon(Icons.check),
              label: const Text('Apply all candidates'),
            ),
          ],
          if (applyResult != null) ...<Widget>[
            const SizedBox(height: 16),
            Text(
              key: AppKeys.installedAutogenApplied,
              'Applied ${applyResult.appliedCount} packages',
            ),
            ListView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: applyResult.applied.length,
              itemBuilder: (context, index) {
                final applied = applyResult.applied[index];
                return ListTile(
                  key: AppKeys.autogenAppliedRow(applied.packageId),
                  title: Text(applied.packageId),
                  subtitle: Text(applied.outputRelativePath),
                );
              },
            ),
          ],
          const SizedBox(height: 24),
          Text(
            'GitHub Android APK autogen',
            style: Theme.of(context).textTheme.titleMedium,
          ),
          const SizedBox(height: 8),
          TextField(
            key: AppKeys.githubAutogenOwnerField,
            decoration: const InputDecoration(labelText: 'GitHub owner'),
            textInputAction: TextInputAction.next,
            onChanged: (value) => _githubOwner = value,
          ),
          TextField(
            key: AppKeys.githubAutogenRepoField,
            decoration: const InputDecoration(labelText: 'GitHub repository'),
            textInputAction: TextInputAction.next,
            onChanged: (value) => _githubRepo = value,
          ),
          TextField(
            key: AppKeys.githubAutogenAndroidPackageField,
            decoration: const InputDecoration(labelText: 'Android package'),
            textInputAction: TextInputAction.next,
            onChanged: (value) => _githubAndroidPackage = value,
          ),
          TextField(
            key: AppKeys.githubAutogenDisplayNameField,
            decoration: const InputDecoration(
              labelText: 'Display name (optional)',
            ),
            textInputAction: TextInputAction.done,
            onChanged: (value) => _githubDisplayName = value,
          ),
          const SizedBox(height: 8),
          ElevatedButton.icon(
            key: AppKeys.previewGithubAutogen,
            onPressed: _running || !canUseBridge ? null : _previewGithubAutogen,
            icon: const Icon(Icons.code),
            label: Text(
              _running ? 'Working…' : 'Preview GitHub Android APK autogen',
            ),
          ),
        ],
      ),
    );
  }
}

String _formatGetterError(GetterError error) {
  final detail = error.detail;
  if (detail == null || detail.trim().isEmpty) {
    return '${error.code}: ${error.message}';
  }
  return '${error.code}: ${error.message}\n$detail';
}

class _InstalledAutogenApplyDialog extends StatelessWidget {
  const _InstalledAutogenApplyDialog({required this.preview});

  final InstalledAutogenPreview preview;

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      key: AppKeys.installedAutogenConfirmDialog,
      title: const Text('Apply generated packages?'),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Text('Target repository: ${preview.targetRepoId}'),
            if (preview.targetRepoPath != null) Text(preview.targetRepoPath!),
            const SizedBox(height: 12),
            const Text('Packages to write:'),
            const SizedBox(height: 8),
            ...preview.candidates.map(
              (candidate) => Padding(
                key: AppKeys.autogenConfirmCandidateRow(candidate.packageId),
                padding: const EdgeInsets.only(bottom: 4),
                child: Text(candidate.packageId),
              ),
            ),
          ],
        ),
      ),
      actions: <Widget>[
        TextButton(
          key: AppKeys.cancelInstalledAutogenApply,
          onPressed: () => Navigator.of(context).pop(false),
          child: const Text('Cancel'),
        ),
        FilledButton(
          key: AppKeys.confirmInstalledAutogenApply,
          onPressed: () => Navigator.of(context).pop(true),
          child: const Text('Apply'),
        ),
      ],
    );
  }
}

class LogsPage extends StatelessWidget {
  const LogsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return const _PlaceholderPage(
      key: AppKeys.logsRoute,
      title: 'Logs',
      stateKey: AppKeys.logsEmpty,
      message: 'No getter events yet',
    );
  }
}

class SettingsPage extends StatelessWidget {
  const SettingsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return const _PlaceholderPage(
      key: AppKeys.settingsRoute,
      title: 'Settings',
      stateKey: AppKeys.settingsShell,
      message: 'Settings shell ready',
    );
  }
}

class MigrationPage extends StatefulWidget {
  const MigrationPage({
    super.key,
    required this.getter,
    required this.legacyMigrationPlatform,
  });

  final GetterAdapter getter;
  final LegacyMigrationPlatform legacyMigrationPlatform;

  @override
  State<MigrationPage> createState() => _MigrationPageState();
}

class _MigrationPageState extends State<MigrationPage> {
  List<MigrationReportSummary> _reports = const <MigrationReportSummary>[];
  LegacyMigrationImportResult? _importResult;
  String? _status;
  GetterError? _error;
  bool _running = false;

  @override
  void initState() {
    super.initState();
    _loadMigrationReports();
  }

  Future<void> _loadMigrationReports() async {
    try {
      final reports = await widget.getter.readMigrationReports();
      if (!mounted) return;
      setState(() {
        _reports = reports;
      });
    } on GetterBridgeException {
      // Reports are best-effort on page open. The explicit migration action
      // surfaces bridge errors to the user.
    }
  }

  Future<void> _startMigration() async {
    setState(() {
      _running = true;
      _status = 'Preparing legacy Room database';
      _error = null;
    });

    try {
      final candidate = await widget.legacyMigrationPlatform
          .prepareLegacyRoomImport();
      if (!mounted) return;
      if (!candidate.found || candidate.databasePath == null) {
        setState(() {
          _status = candidate.message ?? 'No legacy Room database found';
          _running = false;
        });
        return;
      }

      final importResult = await widget.getter.importLegacyRoomDatabase(
        candidate.databasePath!,
      );
      final reports = await widget.getter.readMigrationReports();
      if (!mounted) return;
      setState(() {
        _importResult = importResult;
        _reports = reports;
        _status = importResult.alreadyImported
            ? 'Legacy migration was already completed'
            : 'Legacy migration imported ${importResult.importedRecords} records';
        _running = false;
      });
    } on GetterBridgeException catch (error) {
      var reports = _reports;
      try {
        reports = await widget.getter.readMigrationReports();
      } on GetterBridgeException {
        // Keep the reports already on screen if the bridge cannot list them.
      }
      if (!mounted) return;
      setState(() {
        _error = error.error;
        _status = error.error.message;
        _reports = reports;
        _running = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _error = GetterError(
          code: 'platform.legacy_migration_error',
          message: 'Legacy migration platform adapter failed',
          detail: error.toString(),
        );
        _status = 'Legacy migration platform adapter failed';
        _running = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final canImportLegacyRoom = widget.getter.supportsLegacyRoomImport;
    return Scaffold(
      key: AppKeys.migrationRoute,
      appBar: AppBar(title: const Text('Legacy migration')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: <Widget>[
          ElevatedButton.icon(
            key: AppKeys.startLegacyMigration,
            onPressed: _running || !canImportLegacyRoom
                ? null
                : _startMigration,
            icon: const Icon(Icons.move_down),
            label: Text(_running ? 'Migrating…' : 'Start legacy migration'),
          ),
          if (!canImportLegacyRoom)
            const Padding(
              padding: EdgeInsets.only(top: 12),
              child: Text(
                key: AppKeys.migrationBridgeUnavailable,
                'Getter migration bridge is not connected',
              ),
            ),
          const SizedBox(height: 16),
          if (_status == null && _reports.isEmpty)
            const Text(
              key: AppKeys.migrationReady,
              'Ready to show migration reports',
            ),
          if (_status != null) Text(key: AppKeys.migrationStatus, _status!),
          if (_importResult != null)
            Padding(
              padding: const EdgeInsets.only(top: 12),
              child: Text(
                key: AppKeys.migrationImported,
                '${_importResult!.trackedPackages.length} tracked packages after import',
              ),
            ),
          if (_error != null)
            Padding(
              padding: const EdgeInsets.only(top: 12),
              child: Text(
                key: AppKeys.migrationError,
                '${_error!.code}: ${_error!.message}',
              ),
            ),
          if (_reports.isNotEmpty) ...<Widget>[
            const SizedBox(height: 16),
            Text('Reports', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 8),
            ListView.builder(
              key: AppKeys.migrationReportsList,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: _reports.length,
              itemBuilder: (context, index) {
                final report = _reports[index];
                return ListTile(
                  title: Text(report.code),
                  subtitle: Text(
                    '${report.message} • imported ${report.importedRecords}',
                  ),
                  trailing: report.ok
                      ? const Icon(Icons.check_circle, color: Colors.green)
                      : const Icon(Icons.error, color: Colors.red),
                );
              },
            ),
          ],
        ],
      ),
    );
  }
}

class _RouteButton extends StatelessWidget {
  const _RouteButton({
    super.key,
    required this.icon,
    required this.label,
    required this.routeName,
  });

  final IconData icon;
  final String label;
  final String routeName;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: FilledButton.icon(
        onPressed: () => Navigator.of(context).pushNamed(routeName),
        icon: Icon(icon),
        label: Text(label),
      ),
    );
  }
}

class _PlaceholderPage extends StatelessWidget {
  const _PlaceholderPage({
    super.key,
    required this.title,
    required this.stateKey,
    required this.message,
  });

  final String title;
  final Key stateKey;
  final String message;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(title)),
      body: Center(child: Text(key: stateKey, message)),
    );
  }
}
