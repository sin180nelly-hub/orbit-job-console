/**
 * Navigator Nest — 會動的截止倒數
 *
 * 規則：
 * - 有 dueDate + dueTime → 截止點 = 該日 dueTime
 * - 僅有 dueDate → 截止點 = 該日 23:59:59（當日結束）
 * - 距離 < 24h（含已逾期 < 24h）→ 每秒跳動的 HH:MM:SS（live: true）
 * - 距離 ≥ 1 天 → 靜態「還有 X 天 Y 小時」
 */
(function (global) {
  'use strict';

  function pad2(n) {
    n = Math.floor(Math.abs(n));
    return n < 10 ? '0' + n : String(n);
  }

  function parseDueTime(dueTime) {
    if (dueTime == null || dueTime === '') return null;
    if (typeof dueTime === 'object' && dueTime.hour != null) {
      return { h: Number(dueTime.hour), m: Number(dueTime.minute || 0), s: Number(dueTime.second || 0) };
    }
    var s = String(dueTime);
    var parts = s.split(':');
    if (parts.length < 2) return null;
    return {
      h: parseInt(parts[0], 10) || 0,
      m: parseInt(parts[1], 10) || 0,
      s: parseInt(parts[2], 10) || 0
    };
  }

  function deadlineOf(task) {
    if (!task || !task.dueDate) return null;
    var p = String(task.dueDate).split('-').map(Number);
    if (p.length < 3 || !p[0]) return null;
    var y = p[0], mo = p[1] - 1, d = p[2];
    var t = parseDueTime(task.dueTime);
    if (t) {
      return new Date(y, mo, d, t.h, t.m, t.s || 0, 0);
    }
    return new Date(y, mo, d, 23, 59, 59, 999);
  }

  /** 將 ms 轉成 HH:MM:SS（可超過 24h 的小時數仍用 floor hour） */
  function formatHms(ms) {
    var abs = Math.max(0, Math.floor(Math.abs(ms) / 1000));
    var h = Math.floor(abs / 3600);
    var m = Math.floor((abs % 3600) / 60);
    var s = abs % 60;
    return pad2(h) + ':' + pad2(m) + ':' + pad2(s);
  }

  /**
   * @returns {{ text: string, cls: string, ms: number|null, live: boolean, prefix: string, clock: string }}
   */
  function countdownInfo(task, now) {
    now = now || new Date();
    if (task && task.status === 'DONE') {
      return { text: '已完成', cls: 'is-done', ms: null, live: false, prefix: '', clock: '' };
    }
    var dead = deadlineOf(task);
    if (!dead) {
      return { text: '無截止日', cls: 'is-ok', ms: null, live: false, prefix: '', clock: '' };
    }

    var ms = dead.getTime() - now.getTime();
    var abs = Math.abs(ms);
    var hourMs = 60 * 60 * 1000;
    var dayMs = 24 * hourMs;

    // —— 24 小時內：跳動時:分:秒 ——
    if (abs < dayMs) {
      var clock = formatHms(abs);
      if (ms < 0) {
        return {
          text: '已逾時 ' + clock,
          cls: 'is-overdue is-live',
          ms: ms,
          live: true,
          prefix: '已逾時',
          clock: clock
        };
      }
      // 剩餘很少時用更緊的樣式
      var urgent = abs < 6 * hourMs;
      return {
        text: '還有 ' + clock,
        cls: (urgent ? 'is-today' : 'is-soon') + ' is-live',
        ms: ms,
        live: true,
        prefix: '還有',
        clock: clock
      };
    }

    // —— ≥ 1 天：靜態天+小時 ——
    if (ms < 0) {
      var od = Math.floor(abs / dayMs);
      var remH = Math.floor((abs % dayMs) / hourMs);
      var ot = remH > 0 ? ('已逾期 ' + od + ' 天 ' + remH + ' 小時') : ('已逾期 ' + od + ' 天');
      return { text: ot, cls: 'is-overdue', ms: ms, live: false, prefix: '', clock: '' };
    }

    var days = Math.floor(ms / dayMs);
    var hoursLeft = Math.floor((ms % dayMs) / hourMs);
    var text;
    if (days === 1) {
      text = hoursLeft > 0 ? '還有 1 天 ' + hoursLeft + ' 小時' : '還有 1 天';
    } else if (hoursLeft > 0 && days <= 7) {
      text = '還有 ' + days + ' 天 ' + hoursLeft + ' 小時';
    } else {
      text = '還有 ' + days + ' 天';
    }
    return {
      text: text,
      cls: days <= 3 ? 'is-soon' : 'is-ok',
      ms: ms,
      live: false,
      prefix: '',
      clock: ''
    };
  }

  /** 產生醒目 HTML（live 時含 data 與 clock span） */
  function renderHtml(task, escapeHtml) {
    escapeHtml = escapeHtml || function (s) {
      return String(s == null ? '' : s)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
    };
    var c = countdownInfo(task);
    if (c.live) {
      return '<span class="countdown ' + c.cls + '" data-live="1" data-task-id="' +
        escapeHtml(task && task.id != null ? task.id : '') + '">' +
        '<span class="cd-prefix">' + escapeHtml(c.prefix) + '</span> ' +
        '<span class="cd-clock">' + escapeHtml(c.clock) + '</span></span>';
    }
    return '<span class="countdown ' + c.cls + '">' + escapeHtml(c.text) + '</span>';
  }

  /**
   * 更新 DOM 內所有 [data-live="1"] 倒數；tasksById 為 Map 或 id→task 物件
   */
  function tickLiveElements(root, tasksById) {
    root = root || document;
    var nodes = root.querySelectorAll('.countdown[data-live="1"]');
    if (!nodes.length) return;
    var now = new Date();
    for (var i = 0; i < nodes.length; i++) {
      var el = nodes[i];
      var id = el.getAttribute('data-task-id');
      var task = null;
      if (tasksById) {
        if (typeof tasksById.get === 'function') task = tasksById.get(Number(id));
        else task = tasksById[id] || tasksById[Number(id)];
      }
      if (!task) continue;
      var c = countdownInfo(task, now);
      el.className = 'countdown ' + c.cls;
      if (!c.live) {
        el.removeAttribute('data-live');
        el.textContent = c.text;
        continue;
      }
      el.setAttribute('data-live', '1');
      var clockEl = el.querySelector('.cd-clock');
      var prefixEl = el.querySelector('.cd-prefix');
      if (clockEl && prefixEl) {
        prefixEl.textContent = c.prefix;
        clockEl.textContent = c.clock;
      } else {
        el.innerHTML = '<span class="cd-prefix"></span> <span class="cd-clock"></span>';
        el.querySelector('.cd-prefix').textContent = c.prefix;
        el.querySelector('.cd-clock').textContent = c.clock;
      }
    }
  }

  function formatDueDisplay(task) {
    if (!task || !task.dueDate) return '無';
    var t = parseDueTime(task.dueTime);
    if (!t) return task.dueDate;
    return task.dueDate + ' ' + pad2(t.h) + ':' + pad2(t.m);
  }

  function dueTimeInputValue(dueTime) {
    var t = parseDueTime(dueTime);
    if (!t) return '';
    return pad2(t.h) + ':' + pad2(t.m);
  }

  global.NNCountdown = {
    deadlineOf: deadlineOf,
    countdownInfo: countdownInfo,
    formatDueDisplay: formatDueDisplay,
    dueTimeInputValue: dueTimeInputValue,
    renderHtml: renderHtml,
    tickLiveElements: tickLiveElements,
    formatHms: formatHms
  };
})(typeof window !== 'undefined' ? window : this);
