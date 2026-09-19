import 'dart:io';

Future<bool> openExternalUrl(String url) async {
  final uri = Uri.tryParse(url);
  if (uri == null ||
      uri.host.isEmpty ||
      !{'http', 'https'}.contains(uri.scheme.toLowerCase())) {
    return false;
  }
  final command = switch (Platform.operatingSystem) {
    'linux' => ('xdg-open', <String>[url]),
    'macos' => ('open', <String>[url]),
    'windows' => ('explorer.exe', <String>[url]),
    _ => null,
  };
  if (command == null) return false;
  try {
    final result = await Process.run(command.$1, command.$2);
    return result.exitCode == 0;
  } catch (_) {
    return false;
  }
}
