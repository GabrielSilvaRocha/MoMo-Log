import React from 'react'
import ReactDOM from 'react-dom/client'

import App from './App'
import './styles.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)


const isAndroidAssetHost = window.location.hostname === 'appassets.androidplatform.net'

if ('serviceWorker' in navigator && isAndroidAssetHost) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.getRegistrations()
      .then((registrations) => registrations.forEach((registration) => registration.unregister()))
      .catch(() => undefined)

    if ('caches' in window) {
      caches.keys()
        .then((keys) => keys.forEach((key) => caches.delete(key)))
        .catch(() => undefined)
    }
  })
}

if ('serviceWorker' in navigator && !isAndroidAssetHost) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {
      // Offline support is best-effort when the app is not served from HTTPS or localhost.
    })
  })
}
