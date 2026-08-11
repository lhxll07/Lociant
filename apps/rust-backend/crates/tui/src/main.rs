//! Terminal UI client for headless Lociant nodes.
//!
//! Connects to a running `lociant-server` (local or remote) over the OpenAI
//! and control planes: chat, model switching, node status — all from a
//! terminal over SSH. Usage:
//!
//! ```text
//! lociant-tui --connect http://192.168.10.103:11434 --token TOKEN
//! ```

use std::io::{self, Read};
use std::time::Duration;

use anyhow::Result;
use crossterm::execute;
use crossterm::terminal::{
    disable_raw_mode, enable_raw_mode, EnterAlternateScreen, LeaveAlternateScreen,
};
use ratatui::backend::CrosstermBackend;
use ratatui::layout::{Constraint, Layout, Rect};
use ratatui::style::{Color, Modifier, Style};
use ratatui::text::{Line, Span};
use ratatui::widgets::{Block, Borders, Paragraph, Wrap};
use ratatui::{Frame, Terminal};
use serde_json::{json, Value};
use unicode_width::UnicodeWidthStr;

struct App {
    base_url: String,
    token: String,
    client: reqwest::blocking::Client,
    messages: Vec<(String, String)>, // (role, content)
    models: Vec<String>,
    model: String,
    nodes: Vec<Value>,
    input: String,
    connected: bool,
    notice: String,
    thinking: bool,
    result_rx: std::sync::mpsc::Receiver<ChatOutcome>,
    result_tx: std::sync::mpsc::Sender<ChatOutcome>,
}

struct ChatOutcome {
    ok: bool,
    text: String,
    notice: String,
}

impl App {
    fn new(base_url: String, token: String) -> Self {
        let (result_tx, result_rx) = std::sync::mpsc::channel();
        App {
            client: reqwest::blocking::Client::builder()
                .timeout(Duration::from_secs(30))
                .build()
                .unwrap_or_default(),
            base_url,
            token,
            messages: Vec::new(),
            models: Vec::new(),
            model: String::new(),
            nodes: Vec::new(),
            input: String::new(),
            connected: false,
            notice: String::new(),
            thinking: false,
            result_rx,
            result_tx,
        }
    }

    fn api(&self, path: &str) -> String {
        format!("{}{path}", self.base_url.trim_end_matches('/'))
    }

    fn headers(&self) -> reqwest::header::HeaderMap {
        let mut headers = reqwest::header::HeaderMap::new();
        if !self.token.is_empty() {
            headers.insert(
                reqwest::header::AUTHORIZATION,
                format!("Bearer {}", self.token).parse().unwrap(),
            );
        }
        headers
    }

    fn refresh(&mut self) {
        self.connected = self
            .client
            .get(self.api("/health"))
            .send()
            .map(|r| r.status().is_success())
            .unwrap_or(false);
        self.notice = String::new();
        if let Ok(response) = self
            .client
            .get(self.api("/api/v1/models"))
            .headers(self.headers())
            .send()
        {
            if let Ok(body) = response.json::<Value>() {
                self.models = body
                    .get("models")
                    .and_then(Value::as_array)
                    .map(|list| {
                        list.iter()
                            .filter_map(|m| m.get("id").and_then(Value::as_str).map(str::to_owned))
                            .collect()
                    })
                    .unwrap_or_default();
                if self.model.is_empty() {
                    self.model = self.models.first().cloned().unwrap_or_default();
                }
            }
        }
        if let Ok(response) = self
            .client
            .get(self.api("/api/v1/nodes"))
            .headers(self.headers())
            .send()
        {
            if let Ok(body) = response.json::<Value>() {
                self.nodes = body
                    .get("nodes")
                    .and_then(Value::as_array)
                    .cloned()
                    .unwrap_or_default();
            }
        }
    }

