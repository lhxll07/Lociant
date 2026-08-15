import 'package:flutter/material.dart';

/// A button-triggered floating panel anchored below its child, mirroring the
/// original web UI's popup sidebar. Tapping the child toggles
/// the panel; tapping anywhere outside closes it.
class AnchoredOverlay extends StatefulWidget {
  const AnchoredOverlay({
    super.key,
    required this.builder,
    required this.popupBuilder,
    this.popupWidth = 300,
    this.maxHeight = 430,
    this.offset = const Offset(0, 10),
  });

  final Widget Function(BuildContext context, VoidCallback toggle) builder;
  final Widget Function(BuildContext context, VoidCallback close) popupBuilder;
  final double popupWidth;
  final double maxHeight;
  final Offset offset;

  @override
  State<AnchoredOverlay> createState() => _AnchoredOverlayState();
}

class _AnchoredOverlayState extends State<AnchoredOverlay> {
  final _controller = OverlayPortalController();
  final _link = LayerLink();
  Rect? _anchorRect;

  void _toggle() {
    _captureAnchor();
    setState(() {});
    if (_controller.isShowing) {
      _controller.hide();
    } else {
      _controller.show();
    }
  }

  void _captureAnchor() {
    final ctx = _targetContext;
    if (ctx == null || !ctx.mounted) return;
    final box = ctx.findRenderObject();
    if (box is RenderBox && box.hasSize && box.attached) {
      _anchorRect = box.localToGlobal(Offset.zero) & box.size;
    }
  }

  BuildContext? _targetContext;

  @override
  Widget build(BuildContext context) {
    final maxWidth =
        (_anchorRect == null
                ? widget.popupWidth
                : (MediaQuery.sizeOf(context).width - _anchorRect!.left - 12))
            .clamp(180.0, widget.popupWidth);
    return OverlayPortal(
      controller: _controller,
      overlayChildBuilder: (_) => Stack(
        children: [
          Positioned.fill(
            child: GestureDetector(
              behavior: HitTestBehavior.translucent,
              onTap: () => _controller.hide(),
            ),
          ),
          CompositedTransformFollower(
            link: _link,
            offset: widget.offset,
            targetAnchor: Alignment.topLeft,
            followerAnchor: Alignment.topLeft,
            showWhenUnlinked: false,
            child: ConstrainedBox(
              constraints: BoxConstraints(maxWidth: maxWidth),
              child: Material(
                elevation: 16,
                shadowColor: Colors.black45,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(20),
                  side: BorderSide(
                    color: Theme.of(
                      context,
                    ).colorScheme.outlineVariant.withValues(alpha: 0.4),
                  ),
                ),
                color: Theme.of(context).colorScheme.surfaceContainerHigh,
                clipBehavior: Clip.antiAlias,
                child: ConstrainedBox(
                  constraints: BoxConstraints(maxHeight: widget.maxHeight),
                  child: widget.popupBuilder(context, () => _controller.hide()),
                ),
              ),
            ),
          ),
        ],
      ),
      child: CompositedTransformTarget(
        link: _link,
        child: Builder(
          builder: (targetContext) {
            _targetContext = targetContext;
            return widget.builder(targetContext, _toggle);
          },
        ),
      ),
    );
  }
}
