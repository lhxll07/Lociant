//! Terminal control console for headless Lociant edge nodes.
//!
//! The TUI is intentionally a control-plane client: it shows runtime health,
//! models, nodes and executable tools without becoming another chat client.
//! Usage: `lociant-tui --connect http://HOST:11434 --token TOKEN`.

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
use ratatui::widgets::{Block, Borders, List, ListItem, Paragraph, Wrap};
use ratatui::{Frame, Terminal};
use serde_json::Value;
use unicode_width::UnicodeWidthStr;

struct App {
    base_url: String,
    token: String,
    client: reqwest::blocking::Client,
    runtime: Value,
    models: Vec<Value>,
    nodes: Vec<Value>,
    tools: Vec<Value>,
    input: String,
    connected: bool,
    notice: String,
}

impl App {
    fn new(base_url: String, token: String) -> Self {
        App {
            client: reqwest::blocking::Client::builder()
                .timeout(Duration::from_secs(10))
                .build()
                .unwrap_or_default(),
            base_url,
            token,
            runtime: Value::Object(Default::default()),
            models: Vec::new(),
            nodes: Vec::new(),
            tools: Vec::new(),
            input: String::new(),
            connected: false,
            notice: String::new(),
        }
    }

    fn api(&self, path: &str) -> String {
        format!("{}{path}", self.base_url.trim_end_matches('/'))
    }

    fn headers(&self) -> reqwest::header::HeaderMap {
        let mut headers = reqwest::header::HeaderMap::new();
        if !self.token.is_empty() {
            if let Ok(value) = format!("Bearer {}", self.token).parse() {
                headers.insert(reqwest::header::AUTHORIZATION, value);
            }
        }
        headers
    }

    fn get_json(&self, path: &str) -> Option<Value> {
        self.client
            .get(self.api(path))
            .headers(self.headers())
            .send()
            .ok()
            .filter(|response| response.status().is_success())
            .and_then(|response| response.json().ok())
    }

    fn refresh(&mut self) {
        self.connected = self
            .client
            .get(self.api("/health"))
            .send()
            .map(|response| response.status().is_success())
            .unwrap_or(false);
        if !self.connected {
            self.notice = "无法连接到节点".to_owned();
            return;
        }

        self.runtime = self
            .get_json("/api/v1/runtime")
            .unwrap_or_else(|| Value::Object(Default::default()));
        self.models = self
            .get_json("/api/v1/models")
            .and_then(|body| body.get("models").cloned())
            .and_then(|models| models.as_array().cloned())
            .unwrap_or_default();
        self.nodes = self
            .get_json("/api/v1/nodes")
            .and_then(|body| body.get("nodes").cloned())
            .and_then(|nodes| nodes.as_array().cloned())
            .unwrap_or_default();
        self.tools = self
            .get_json("/api/v1/tools")
            .and_then(|body| body.get("data").cloned())
            .and_then(|tools| tools.as_array().cloned())
            .unwrap_or_default();
        self.notice = format!(
            "已刷新 · {} 个模型 · {} 个节点 · {} 个工具",
            self.models.len(),
            self.nodes.len(),
            self.tools.len()
        );
    }

    fn run_command(&mut self, raw: &str) -> bool {
        let command = raw.trim();
        match command {
            "" => false,
            "/help" => {
                self.notice = "/refresh 重新读取  /models  /nodes  /tools  /quit".to_owned();
                false
            }
            "/refresh" | "r" => {
                self.refresh();
                false
            }
            "/models" | "m" => {
                self.notice = format!("模型列表已显示，共 {} 个", self.models.len());
                false
            }
            "/nodes" | "n" => {
                self.notice = format!("节点列表已显示，共 {} 个", self.nodes.len());
                false
            }
            "/tools" | "t" => {
                self.notice = format!("工具列表已显示，共 {} 个", self.tools.len());
                false
            }
            "/quit" | "q" => true,
            _ => {
                self.notice = format!("未知命令: {command}（输入 /help 查看）");
                false
            }
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
                println!("lociant-tui — Lociant 边缘节点控制台");
                println!("用法: lociant-tui [--connect http://host:11434] [--token TOKEN]");
                return Ok(());
            }
            _ => {
                eprintln!("未知参数: {arg}（--help 查看）");
                std::process::exit(2);
            }
        }
    }

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
    let (byte_tx, byte_rx) = std::sync::mpsc::channel::<u8>();
    std::thread::spawn(move || {
        let mut stdin = io::stdin();
        let mut buffer = [0u8; 256];
        loop {
            match stdin.read(&mut buffer) {
                Ok(0) => break,
                Ok(size) => {
                    for &byte in &buffer[..size] {
                        if byte_tx.send(byte).is_err() {
                            return;
                        }
                    }
                }
                Err(_) => break,
            }
        }
    });

    let mut utf8_buffer = Vec::new();
    let mut in_escape = false;
    let mut csi = false;
    loop {
        terminal.draw(|frame| draw(frame, app))?;
        while let Ok(byte) = byte_rx.try_recv() {
            if in_escape {
                if csi {
                    if (0x40..=0x7e).contains(&byte) {
                        in_escape = false;
                        csi = false;
                    }
                } else if byte == b'[' || byte == b'O' {
                    csi = true;
                } else {
                    in_escape = false;
                }
                continue;
            }
            match byte {
                0x03 => return Ok(()),
                0x1b => {
                    in_escape = true;
                    csi = false;
                }
                b'\r' | b'\n' => {
                    flush_utf8(&mut utf8_buffer, &mut app.input);
                    let command = std::mem::take(&mut app.input);
                    if app.run_command(&command) {
                        return Ok(());
                    }
                }
                0x7f | 0x08 => {
                    flush_utf8(&mut utf8_buffer, &mut app.input);
                    app.input.pop();
                }
                _ => utf8_buffer.push(byte),
            }
            if utf8_buffer.len() >= 4 {
                flush_utf8(&mut utf8_buffer, &mut app.input);
            }
        }
        flush_utf8(&mut utf8_buffer, &mut app.input);
        std::thread::sleep(Duration::from_millis(30));
    }
}

