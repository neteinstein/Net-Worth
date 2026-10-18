# Google Cloud setup (sign-in + Drive)

Net Worth keeps the family database in **one JSON file in the user's Google Drive** (see
[database.md](database.md)). Every platform signs in with Google and asks for a single scope,
`https://www.googleapis.com/auth/drive.file`: the app can only see files it created or that the user picked
in the Google Picker. It is a *non-sensitive* scope, so Google's security assessment is not required.

Without this configuration the app builds and runs, but "Sign in with Google" shows
"Google sign-in isn't set up in this build yet".

## 1. Cloud project

1. Create (or pick) a project in the [Google Cloud console](https://console.cloud.google.com/).
2. **APIs & Services → Library**: enable **Google Drive API** and **Google Picker API**.
3. **Google Auth Platform → Branding / Audience**: create the consent screen (External). While in *Testing*,
   add each family member's Google account as a test user. Add the scopes `openid`, `email`, `profile`
   and `…/auth/drive.file` under **Data access**.

## 2. OAuth clients and key

| What | Where | Used by |
| --- | --- | --- |
| **Web application** client | Authorised JavaScript origins: `http://localhost:8080` (dev server) and every hosting origin (GitHub Pages and Cloudflare Pages while both are live) | Web sign-in, and as the *server client id* for Android's Credential Manager |
| **Android** client | Package `com.neteinstein.networth` (and `com.neteinstein.networth.debug`) + the SHA-1 of each signing key (`./gradlew :androidApp:signingReport`) | Android authorisation (no value goes in the app; Google matches package + SHA-1) |
| **iOS** client | Bundle id of `iosApp` | iOS sign-in and Picker (redirects to the reversed client id) |
| **API key** | Restrict to the Google Picker API and your web origin(s) | Web Picker |
| **Project number** | Project settings | Web Picker `appId` |

## 3. Give the values to the build

`:core:google:generateGoogleConfig` compiles them into `GoogleConfig`. Each value is read from, in order,
a Gradle property, the git-ignored `local.properties`, or an environment variable:

```properties
# local.properties (never committed)
networth.google.webClientId=1234-abc.apps.googleusercontent.com
networth.google.iosClientId=1234-def.apps.googleusercontent.com
networth.google.apiKey=AIza…
networth.google.projectNumber=1234
```

Environment variables: `NETWORTH_GOOGLE_WEB_CLIENT_ID`, `NETWORTH_GOOGLE_IOS_CLIENT_ID`,
`NETWORTH_GOOGLE_API_KEY`, `NETWORTH_GOOGLE_PROJECT_NUMBER` (CI secrets of the same names).

Client ids and the Picker key are not secrets (they ship inside every app), but they are per project, so
they stay out of the repository.

## How each platform signs in

- **Android** — Credential Manager "Sign in with Google" (identity), then
  `Identity.getAuthorizationClient().authorize()` for the `drive.file` token. "Open a Google Drive file"
  sends the same request with the `PICKER_OAUTH_TRIGGER` resource parameter; the picked ids come back in
  `picked_file_ids`.
- **iOS** — OAuth 2.0 authorisation code + PKCE in an `ASWebAuthenticationSession`, redirecting to
  `com.googleusercontent.apps.<id>:/oauth2redirect`. The refresh token is stored in the Keychain. The Picker
  is the same browser flow with `trigger_onepick=true`.
- **Web** — Google Identity Services token client and the Google Picker (`index.html` loads both scripts).

## Sharing with the other parent

Settings → *Share with your partner* gives their Google account edit access to the file. They sign in on
their device and choose **Open a Google Drive file**, then pick it in the Picker. Picking is what grants the
app access to that file under `drive.file`.
