/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        mo: {
          background: '#050807',
          surface: '#0b1110',
          surfaceElevated: '#111a18',
          border: '#1c2a27',
          primary: '#77ff6b',
          primarySoft: '#d8ffd4',
          muted: '#8fa19c',
        },
      },
      boxShadow: {
        glow: '0 0 30px rgba(119, 255, 107, 0.18)',
      },
    },
  },
  plugins: [],
}
