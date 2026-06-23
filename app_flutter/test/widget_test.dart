import 'package:flutter_test/flutter_test.dart';

import 'package:upgradeall/getter_adapter.dart';
import 'package:upgradeall/main.dart';

void main() {
  testWidgets('fresh launch exposes home route and getter state',
      (tester) async {
    await tester.pumpWidget(const UpgradeAllApp());

    expect(find.byKey(AppKeys.homeRoute), findsOneWidget);
    expect(find.byKey(AppKeys.updateSummary), findsOneWidget);
    expect(find.byKey(AppKeys.getterStatus), findsOneWidget);
    expect(find.text('0 updates available'), findsOneWidget);
    expect(find.text('Fake getter ready'), findsOneWidget);
  });

  testWidgets('app list and detail routes use stable keys', (tester) async {
    await tester.pumpWidget(const UpgradeAllApp());

    await tester.tap(find.byKey(AppKeys.openApps));
    await tester.pumpAndSettle();

    expect(find.byKey(AppKeys.appsRoute), findsOneWidget);
    expect(find.byKey(AppKeys.appsList), findsOneWidget);
    expect(find.byKey(AppKeys.appRow('android/org.fdroid.fdroid')),
        findsOneWidget);
    expect(find.text('Network'), findsOneWidget);

    await tester.tap(find.byKey(AppKeys.appRow('android/org.fdroid.fdroid')));
    await tester.pumpAndSettle();

    expect(find.byKey(AppKeys.appDetailRoute), findsOneWidget);
    expect(find.text('android/org.fdroid.fdroid'), findsOneWidget);
    expect(find.text('Installed: 1.20.0'), findsOneWidget);
    expect(find.text('Latest: 1.20.0'), findsOneWidget);
    expect(find.text('Network access required'), findsOneWidget);
  });

  testWidgets('repository route lists priority ordered repository IDs',
      (tester) async {
    await tester.pumpWidget(const UpgradeAllApp());

    await tester.tap(find.byKey(AppKeys.openRepositories));
    await tester.pumpAndSettle();

    expect(find.byKey(AppKeys.repositoriesRoute), findsOneWidget);
    expect(find.byKey(AppKeys.repositoriesList), findsOneWidget);
    expect(find.byKey(AppKeys.repoRow('local')), findsOneWidget);
    expect(find.byKey(AppKeys.repoRow('official')), findsOneWidget);
    expect(find.byKey(AppKeys.repoRow('local_autogen')), findsOneWidget);
  });

  testWidgets('downloads route renders getter task DTOs read-only',
      (tester) async {
    await tester.pumpWidget(const UpgradeAllApp());

    await tester.tap(find.byKey(AppKeys.openDownloads));
    await tester.pumpAndSettle();

    expect(find.byKey(AppKeys.downloadsRoute), findsOneWidget);
    expect(find.byKey(AppKeys.downloadsList), findsOneWidget);
    expect(find.byKey(AppKeys.downloadTaskRow('task-1')), findsOneWidget);
    expect(find.byKey(AppKeys.taskEventsList), findsOneWidget);
    expect(find.byKey(AppKeys.taskEventRow(3)), findsOneWidget);
    expect(find.text('Install handoff'), findsOneWidget);
  });

  testWidgets('downloads route exposes getter empty task state',
      (tester) async {
    await tester.pumpWidget(
      const UpgradeAllApp(getter: _NoTaskGetterAdapter()),
    );

    await tester.tap(find.byKey(AppKeys.openDownloads));
    await tester.pumpAndSettle();

    expect(find.byKey(AppKeys.downloadsRoute), findsOneWidget);
    expect(find.byKey(AppKeys.downloadsEmpty), findsOneWidget);
  });

  testWidgets('placeholder routes expose stable empty-state keys',
      (tester) async {
    await tester.pumpWidget(const UpgradeAllApp());

    await tester.tap(find.byKey(AppKeys.openLogs));
    await tester.pumpAndSettle();
    expect(find.byKey(AppKeys.logsRoute), findsOneWidget);
    expect(find.byKey(AppKeys.logsEmpty), findsOneWidget);

    await tester.pageBack();
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(AppKeys.openSettings));
    await tester.pumpAndSettle();
    expect(find.byKey(AppKeys.settingsRoute), findsOneWidget);
    expect(find.byKey(AppKeys.settingsShell), findsOneWidget);

    await tester.pageBack();
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(AppKeys.openMigration));
    await tester.pumpAndSettle();
    expect(find.byKey(AppKeys.migrationRoute), findsOneWidget);
    expect(find.byKey(AppKeys.migrationReady), findsOneWidget);
  });
}

class _NoTaskGetterAdapter extends FakeGetterAdapter {
  const _NoTaskGetterAdapter();

  @override
  List<DownloadTaskSummary> listDownloadTasks() =>
      const <DownloadTaskSummary>[];

  @override
  TaskEventPage listTaskEvents({required int after, required int limit}) {
    return const TaskEventPage(
      events: <TaskEventSummary>[],
      nextCursor: 0,
      hasMore: false,
    );
  }
}
