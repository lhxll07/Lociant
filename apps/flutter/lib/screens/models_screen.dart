import 'dart:async';

import 'package:flutter/material.dart';

import '../app.dart';
import '../core/models.dart';
import '../l10n/app_localizations.dart';

class ModelsScreen extends StatefulWidget {
  const ModelsScreen({super.key});

  @override
  State<ModelsScreen> createState() => _ModelsScreenState();
}

class _ModelsScreenState extends State<ModelsScreen> {
  String _view = 'home';
  List<ModelInfo> _models = [];
  List<MarketModel> _market = [];
  String _query = '';
  String? _installingId;
  Timer? _pollTimer;
  bool _polling = false;

  @override
  void dispose() {
    _pollTimer?.cancel();
    super.dispose();
  }

  Future<void> _loadModels({bool refresh = false}) async {
    final scope = AppScope.of(context);
    try {
      final data = await scope.runtime.api.get(
        '/api/v1/models${refresh ? '?refresh=true' : ''}',
      );
      final list = data is Map && data['models'] is List
          ? data['models'] as List
          : const [];
      if (!mounted) return;
      setState(
        () => _models = list
            .whereType<Map>()
            .map((e) => ModelInfo.fromJson(Map<String, dynamic>.from(e)))
            .toList(),
      );
    } catch (_) {}
  }

  Future<void> _loadMarket() async {
    final scope = AppScope.of(context);
    try {
      final data = await scope.runtime.api.get(
        '/api/v1/catalog/models${_query.isEmpty ? '' : '?q=${Uri.encodeQueryComponent(_query)}'}',
      );
      final list = data is Map && data['models'] is List
          ? data['models'] as List
          : const [];
      if (!mounted) return;
      setState(
        () => _market = list
            .whereType<Map>()
            .map((e) => MarketModel.fromJson(Map<String, dynamic>.from(e)))
            .toList(),
      );
    } catch (_) {}
  }

  Future<void> _install(MarketModel model) async {
    final scope = AppScope.of(context);
    if (_installingId != null) return;
    setState(() => _installingId = model.id);
    try {
      final data = await scope.runtime.api.post('/api/v1/model-installations', {
        'modelId': model.id,
      });
      final jobId =
          (data is Map ? data['jobId'] : null)?.toString() ?? model.id;
      _pollInstall(jobId);
    } catch (_) {
      if (!mounted) return;
      setState(() => _installingId = null);
      _toast(context, AppLocalizations.of(context)!.toastModelImportFailed);
    }
  }

  void _pollInstall(String jobId) {
    final scope = AppScope.of(context);
    _pollTimer?.cancel();
    var retries = 0;
    Future<void> poll() async {
      if (_polling) return;
      _polling = true;
      try {
        final data = await scope.runtime.api.get(
          '/api/v1/model-installations/${Uri.encodeComponent(jobId)}',
        );
        if (data is! Map) return;
        final progress = InstallProgress.fromJson(
          Map<String, dynamic>.from(data),
        );
        if (!mounted) return;
        setState(() {
          _installingId = progress.active ? progress.modelId : null;
          _pendingProgress = progress;
        });
        if (progress.state == 'done' || progress.state == 'error') {
          _pollTimer?.cancel();
          _pendingProgress = null;
          await _loadModels();
        }
      } catch (_) {
        retries++;
        if (retries >= 20) {
          _pollTimer?.cancel();
          if (mounted) setState(() => _installingId = null);
        }
      } finally {
        _polling = false;
        if (mounted && _installingId != null) {
          _pollTimer = Timer(const Duration(milliseconds: 800), poll);
        }
      }
    }

    poll();
  }