    fn send_chat(&mut self, text: String) {
        let text = text.trim().to_owned();
        if text.is_empty() || self.thinking {
            return;
        }
        self.input.clear();
        self.messages.push(("user".to_owned(), text.clone()));
        self.thinking = true;
        let url = self.api("/v1/chat/completions");
        let headers = self.headers();
        // The server requires a model field; fall back to "rkllm" when no
        // model list was loaded (e.g. headless board with built-in RKLLM).
        let model = if self.model.is_empty() {
            "rkllm".to_owned()
        } else {
            self.model.clone()
        };
        let client = self.client.clone();
        let tx = self.result_tx.clone();
        std::thread::spawn(move || {
            let mut body = json!({
                "messages": [{"role": "user", "content": text}],
                "stream": false,
                "max_tokens": 1024,
            });
            if !model.is_empty() {
                body["model"] = json!(model);
            }
            let outcome = match client.post(url).headers(headers).json(&body).send() {
                Ok(response) if response.status().is_success() => {
                    match response.json::<Value>() {
                        Ok(parsed) => ChatOutcome {
                            ok: true,
                            text: parsed
                                .pointer("/choices/0/message/content")
                                .and_then(Value::as_str)
                                .unwrap_or("")
                                .to_owned(),
                            notice: String::new(),
                        },
                        Err(error) => ChatOutcome {
                            ok: false,
                            text: String::new(),
                            notice: format!("响应解析失败: {error}"),
                        },
                    }
                }
                Ok(response) => {
                    match response.json::<Value>() {
                        Ok(parsed) => ChatOutcome {
                            ok: false,
                            text: String::new(),
                            notice: parsed
                                .get("error")
                                .and_then(|e| e.get("message"))
                                .and_then(Value::as_str)
                                .unwrap_or("unknown error")
                                .to_owned(),
                        },
                        Err(error) => ChatOutcome {
                            ok: false,
                            text: String::new(),
                            notice: format!("响应解析失败: {error}"),
                        },
                    }
                }
                Err(error) => {
                    ChatOutcome {
                        ok: false,
                        text: String::new(),
                        notice: format!("请求失败: {error}"),
                    }
                }
            };
            let _ = tx.send(outcome);
        });
    }

    fn run_command(&mut self, raw: &str) {
        let (command, arg) = raw
            .split_once(char::is_whitespace)
            .map(|(c, a)| (c, a.trim()))
            .unwrap_or((raw, ""));
        match command {
            "/help" => {
                self.notice = "/help /models /model <id> /nodes /clear /quit — 输入文本直接聊天".to_owned();
            }
            "/models" => {
                self.refresh();
                let list = if self.models.is_empty() {
                    "（无可用模型）".to_owned()
                } else {
                    self.models.join("\n")
                };
                self.messages.push(("system".to_owned(), format!("可用模型:\n{list}")));
            }
            "/model" => {
                if arg.is_empty() {
                    self.notice = format!("当前模型: {}", self.model);
                } else if self.models.iter().any(|m| m == arg) {
                    self.model = arg.to_owned();
                    self.notice = format!("已切换到模型: {arg}");
                } else {
                    self.notice = format!("未知模型: {arg}（用 /models 查看）");
                }
            }
            "/nodes" => {
                self.refresh();
                let list = self
                    .nodes
                    .iter()
                    .map(|n| {
                        format!(
                            "{} {}:{} {}",
                            n.get("name").and_then(Value::as_str).unwrap_or("?"),
                            n.get("host").and_then(Value::as_str).unwrap_or("?"),
                            n.get("port").and_then(Value::as_u64).unwrap_or(0),
                            if n.get("self").and_then(Value::as_bool).unwrap_or(false) {
                                "(本机)"
                            } else {
                                ""
                            }
                        )
                    })
                    .collect::<Vec<_>>()
                    .join("\n");
                self.messages.push(("system".to_owned(), format!("节点:\n{list}")));
            }
            "/clear" => {
                self.messages.clear();
            }
            "/quit" => std::process::exit(0),
            _ => self.notice = format!("未知命令: {command}（/help 查看）"),
        }
    }
}

fn main() -> Result<()> {
    let mut base_url = "http://127.0.0.1:11434".to_owned();
    let mut token = String::new();
    let mut args = std::env::args().skip(1);
    while let Some(arg) = args.next() {
        match arg.as_str() {
            "--connect" => base_url = args.next().unwrap_or(base_url),
            "--token" => token = args.next().unwrap_or_default(),
            "-h" | "--help" => {
                println!("lociant-tui — Lociant 终端客户端");
                println!("用法: lociant-tui [--connect http://host:11434] [--token TOKEN]");
                return Ok(());
            }
            _ => {
                eprintln!("未知参数: {arg}（--help 查看）");
                std::process::exit(2);
            }
        }
    }

    // Auto-read the node token from the headless config when not given, so
    // `lociant-tui` on a board works without flags.
    if token.is_empty() {
        let config_path = std::env::var("LOCIANT_CONFIG")
            .unwrap_or_else(|_| "/etc/lociant/config.json".to_owned());
        if let Ok(raw) = std::fs::read_to_string(&config_path) {
            if let Ok(config) = serde_json::from_str::<Value>(&raw) {
                token = config
                    .get("authToken")
                    .and_then(Value::as_str)
                    .unwrap_or("")
                    .to_owned();
            }
        }
    }

    let mut app = App::new(base_url, token);
    app.refresh();

    enable_raw_mode()?;
    let mut stdout = io::stdout();
    execute!(stdout, EnterAlternateScreen)?;
    let backend = CrosstermBackend::new(stdout);
    let mut terminal = Terminal::new(backend)?;

    let result = run(&mut terminal, &mut app);

    disable_raw_mode()?;
    execute!(terminal.backend_mut(), LeaveAlternateScreen)?;
    terminal.show_cursor()?;
    result
}

