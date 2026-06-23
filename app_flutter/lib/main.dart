import 'package:flutter/material.dart';

import 'getter_adapter.dart';
import 'legacy_migration_platform.dart';

void main() {
  runApp(
    const UpgradeAllApp(
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

  static const openApps = ValueKey<String>('action.open_apps');
  static const openRepositories = ValueKey<String>('action.open_repositories');
  static const openDownloads = ValueKey<String>('action.open_downloads');
  static const openLogs = ValueKey<String>('action.open_logs');
  static const openSettings = ValueKey<String>('action.open_settings');
  static const openMigration = ValueKey<String>('action.open_migration');
  static const openFirstApp = ValueKey<String>('action.open_first_app');
  static const startLegacyMigration =
      ValueKey<String>('action.start_legacy_migration');

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
  static const migrationBridgeUnavailable =
      ValueKey<String>('state.migration_bridge_unavailable');
  static const migrationImported = ValueKey<String>('state.migration_imported');
  static const migrationError = ValueKey<String>('state.migration_error');
  static const migrationReportsList =
      ValueKey<String>('state.migration_reports_list');

  static ValueKey<String> appRow(String packageId) =>
      ValueKey<String>('state.app.$packageId');
  static ValueKey<String> repoRow(String repositoryId) =>
      ValueKey<String>('state.repository.$repositoryId');
  static ValueKey<String> downloadTaskRow(String taskId) =>
      ValueKey<String>('state.download_task.$taskId');
  static ValueKey<String> taskEventRow(int cursor) =>
      ValueKey<String>('state.task_event.$cursor');
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
      },
      onGenerateRoute: (settings) {
        if (settings.name == '/apps/detail') {
          final app = settings.arguments! as AppSummary;
          return MaterialPageRoute<void>(
            builder: (context) => AppDetailPage(app: app),
            settings: settings,
          );
        }
        return null;
      },
    );
  }
}

class HomePage extends StatelessWidget {
  const HomePage({super.key, required this.getter});

  final GetterAdapter getter;

  @override
  Widget build(BuildContext context) {
    final snapshot = getter.loadSnapshot();
    return Scaffold(
      key: AppKeys.homeRoute,
      appBar: AppBar(title: const Text('UpgradeAll')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: <Widget>[
          Card(
            key: AppKeys.updateSummary,
            child: ListTile(
              title: const Text('Updates'),
              subtitle: Text('${snapshot.updateCount} updates available'),
            ),
          ),
          Card(
            key: AppKeys.getterStatus,
            child: ListTile(
              title: const Text('Getter core'),
              subtitle: Text(snapshot.status),
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
        ],
      ),
    );
  }
}

class AppsPage extends StatelessWidget {
  const AppsPage({super.key, required this.getter});

  final GetterAdapter getter;

  @override
  Widget build(BuildContext context) {
    final apps = getter.loadSnapshot().apps;
    return Scaffold(
      key: AppKeys.appsRoute,
      appBar: AppBar(title: const Text('Apps')),
      body: ListView.builder(
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
              Navigator.of(context).pushNamed('/apps/detail', arguments: app);
            },
          );
        },
      ),
    );
  }
}

class AppDetailPage extends StatelessWidget {
  const AppDetailPage({super.key, required this.app});

  final AppSummary app;

  @override
  Widget build(BuildContext context) {
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

class RepositoriesPage extends StatelessWidget {
  const RepositoriesPage({super.key, required this.getter});

  final GetterAdapter getter;

  @override
  Widget build(BuildContext context) {
    final repositories = getter.loadSnapshot().repositories;
    return Scaffold(
      key: AppKeys.repositoriesRoute,
      appBar: AppBar(title: const Text('Repositories')),
      body: ListView.builder(
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
      ),
    );
  }
}

class DownloadsPage extends StatelessWidget {
  const DownloadsPage({super.key, required this.getter});

  final GetterAdapter getter;

  @override
  Widget build(BuildContext context) {
    final tasks = getter.listDownloadTasks();
    final events = getter.listTaskEvents(after: 0, limit: 20).events;
    return Scaffold(
      key: AppKeys.downloadsRoute,
      appBar: AppBar(title: const Text('Downloads')),
      body: tasks.isEmpty
          ? const Center(
              child: Text(key: AppKeys.downloadsEmpty, 'No download tasks yet'),
            )
          : ListView(
              padding: const EdgeInsets.all(16),
              children: <Widget>[
                Text('Tasks', style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: 8),
                ListView.builder(
                  key: AppKeys.downloadsList,
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  itemCount: tasks.length,
                  itemBuilder: (context, index) {
                    final task = tasks[index];
                    return Card(
                      child: ListTile(
                        key: AppKeys.downloadTaskRow(task.id),
                        title: Text(task.packageId),
                        subtitle: Text(
                          '${task.status} • ${task.downloadFileName}',
                        ),
                        trailing: task.installHandoffId == null
                            ? null
                            : const Chip(label: Text('Install handoff')),
                      ),
                    );
                  },
                ),
                const SizedBox(height: 16),
                Text('Events', style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: 8),
                ListView.builder(
                  key: AppKeys.taskEventsList,
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  itemCount: events.length,
                  itemBuilder: (context, index) {
                    final event = events[index];
                    return ListTile(
                      key: AppKeys.taskEventRow(event.cursor),
                      title: Text(event.kind),
                      subtitle: Text(
                        '${event.taskId} • ${event.status ?? 'no status'}',
                      ),
                    );
                  },
                ),
              ],
            ),
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
  late List<MigrationReportSummary> _reports;
  LegacyMigrationImportResult? _importResult;
  String? _status;
  GetterError? _error;
  bool _running = false;

  @override
  void initState() {
    super.initState();
    _reports = widget.getter.readMigrationReports();
  }

  Future<void> _startMigration() async {
    setState(() {
      _running = true;
      _status = 'Preparing legacy Room database';
      _error = null;
    });

    try {
      final candidate =
          await widget.legacyMigrationPlatform.prepareLegacyRoomImport();
      if (!mounted) return;
      if (!candidate.found || candidate.databasePath == null) {
        setState(() {
          _status = candidate.message ?? 'No legacy Room database found';
          _running = false;
        });
        return;
      }

      final importResult =
          widget.getter.importLegacyRoomDatabase(candidate.databasePath!);
      final reports = widget.getter.readMigrationReports();
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
      if (!mounted) return;
      setState(() {
        _error = error.error;
        _status = error.error.message;
        _reports = widget.getter.readMigrationReports();
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
            onPressed:
                _running || !canImportLegacyRoom ? null : _startMigration,
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
      body: Center(
        child: Text(key: stateKey, message),
      ),
    );
  }
}
