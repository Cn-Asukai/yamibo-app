# Baidu NOX WAF Recovery

## What the WAF Does

Some connections to `bbs.yamibo.com` are routed through Baidu WAF. When a request does not contain a valid `nox_jst_v1` cookie, the WAF returns HTTP 405 with a NOX JavaScript challenge instead of the requested forum page.

A regular HTTP client cannot execute this JavaScript, so repeating the same request still returns 405. A browser can execute the challenge, obtain `nox_jst_v1`, and use that cookie to access the forum normally.

## How the App Recovers

1. `yamibo-api` identifies Baidu WAF only when an HTTP 405 response body contains a known NOX marker. Ordinary HTTP 405 responses do not start recovery.
2. While a foreground UI host is available, the API creates a system-native WebView behind the existing app content and loads the same Yamibo URL. The WebView is never shown to the user.
3. The API polls the WebView cookie store without waiting for the forum page to finish loading. It stops the WebView as soon as `nox_jst_v1` is available.
4. The new NOX cookie replaces only the entry with the same name in the client's composed Cookie header. Login cookies remain unchanged.
5. The API validates the cookie with a safe same-origin GET. After successful validation, it replays the original request at most once.

Concurrent requests share a single WebView challenge instead of creating multiple WebViews.

## Recovery Flight and Host Lifecycle

The recovery flight belongs to the process-scoped `YamiboClient`, not to the Activity, view controller, or WebView that executes one browser attempt:

- A detected challenge immediately creates one shared flight. If no foreground host exists, it waits up to `hostWaitTimeoutMillis` (30 seconds by default) instead of failing immediately.
- A permission dialog, rotation, Activity/view-controller recreation, or background transition destroys the current WebView attempt but does not end the flight. The challenge budget pauses while no host is usable.
- When a host returns, the API starts a fresh browser attempt with the flight's remaining budget. Each continuous host outage has a new bounded host-wait window.
- Host registrations and browser-attempt callbacks are identity-scoped, so a stale detach, cookie, or WebView error cannot affect the replacement host or attempt.
- Host wait timeout returns `FOREGROUND_REQUIRED`; challenge budget exhaustion returns `TIMED_OUT`; client close returns `CANCELLED`.

Android and iOS must keep using their process-level `AndroidYamiboClientProvider` and `IOSYamiboClientProvider`. Those providers also hydrate cookies and share the same WAF-enabled client with background app services; UI-local client holders must not be introduced.

## User Experience

Recovery is silent. The current screen remains visible and may only appear to load longer than usual. No browser, WAF prompt, or additional control is displayed.

If the WebView, cookie validation, or replay fails, the API returns `YamiboResult.WafChallenge`. The app displays it as a normal loading failure, and the user can use the existing refresh action to fetch again.

Background work never creates a WebView when no foreground host is available. A flight may wait for the UI to return within its bounded host-wait window, and a WAF challenge is never treated as logout.

## Security Limits

- The WebView permits only same-origin HTTPS Yamibo navigation and exposes no native JavaScript bridge.
- Login cookies and `nox_jst_v1` are never written to logs, error reports, or test data.
- Each request allows one shared recovery flight and at most one replay. Only one browser attempt is active at a time, preventing loops and duplicate writes while still allowing host recreation.
- Recovery supports only the observed non-interactive NOX challenge. It does not automate CAPTCHA.