fn run(terminal: &mut Terminal<CrosstermBackend<io::Stdout>>, app: &mut App) -> Result<()> {
    // Raw stdin bytes arrive through a dedicated reader thread; crossterm's
    // event parser drops Enter after multi-byte UTF-8 input on some
    // terminals, so the UI parses the few keys it needs itself.
    let (byte_tx, byte_rx) = std::sync::mpsc::channel::<u8>();
    std::thread::spawn(move || {
        let mut stdin = io::stdin();
        let mut buf = [0u8; 256];
        loop {
            match stdin.read(&mut buf) {
                Ok(0) => break,
                Ok(n) => {
                    for &byte in &buf[..n] {
                        if byte_tx.send(byte).is_err() {
                            return;
                        }
                    }
                }
                Err(_) => break,
            }
        }
    });
    let mut utf8_buf: Vec<u8> = Vec::new();
    // True while an escape sequence (arrow keys, mouse reports, function
    // keys) is being consumed; those are ignored so they never pollute the
    // input buffer or move the cursor.
    let mut in_escape = false;
    let mut csi = false;
    loop {
        terminal.draw(|frame| draw(frame, app))?;
        if let Ok(outcome) = app.result_rx.try_recv() {
            if outcome.ok {
                app.messages.push(("assistant".to_owned(), outcome.text));
            } else {
                let notice = if outcome.notice.is_empty() {
                    "请求失败（无详情）".to_owned()
                } else {
                    outcome.notice.clone()
                };
                app.notice = notice.clone();
                app.messages.push(("system".to_owned(), notice));
            }
            app.thinking = false;
        }
        while let Ok(byte) = byte_rx.try_recv() {
            if in_escape {
                if csi {
                    if (0x40..=0x7e).contains(&byte) {
                        in_escape = false;
                        csi = false; // end of CSI sequence
                    }
                } else if byte == 0x5b || byte == 0x4f {
                    csi = true; // ESC [ or ESC O prefix
                } else {
                    in_escape = false; // lone ESC
                }
                continue;
            }
            match byte {
                0x03 => return Ok(()), // Ctrl+C
                0x1b => {
                    in_escape = true;
                    csi = false;
                }
                b'\r' | b'\n' => {
                    flush_utf8(&mut utf8_buf, &mut app.input);
                    let raw = app.input.trim().to_owned();
                    app.input.clear();
                    if raw.starts_with('/') {
                        app.run_command(&raw);
                    } else {
                        app.send_chat(raw);
                    }
                }
                0x7f | 0x08 => {
                    flush_utf8(&mut utf8_buf, &mut app.input);
                    app.input.pop();
                }
                _ => utf8_buf.push(byte),
            }
            if utf8_buf.len() >= 4 {
                flush_utf8(&mut utf8_buf, &mut app.input);
            }
        }
        flush_utf8(&mut utf8_buf, &mut app.input);
        std::thread::sleep(Duration::from_millis(30));
    }
}

/// Decodes complete UTF-8 sequences accumulated from raw stdin bytes into
/// `input`, keeping a trailing incomplete sequence for the next read.
fn flush_utf8(buf: &mut Vec<u8>, input: &mut String) {
    if buf.is_empty() {
        return;
    }
    match std::str::from_utf8(buf) {
        Ok(text) => {
            input.push_str(text);
            buf.clear();
        }
        Err(error) if error.error_len().is_none() => {
            // Incomplete trailing sequence; wait for more bytes.
        }
        Err(_) => {
            // Invalid byte; drop it so the stream stays healthy.
            buf.clear();
        }
    }
}

fn draw(frame: &mut Frame, app: &mut App) {
    let layout = Layout::vertical([
        Constraint::Length(1),
        Constraint::Min(3),
        Constraint::Length(1),
        Constraint::Length(3),
    ])
    .split(frame.area());
    draw_status(frame, layout[0], app);
    draw_chat(frame, layout[1], app);
    draw_hint(frame, layout[2], app);
    draw_input(frame, layout[3], app);
}

