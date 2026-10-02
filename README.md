# Pusher Alert Android App

This Android app listens to:

Channel:
chat-channel

Event:
new-text

Expected payload:

{
  "text1": "ALL",
  "text2": "NQHIGH"
}

It then displays an Android notification.

## GitHub Actions

The workflow is:

.github/workflows/build-apk.yml

It builds:

app/build/outputs/apk/debug/app-debug.apk

No Codemagic is required.

## Important

Only the Pusher public app key belongs in the Android app.

Never put the Pusher secret in the Android app.

The Pusher secret should remain only in your Python/server-side code.