  InstallProgress? _pendingProgress;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      appBar: AppBar(
        title: Text(
          _view == 'home'
              ? l10n.modelsTitle
              : _view == 'local'
              ? l10n.modelsLocalTitle
              : _view == 'market'
              ? l10n.modelsMarketTitle
              : l10n.modelsRuntimeTitle,
        ),
        leading: _view == 'home'
            ? null
            : IconButton(
                icon: const Icon(Icons.arrow_back),
                onPressed: () => setState(() => _view = 'home'),
              ),
        actions: _view == 'local'
            ? [
                IconButton(
                  tooltip: l10n.modelsRescan,
                  icon: const Icon(Icons.refresh),
                  onPressed: () => _loadModels(refresh: true),
                ),
                IconButton(
                  tooltip: l10n.modelsImport,
                  icon: const Icon(Icons.file_open_outlined),
                  onPressed: () =>
                      AppScope.of(context).runtime.openModelPackagePicker(),
                ),
              ]
            : null,
      ),
      body: switch (_view) {
        'home' => _homeGrid(l10n),
        'local' => _localView(l10n),
        'market' => _marketView(l10n),
        _ => _runtimeView(l10n),
      },
    );
  }

  Widget _homeGrid(AppLocalizations l10n) {
    final readyCount = _models.where((m) => m.ready).length;
    final state = AppScope.of(context).runtime.state;
    return ListView(
      padding: const EdgeInsets.all(14),
      children: [
        _tile(
          l10n.modelsLocalTitle,
          l10n.modelsLocalSub,
          '$readyCount',
          Icons.folder_outlined,
          () {
            setState(() => _view = 'local');
            _loadModels();
          },
        ),
        _tile(
          l10n.modelsMarketTitle,
          l10n.modelsMarketSub,
          l10n.modelsMarketSub,
          Icons.storefront_outlined,
          () {
            setState(() => _view = 'market');
            _loadMarket();
          },
        ),
        _tile(
          l10n.modelsRuntimeTitle,
          l10n.modelsRuntimeSub,
          state?.modelId ?? '--',
          Icons.bolt_outlined,
          () {
            setState(() => _view = 'runtime');
            _loadModels();
          },
        ),
      ],
    );
  }

  Widget _tile(
    String title,
    String sub,
    String state,
    IconData icon,
    VoidCallback onTap,
  ) {
    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      child: ListTile(
        leading: Icon(icon),
        title: Text(title, style: const TextStyle(fontWeight: FontWeight.w600)),
        subtitle: Text(sub, maxLines: 1, overflow: TextOverflow.ellipsis),
        trailing: Padding(
          padding: const EdgeInsets.only(left: 8),
          child: Text(state, maxLines: 1, overflow: TextOverflow.ellipsis),
        ),
        onTap: onTap,
      ),
    );
  }

  Widget _localView(AppLocalizations l10n) {
    if (_models.isEmpty) {
      return const Center(child: Text('--'));
    }
    return ListView.builder(
      padding: const EdgeInsets.all(8),
      itemCount: _models.length,
      itemBuilder: (context, index) {
        final model = _models[index];
        final tags = [
          model.runtime,
          model.type,
        ].where((t) => t.isNotEmpty).join(' · ');
        return Card(
          child: ListTile(
            title: Text(model.name),
            subtitle: Text(tags),
            trailing: model.installed
                ? TextButton(
                    onPressed: () async {
                      await AppScope.of(context).runtime.api.delete(
                        '/api/v1/models/${Uri.encodeComponent(model.id)}',
                      );
                      await _loadModels();
                    },
                    child: Text(l10n.modelsDelete),
                  )
                : Text(model.ready ? l10n.statusRunning : l10n.emptyModels),
          ),
        );
      },
    );
  }

  Widget _marketView(AppLocalizations l10n) {
    final progress = _pendingProgress;
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(12, 10, 12, 4),
          child: Row(
            children: [
              Expanded(
                child: TextField(
                  decoration: const InputDecoration(
                    isDense: true,
                    hintText: '',
                  ),
                  onSubmitted: (value) {
                    _query = value.trim();
                    _loadMarket();
                  },
                ),
              ),
              IconButton(
                icon: const Icon(Icons.search),
                onPressed: () => _loadMarket(),
              ),
            ],
          ),
        ),
        if (progress != null)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: LinearProgressIndicator(
              value: progress.progress == null
                  ? null
                  : (progress.progress! / 100).clamp(0, 1),
            ),
          ),
        if (progress != null)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
            child: Text(
              '${progress.message}${progress.progress != null ? ' ${progress.progress!.round()}%' : ''}',
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ),
        Expanded(
          child: _market.isEmpty
              ? const Center(child: Text('--'))
              : ListView.builder(
                  padding: const EdgeInsets.all(8),
                  itemCount: _market.length,
                  itemBuilder: (context, index) {
                    final model = _market[index];
                    final installing = _installingId == model.id;
                    return Card(
                      child: ListTile(
                        title: Text(
                          model.name,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        subtitle: Text(
                          model.description,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                        ),
                        trailing: model.installed
                            ? Text(l10n.modelsInstalled)
                            : FilledButton.tonal(
                                onPressed: installing
                                    ? null
                                    : () => _install(model),
                                child: Text(
                                  installing
                                      ? l10n.modelsInstalling
                                      : l10n.modelsInstall,
                                ),
                              ),
                      ),
                    );
                  },
                ),
        ),
      ],
    );
  }

  Widget _runtimeView(AppLocalizations l10n) {
    final ready = _models.where((m) => m.ready && m.isChatModel).toList();
    final state = AppScope.of(context).runtime.state;
    return ListView.builder(
      padding: const EdgeInsets.all(8),
      itemCount: ready.length,
      itemBuilder: (context, index) {
        final model = ready[index];
        final selected = state?.modelId == model.id;
        return Card(
          child: RadioGroup<String>(
            groupValue: selected ? model.id : null,
            onChanged: (_) => AppScope.of(
              context,
            ).runtime.updateSettings({'modelId': model.id}),
            child: RadioListTile<String>(
              value: model.id,
              title: Text(model.name),
              subtitle: Text('${model.runtime} · ${model.type}'),
            ),
          ),
        );
      },
    );
  }

  void _toast(BuildContext context, String message) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }
}
