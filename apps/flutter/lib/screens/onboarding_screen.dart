import 'package:flutter/material.dart';

import '../l10n/app_localizations.dart';

/// First-run wizard shown until the user finishes (or skips) it.
///
/// Scenario-driven pages: welcome, cloud model, permissions, node
/// interconnect, ready. Completion is stored by the app shell, so the
/// wizard never shows again.
class OnboardingScreen extends StatefulWidget {
  const OnboardingScreen({super.key, required this.onDone});

  final VoidCallback onDone;

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  final PageController _controller = PageController();
  int _page = 0;
  List<_PageData> _pages = const [];

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _next() {
    if (_page < _pages.length - 1) {
      _controller.nextPage(
        duration: const Duration(milliseconds: 250),
        curve: Curves.easeOut,
      );
    } else {
      widget.onDone();
    }
  }

  void _back() {
    if (_page > 0) {
      _controller.previousPage(
        duration: const Duration(milliseconds: 250),
        curve: Curves.easeOut,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final scheme = Theme.of(context).colorScheme;
    _pages = [
      _PageData(
        Icons.smart_toy_outlined,
        l10n.onboardingWelcomeTitle,
        l10n.onboardingWelcomeBody,
      ),
      _PageData(
        Icons.cloud_outlined,
        l10n.onboardingCloudTitle,
        l10n.onboardingCloudBody,
      ),
      _PageData(
        Icons.touch_app_outlined,
        l10n.onboardingPermissionTitle,
        l10n.onboardingPermissionBody,
      ),
      _PageData(
        Icons.hub_outlined,
        l10n.onboardingNodesTitle,
        l10n.onboardingNodesBody,
      ),
      _PageData(
        Icons.check_circle_outline,
        l10n.onboardingReadyTitle,
        l10n.onboardingReadyBody,
      ),
    ];
    final isLast = _page == _pages.length - 1;
    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            Align(
              alignment: Alignment.centerRight,
              child: Padding(
                padding: const EdgeInsets.all(8),
                child: TextButton(
                  onPressed: widget.onDone,
                  child: Text(l10n.onboardingSkip),
                ),
              ),
            ),
            Expanded(
              child: PageView.builder(
                controller: _controller,
                itemCount: _pages.length,
                onPageChanged: (index) => setState(() => _page = index),
                itemBuilder: (context, index) {
                  final data = _pages[index];
                  return Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 40),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(data.icon, size: 96, color: scheme.primary),
                        const SizedBox(height: 32),
                        Text(
                          data.title,
                          style: Theme.of(context).textTheme.headlineSmall,
                          textAlign: TextAlign.center,
                        ),
                        const SizedBox(height: 16),
                        Text(
                          data.body,
                          style: Theme.of(context).textTheme.bodyLarge
                              ?.copyWith(color: scheme.onSurfaceVariant),
                          textAlign: TextAlign.center,
                        ),
                      ],
                    ),
                  );
                },
              ),
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                SizedBox(
                  width: 96,
                  child: _page > 0
                      ? TextButton(
                          onPressed: _back,
                          child: Text(l10n.commonBack),
                        )
                      : null,
                ),
                Row(
                  children: List.generate(_pages.length, (index) {
                    final active = index == _page;
                    return AnimatedContainer(
                      duration: const Duration(milliseconds: 200),
                      margin: const EdgeInsets.symmetric(horizontal: 4),
                      width: active ? 22 : 8,
                      height: 8,
                      decoration: BoxDecoration(
                        color: active ? scheme.primary : scheme.outlineVariant,
                        borderRadius: BorderRadius.circular(4),
                      ),
                    );
                  }),
                ),
                Padding(
                  padding: const EdgeInsets.all(16),
                  child: FilledButton(
                    onPressed: _next,
                    child: Text(
                      isLast ? l10n.onboardingStart : l10n.onboardingNext,
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _PageData {
  const _PageData(this.icon, this.title, this.body);

  final IconData icon;
  final String title;
  final String body;
}