fn draw_status(frame: &mut Frame, area: Rect, app: &App) {
    let status = if app.connected {
        Span::styled("● 在线", Style::default().fg(Color::Green))
    } else {
        Span::styled("○ 离线", Style::default().fg(Color::Red))
    };
    let model = format!("模型: {}", if app.model.is_empty() { "未选择" } else { &app.model });
    let nodes = format!("节点: {}", app.nodes.len());
    let line = Line::from(vec![
        Span::styled(" Lociant TUI ", Style::default().fg(Color::Cyan).add_modifier(Modifier::BOLD)),
        status,
        Span::raw("  |  "),
        Span::raw(model),
        Span::raw("  |  "),
        Span::raw(nodes),
        Span::raw("  |  "),
        Span::raw(&app.notice),
    ]);
    frame.render_widget(Paragraph::new(line), area);
}

fn draw_chat(frame: &mut Frame, area: Rect, app: &App) {
    // Render the whole conversation as wrapped text so long replies are not
    // clipped at the terminal width; scroll stays at the latest message.
    let mut lines: Vec<Line> = Vec::new();
    for (role, content) in &app.messages {
        let (prefix, style) = match role.as_str() {
            "user" => ("你: ", Style::default().fg(Color::Cyan)),
            "assistant" => ("AI: ", Style::default().fg(Color::Green)),
            _ => ("系统: ", Style::default().fg(Color::Yellow)),
        };
        for (i, text_line) in content.lines().enumerate() {
            if i == 0 {
                lines.push(Line::from(vec![
                    Span::styled(prefix, style),
                    Span::raw(text_line),
                ]));
            } else {
                lines.push(Line::from(vec![Span::raw(text_line)]));
            }
        }
    }
    if app.thinking {
        lines.push(Line::from(vec![
            Span::styled("…", Style::default().fg(Color::Yellow)),
            Span::styled(" 思考中", Style::default().fg(Color::Yellow)),
        ]));
    }
    let content_area = Block::default()
        .borders(Borders::ALL)
        .title("对话")
        .inner(area);
    let height = content_area.height.saturating_sub(1).max(1) as usize;
    let total = lines.len().saturating_sub(1);
    let scroll = total.saturating_sub(height) as u16;
    let paragraph = Paragraph::new(lines)
        .wrap(Wrap { trim: false })
        .scroll((scroll, 0));
    frame.render_widget(
        paragraph.block(Block::default().borders(Borders::ALL).title("对话")),
        area,
    );
}

fn draw_input(frame: &mut Frame, area: Rect, app: &App) {
    let input = Paragraph::new(Line::from(vec![
        Span::raw("> "),
        Span::raw(&app.input),
    ]))
    .block(Block::default().borders(Borders::ALL).title("输入"))
    .wrap(Wrap { trim: true });
    frame.render_widget(input, area);
    frame.set_cursor_position((
        area.x + 2 + UnicodeWidthStr::width(app.input.as_str()) as u16,
        area.y + 1,
    ));
}

fn draw_hint(frame: &mut Frame, area: Rect, app: &App) {
    let hint = if app.thinking {
        "正在请求模型，请稍候…（Ctrl+C 可随时退出）"
    } else if app.input.starts_with('/') {
        "命令: /help  /models  /model <id>  /nodes  /clear  /quit"
    } else {
        "输入 / 查看命令，Enter 发送，Ctrl+C 退出"
    };
    let line = Line::from(vec![Span::styled(
        hint,
        Style::default().fg(Color::DarkGray),
    )]);
    frame.render_widget(Paragraph::new(line), area);
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn chat_outcome_reaches_channel() {
        // Requires a local node on :12001 (test fixture):
        //   LOCIANT_CONFIG=/tmp/peer-local/config.json ... lociant-server
        let mut app = App::new("http://127.0.0.1:12001".to_owned(), "tok-local".to_owned());
        app.model = "peer:lociant-node:qwen3.5-0.8b-w4a16-g128-opt0".to_owned();
        app.send_chat("你好，请一句话回复".to_owned());
        let outcome = app
            .result_rx
            .recv_timeout(Duration::from_secs(40))
            .expect("chat outcome should arrive within 40s");
        assert!(outcome.ok, "chat failed: {}", outcome.notice);
        assert!(!outcome.text.is_empty(), "empty reply");
    }
}