fn flush_utf8(buffer: &mut Vec<u8>, input: &mut String) {
    if buffer.is_empty() {
        return;
    }
    match std::str::from_utf8(buffer) {
        Ok(text) => {
            input.push_str(text);
            buffer.clear();
        }
        Err(error) if error.error_len().is_none() => {}
        Err(_) => buffer.clear(),
    }
}

fn draw(frame: &mut Frame, app: &App) {
    let outer = Layout::vertical([
        Constraint::Length(1),
        Constraint::Min(8),
        Constraint::Length(1),
        Constraint::Length(3),
    ])
    .split(frame.area());
    draw_status(frame, outer[0], app);

    let columns = Layout::horizontal([
        Constraint::Percentage(34),
        Constraint::Percentage(33),
        Constraint::Percentage(33),
    ])
    .split(outer[1]);
    draw_runtime(frame, columns[0], app);
    draw_models(frame, columns[1], app);
    draw_nodes(frame, columns[2], app);
    draw_hint(frame, outer[2], app);
    draw_input(frame, outer[3], app);
}

fn draw_status(frame: &mut Frame, area: Rect, app: &App) {
    let status = if app.connected {
        Span::styled("● 在线", Style::default().fg(Color::Green))
    } else {
        Span::styled("○ 离线", Style::default().fg(Color::Red))
    };
    let line = Line::from(vec![
        Span::styled(
            " Lociant Edge ",
            Style::default()
                .fg(Color::Cyan)
                .add_modifier(Modifier::BOLD),
        ),
        status,
        Span::raw("  "),
        Span::raw(&app.base_url),
        Span::raw("  "),
        Span::styled(&app.notice, Style::default().fg(Color::DarkGray)),
    ]);
    frame.render_widget(Paragraph::new(line), area);
}

fn draw_runtime(frame: &mut Frame, area: Rect, app: &App) {
    let running = app
        .runtime
        .get("running")
        .and_then(Value::as_bool)
        .unwrap_or(false);
    let model = field(&app.runtime, "modelId", "未选择");
    let exposure = field(&app.runtime, "toolExposure", "action");
    let lines = vec![
        Line::from(vec![
            Span::raw("状态  "),
            Span::styled(
                if running { "运行中" } else { "已停止" },
                Style::default().fg(if running { Color::Green } else { Color::Red }),
            ),
        ]),
        Line::from(format!("模型  {model}")),
        Line::from(format!("工具  {exposure}")),
        Line::from(format!("端口  {}", field(&app.runtime, "port", "11434"))),
    ];
    frame.render_widget(
        Paragraph::new(lines)
            .block(Block::default().borders(Borders::ALL).title("运行时"))
            .wrap(Wrap { trim: false }),
        area,
    );
}

fn draw_models(frame: &mut Frame, area: Rect, app: &App) {
    let items = app.models.iter().map(|model| {
        let id = field(model, "id", "?");
        let runtime = field(model, "runtime", "unknown");
        ListItem::new(Line::from(vec![
            Span::styled(id, Style::default().fg(Color::Cyan)),
            Span::raw(format!("  {runtime}")),
        ]))
    });
    frame.render_widget(
        List::new(items).block(Block::default().borders(Borders::ALL).title("模型")),
        area,
    );
}

fn draw_nodes(frame: &mut Frame, area: Rect, app: &App) {
    let items = app.nodes.iter().map(|node| {
        let name = field(node, "name", "?");
        let platform = field(node, "platform", "unknown");
        let online = node.get("online").and_then(Value::as_bool).unwrap_or(false);
        ListItem::new(Line::from(vec![
            Span::styled(
                name,
                Style::default().fg(if online { Color::Green } else { Color::Gray }),
            ),
            Span::raw(format!("  {platform}")),
        ]))
    });
    frame.render_widget(
        List::new(items).block(Block::default().borders(Borders::ALL).title("节点")),
        area,
    );
}

fn draw_hint(frame: &mut Frame, area: Rect, app: &App) {
    let text = format!(
        "工具 {} 个 · 输入 /help 查看命令 · Enter 执行 · Ctrl+C 退出",
        app.tools.len()
    );
    frame.render_widget(
        Paragraph::new(Span::styled(text, Style::default().fg(Color::DarkGray))),
        area,
    );
}

fn draw_input(frame: &mut Frame, area: Rect, app: &App) {
    let input = Paragraph::new(Line::from(vec![Span::raw("> "), Span::raw(&app.input)]))
        .block(Block::default().borders(Borders::ALL).title("控制命令"))
        .wrap(Wrap { trim: true });
    frame.render_widget(input, area);
    frame.set_cursor_position((
        area.x + 2 + UnicodeWidthStr::width(app.input.as_str()) as u16,
        area.y + 1,
    ));
}

fn field(value: &Value, key: &str, fallback: &str) -> String {
    value
        .get(key)
        .and_then(Value::as_str)
        .map(str::to_owned)
        .or_else(|| value.get(key).map(ToString::to_string))
        .filter(|value| !value.is_empty())
        .unwrap_or_else(|| fallback.to_owned())
}
